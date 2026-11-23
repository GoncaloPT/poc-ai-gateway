package pt.goncalo.aigateway.audit;

/**
 * Audit record of one MCP call. Metadata only, never arguments or results.
 *
 * @param clientId AI client id
 * @param userId calling user
 * @param method JSON-RPC method, {@code null} if rejected before parsing
 * @param tool public tool name, {@code null} for non tool calls
 * @param upstream upstream id the call went to, {@code null} if not forwarded
 * @param outcome {@code OK}, {@code TOOL_ERROR}, {@code UNAVAILABLE}, {@code ERROR} or {@code DENIED_*}
 * @param durationMs time spent in the gateway incl. upstream
 */
public record AuditEntry(String clientId, String userId, String method, String tool, String upstream, String outcome,
		long durationMs) {
}
