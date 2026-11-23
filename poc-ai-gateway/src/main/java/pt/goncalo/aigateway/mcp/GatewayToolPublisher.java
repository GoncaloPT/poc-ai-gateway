package pt.goncalo.aigateway.mcp;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.server.McpStatelessSyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pt.goncalo.aigateway.registry.RegisteredTool;
import pt.goncalo.aigateway.registry.RegistryChangedEvent;
import pt.goncalo.aigateway.registry.ToolRegistry;

/**
 * Keeps the tools of the outward MCP server in sync with the registry: every enabled tool is registered under its
 * public name with {@link ToolCallForwarder} as handler; disabled, removed or changed tools are (re)registered.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class GatewayToolPublisher {

	private final McpStatelessSyncServer server;

	private final ToolRegistry registry;

	private final ToolCallForwarder forwarder;

	// Public name -> definition currently registered on the server.
	private final Map<String, McpSchema.Tool> published = new HashMap<>();

	@EventListener(RegistryChangedEvent.class)
	synchronized void sync() {
		var desired = registry.tools()
			.stream()
			.filter(RegisteredTool::isEnabled)
			.map(RegisteredTool::publicDefinition)
			.collect(Collectors.toMap(McpSchema.Tool::name, tool -> tool));

		published.entrySet().removeIf(entry -> {
			var stale = !entry.getValue().equals(desired.get(entry.getKey()));
			if (stale) {
				server.removeTool(entry.getKey());
				log.info("Tool unpublished: {}", entry.getKey());
			}
			return stale;
		});

		desired.forEach((name, tool) -> {
			if (!published.containsKey(name)) {
				server.addTool(McpStatelessServerFeatures.SyncToolSpecification.builder()
					.tool(tool)
					.callHandler(forwarder::call)
					.build());
				published.put(name, tool);
				log.info("Tool published: {}", name);
			}
		});
	}

}
