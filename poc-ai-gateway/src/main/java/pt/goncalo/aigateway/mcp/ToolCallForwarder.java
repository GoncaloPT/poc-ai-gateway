package pt.goncalo.aigateway.mcp;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import pt.goncalo.aigateway.audit.AuditEntry;
import pt.goncalo.aigateway.audit.AuditLogger;
import pt.goncalo.aigateway.caller.Caller;
import pt.goncalo.aigateway.registry.ToolRegistry;
import pt.goncalo.aigateway.upstream.UpstreamClients;

/**
 * Executes a public tool call: re-checks the grant, maps the public name to the internal one, forwards the call with
 * the caller's identity to the owning upstream, and audits the outcome.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ToolCallForwarder {

	private final ToolRegistry registry;

	private final UpstreamClients upstreamClients;

	private final AuditLogger auditLogger;

	/**
	 * @param context MCP transport context holding the {@link Caller}
	 * @param request call with the public tool name
	 * @return upstream result, or an error result if the upstream is unavailable
	 */
	McpSchema.CallToolResult call(McpTransportContext context, McpSchema.CallToolRequest request) {
		var started = System.nanoTime();
		var caller = Caller.from(context);
		// Defense in depth: ClientScopedToolsTransport already filtered, but the registry may have changed since.
		var tool = registry.findByPublicName(request.name())
			.filter(candidate -> candidate.isCallableBy(caller.clientId()))
			.orElseThrow(() -> {
				audit(caller, request.name(), null, "DENIED_TOOL", started);
				return McpError.builder(McpSchema.ErrorCodes.INVALID_PARAMS)
					.message("Unknown tool: invalid_tool_name")
					.data("Tool not found: " + request.name())
					.build();
			});

		var internalRequest = new McpSchema.CallToolRequest(tool.internalName(), request.arguments(), request.meta());
		try {
			var result = upstreamClients.callTool(tool.upstreamId(), caller, internalRequest);
			audit(caller, request.name(), tool.upstreamId(), Boolean.TRUE.equals(result.isError()) ? "TOOL_ERROR" : "OK",
					started);
			return result;
		}
		catch (RuntimeException ex) {
			log.warn("Call of {} on upstream {} failed: {}", tool.internalName(), tool.upstreamId(), ex.getMessage());
			audit(caller, request.name(), tool.upstreamId(), "UNAVAILABLE", started);
			return McpSchema.CallToolResult.builder()
				.isError(true)
				.addTextContent("Tool %s is temporarily unavailable. Try again later.".formatted(request.name()))
				.build();
		}
	}

	private void audit(Caller caller, String tool, String upstream, String outcome, long started) {
		auditLogger.record(new AuditEntry(caller.clientId(), caller.userId(), McpSchema.METHOD_TOOLS_CALL, tool,
				upstream, outcome, (System.nanoTime() - started) / 1_000_000));
	}

}
