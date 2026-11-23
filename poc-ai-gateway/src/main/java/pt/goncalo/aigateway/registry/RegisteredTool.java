package pt.goncalo.aigateway.registry;

import java.util.List;

import io.modelcontextprotocol.spec.McpSchema;

/**
 * A tool known to the gateway: its upstream definition plus the gateway's exposure rules. Immutable, the registry
 * replaces instances on change.
 *
 * @param upstreamId upstream that owns the tool
 * @param internalName tool name on the upstream
 * @param publicName tool name shown to AI clients
 * @param status exposure status
 * @param clients granted clients, empty = every enabled client
 * @param descriptionOverride description shown to AI clients, {@code null} = upstream description
 * @param definition last tool definition read from the upstream
 */
public record RegisteredTool(String upstreamId, String internalName, String publicName, ToolStatus status,
		List<String> clients, String descriptionOverride, McpSchema.Tool definition) {

	/**
	 * @return registry key, unique across upstreams
	 */
	public String id() {
		return key(upstreamId, internalName);
	}

	/**
	 * @return {@code true} if the tool is exposed at all
	 */
	public boolean isEnabled() {
		return status == ToolStatus.ENABLED;
	}

	/**
	 * @param clientId AI client id
	 * @return {@code true} if the tool is enabled and granted to the client
	 */
	public boolean isCallableBy(String clientId) {
		return isEnabled() && (clients.isEmpty() || clients.contains(clientId));
	}

	/**
	 * Tool definition as shown to AI clients: public name and description, upstream schemas and hints unchanged.
	 *
	 * @return public tool definition
	 */
	public McpSchema.Tool publicDefinition() {
		var description = descriptionOverride != null ? descriptionOverride : definition.description();
		return new McpSchema.Tool(publicName, publicName, description, definition.inputSchema(),
				definition.outputSchema(), definition.annotations(), definition.meta(), definition.icons());
	}

	RegisteredTool withDefinition(McpSchema.Tool newDefinition) {
		return new RegisteredTool(upstreamId, internalName, publicName, status, clients, descriptionOverride,
				newDefinition);
	}

	RegisteredTool withRules(ToolStatus newStatus, List<String> newClients, String newDescription) {
		return new RegisteredTool(upstreamId, internalName, publicName, newStatus, List.copyOf(newClients),
				newDescription, definition);
	}

	static String key(String upstreamId, String internalName) {
		return upstreamId + "/" + internalName;
	}

}
