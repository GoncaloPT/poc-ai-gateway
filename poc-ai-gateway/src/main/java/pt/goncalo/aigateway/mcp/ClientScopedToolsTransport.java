package pt.goncalo.aigateway.mcp;

import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import reactor.core.publisher.Mono;
import pt.goncalo.aigateway.audit.AuditEntry;
import pt.goncalo.aigateway.audit.AuditLogger;
import pt.goncalo.aigateway.caller.Caller;
import pt.goncalo.aigateway.registry.ToolRegistry;

/**
 * Decorates the MCP server transport to make the tool list client specific.
 * <p>
 * The SDK server holds one global tool list (all enabled tools). This decorator wraps the server's request handler:
 * <ul>
 * <li>{@code tools/list}: removes tools the calling client is not granted.</li>
 * <li>{@code tools/call}: rejects tools the client is not granted before the SDK touches them, with the same error the
 * SDK uses for unknown tools, so clients cannot probe which tools exist.</li>
 * </ul>
 */
class ClientScopedToolsTransport implements McpStatelessServerTransport {

	private final McpStatelessServerTransport delegate;

	private final ToolRegistry registry;

	private final AuditLogger auditLogger;

	/**
	 * @param delegate real HTTP transport
	 * @param registry gateway registry
	 * @param auditLogger audit sink for denied calls
	 */
	ClientScopedToolsTransport(McpStatelessServerTransport delegate, ToolRegistry registry, AuditLogger auditLogger) {
		this.delegate = delegate;
		this.registry = registry;
		this.auditLogger = auditLogger;
	}

	@Override
	public void setMcpHandler(McpStatelessServerHandler handler) {
		delegate.setMcpHandler(new ClientScopedHandler(handler));
	}

	@Override
	public Mono<Void> closeGracefully() {
		return delegate.closeGracefully();
	}

	@Override
	public List<String> protocolVersions() {
		return delegate.protocolVersions();
	}

	private final class ClientScopedHandler implements McpStatelessServerHandler {

		private final McpStatelessServerHandler handler;

		private ClientScopedHandler(McpStatelessServerHandler handler) {
			this.handler = handler;
		}

		@Override
		public Mono<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext context,
				McpSchema.JSONRPCRequest request) {
			var caller = Caller.from(context);
			if (McpSchema.METHOD_TOOLS_CALL.equals(request.method())) {
				var toolName = toolName(request.params());
				if (!isCallable(toolName, caller)) {
					auditLogger.record(new AuditEntry(caller.clientId(), caller.userId(), request.method(), toolName,
							null, "DENIED_TOOL", 0));
					return Mono.just(McpSchema.JSONRPCResponse.error(request.id(),
							new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INVALID_PARAMS,
									"Unknown tool: invalid_tool_name", "Tool not found: " + toolName)));
				}
			}
			var response = handler.handleRequest(context, request);
			if (McpSchema.METHOD_TOOLS_LIST.equals(request.method())) {
				return response.map(result -> filterToolsList(result, caller));
			}
			return response;
		}

		@Override
		public Mono<Void> handleNotification(McpTransportContext context,
				McpSchema.JSONRPCNotification notification) {
			return handler.handleNotification(context, notification);
		}

		private McpSchema.JSONRPCResponse filterToolsList(McpSchema.JSONRPCResponse response, Caller caller) {
			if (!(response.result() instanceof McpSchema.ListToolsResult result)) {
				return response;
			}
			var visible = result.tools().stream().filter(tool -> isCallable(tool.name(), caller)).toList();
			return McpSchema.JSONRPCResponse.result(response.id(),
					new McpSchema.ListToolsResult(visible, result.nextCursor(), result.meta()));
		}

		private boolean isCallable(String publicName, Caller caller) {
			return publicName != null && registry.findByPublicName(publicName)
				.map(tool -> tool.isCallableBy(caller.clientId()))
				.orElse(false);
		}

		private static String toolName(Object params) {
			return params instanceof Map<?, ?> map && map.get("name") instanceof String name ? name : null;
		}

	}

}
