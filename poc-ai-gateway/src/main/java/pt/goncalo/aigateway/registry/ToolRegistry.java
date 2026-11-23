package pt.goncalo.aigateway.registry;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Gateway registry: which AI clients may connect and which upstream tools are exposed to whom.
 * <p>
 * POC: in memory, seeded from {@link AiGatewayProperties}. Target: DB-backed with an audited admin API.
 */
@Slf4j
@Component
public class ToolRegistry {

	private final AiGatewayProperties properties;

	private final ApplicationEventPublisher eventPublisher;

	private final Map<String, RegisteredTool> tools = new ConcurrentHashMap<>();

	private final Map<String, Boolean> clients;

	/**
	 * @param properties gateway configuration and seed
	 * @param eventPublisher publishes {@link RegistryChangedEvent}
	 */
	public ToolRegistry(AiGatewayProperties properties, ApplicationEventPublisher eventPublisher) {
		this.properties = properties;
		this.eventPublisher = eventPublisher;
		this.clients = new ConcurrentHashMap<>(properties.clients());
	}

	/**
	 * @return {@code false} when the global kill switch is off
	 */
	public boolean isGatewayEnabled() {
		return properties.enabled();
	}

	/**
	 * @param clientId AI client id, may be {@code null}
	 * @return {@code true} if the client is registered and enabled
	 */
	public boolean isClientEnabled(String clientId) {
		return clientId != null && clients.getOrDefault(clientId, false);
	}

	/**
	 * @return all known clients and whether they are enabled, sorted by id
	 */
	public Map<String, Boolean> clients() {
		return new TreeMap<>(clients);
	}

	/**
	 * Registers a client or changes its enabled flag. Takes effect on the next request.
	 *
	 * @param clientId AI client id
	 * @param enabled new state
	 */
	public void setClientEnabled(String clientId, boolean enabled) {
		clients.put(clientId, enabled);
		log.info("Client {} {}", clientId, enabled ? "enabled" : "disabled");
	}

	/**
	 * @return all known tools, sorted by id
	 */
	public List<RegisteredTool> tools() {
		return tools.values().stream().sorted(Comparator.comparing(RegisteredTool::id)).toList();
	}

	/**
	 * @param publicName public tool name
	 * @return the tool with that public name
	 */
	public Optional<RegisteredTool> findByPublicName(String publicName) {
		return tools.values().stream().filter(tool -> tool.publicName().equals(publicName)).findFirst();
	}

	/**
	 * Applies the current tool list of an upstream. New tools are added as {@link ToolStatus#DISCOVERED} unless
	 * seeded; changed definitions are taken over silently; tools no longer offered are removed.
	 *
	 * @param upstreamId upstream id
	 * @param upstreamTools tools returned by the upstream {@code tools/list}
	 */
	public void syncUpstream(String upstreamId, List<McpSchema.Tool> upstreamTools) {
		var seen = new HashSet<String>();
		for (var definition : upstreamTools) {
			var id = RegisteredTool.key(upstreamId, definition.name());
			seen.add(id);
			tools.compute(id, (key, existing) -> existing != null ? existing.withDefinition(definition)
					: newTool(upstreamId, definition));
		}
		tools.values()
			.removeIf(tool -> tool.upstreamId().equals(upstreamId) && !seen.contains(tool.id()));
		eventPublisher.publishEvent(new RegistryChangedEvent());
	}

	/**
	 * Changes the exposure rules of a tool.
	 *
	 * @param toolId registry id ({@code <upstream>/<internalName>})
	 * @param status new status
	 * @param clients granted clients, empty = every enabled client
	 * @param description description override, {@code null} = upstream description
	 * @return updated tool, empty if unknown
	 */
	public Optional<RegisteredTool> updateTool(String toolId, ToolStatus status, List<String> clients,
			String description) {
		var updated = tools.computeIfPresent(toolId, (key, tool) -> tool.withRules(status, clients, description));
		if (updated != null) {
			log.info("Tool {} updated: status={} clients={}", toolId, status, clients);
			eventPublisher.publishEvent(new RegistryChangedEvent());
		}
		return Optional.ofNullable(updated);
	}

	private RegisteredTool newTool(String upstreamId, McpSchema.Tool definition) {
		var upstream = properties.upstreams().get(upstreamId);
		var seed = properties.tools()
			.stream()
			.filter(candidate -> candidate.upstream().equals(upstreamId)
					&& candidate.internalName().equals(definition.name()))
			.findFirst();
		var publicName = seed.map(AiGatewayProperties.ToolSeed::publicName)
			.orElse("%s_%s".formatted(upstream.domain(), definition.name()));
		if (seed.isEmpty()) {
			log.info("New tool discovered: {}/{} (disabled until approved)", upstreamId, definition.name());
			return new RegisteredTool(upstreamId, definition.name(), publicName, ToolStatus.DISCOVERED, List.of(),
					null, definition);
		}
		var status = seed.get().enabled() ? ToolStatus.ENABLED : ToolStatus.DISABLED;
		return new RegisteredTool(upstreamId, definition.name(), publicName, status, List.copyOf(seed.get().clients()),
				seed.get().description(), definition);
	}

}
