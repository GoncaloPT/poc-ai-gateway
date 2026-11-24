package pt.goncalo.aigateway.registry;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

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

	/** MCP tool name rules (spec 2025-11-25). */
	private static final Pattern PUBLIC_NAME = Pattern.compile("[A-Za-z0-9_.-]{1,128}");

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
	 * seeded; changed definitions are taken over silently, except that an enabled tool which becomes a write tool
	 * while open to every client is disabled; tools no longer offered are removed.
	 *
	 * @param upstreamId upstream id
	 * @param upstreamTools tools returned by the upstream {@code tools/list}
	 */
	public synchronized void syncUpstream(String upstreamId, List<McpSchema.Tool> upstreamTools) {
		var seen = new HashSet<String>();
		for (var definition : upstreamTools) {
			var id = RegisteredTool.key(upstreamId, definition.name());
			seen.add(id);
			tools.compute(id, (key, existing) -> closeOpenWriteTool(
					existing != null ? existing.withDefinition(definition) : newTool(upstreamId, definition)));
		}
		tools.values()
			.removeIf(tool -> tool.upstreamId().equals(upstreamId) && !seen.contains(tool.id()));
		eventPublisher.publishEvent(new RegistryChangedEvent());
	}

	/**
	 * Changes the exposure rules and public name of a tool.
	 *
	 * @param toolId registry id ({@code <upstream>/<internalName>})
	 * @param publicName new public name, {@code null} = keep the current one
	 * @param status new status
	 * @param clients granted clients, empty = every enabled client (read-only tools only)
	 * @param description description override, {@code null} = upstream description
	 * @return updated tool, empty if unknown
	 * @throws InvalidToolRulesException if the public name is invalid or an enabled write tool has no grant
	 * @throws PublicNameTakenException if another tool already uses the public name
	 */
	public synchronized Optional<RegisteredTool> updateTool(String toolId, String publicName, ToolStatus status,
			List<String> clients, String description) {
		var current = tools.get(toolId);
		if (current == null) {
			return Optional.empty();
		}
		var name = publicName != null ? publicName : current.publicName();
		if (!PUBLIC_NAME.matcher(name).matches()) {
			throw new InvalidToolRulesException(
					"Public name must be 1-128 characters of A-Z, a-z, 0-9, '_', '-' or '.'");
		}
		findByPublicName(name).filter(owner -> !owner.id().equals(toolId)).ifPresent(owner -> {
			throw new PublicNameTakenException(name, owner.id());
		});
		var updated = current.withRules(name, status, clients, description);
		if (updated.isOpenWriteTool()) {
			throw new InvalidToolRulesException("Write tools need at least one explicitly granted client");
		}
		tools.put(toolId, updated);
		log.info("Tool {} updated: publicName={} status={} clients={}", toolId, name, status, clients);
		eventPublisher.publishEvent(new RegistryChangedEvent());
		return Optional.of(updated);
	}

	private RegisteredTool closeOpenWriteTool(RegisteredTool tool) {
		if (!tool.isOpenWriteTool()) {
			return tool;
		}
		log.warn("Tool {} is a write tool without an explicit grant, disabled until granted", tool.id());
		return tool.withStatus(ToolStatus.DISABLED);
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
