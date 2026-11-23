package pt.goncalo.aigateway.edge;

import java.util.LinkedHashMap;
import java.util.Map;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.EntityResponse;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Keeps internals out of error responses.
 * <p>
 * Spring AI's stateless MCP transport answers malformed or failing requests with an {@link McpError} as response
 * body. {@code McpError} is an exception, so Jackson serializes it with its stack trace, exposing class names and
 * library versions. This filter replaces such bodies with a plain JSON-RPC error (code and message only), keeping the
 * HTTP status.
 */
final class McpErrorSanitizer {

	private McpErrorSanitizer() {
	}

	/**
	 * Route filter that must sit directly around the MCP transport, so it sees the transport's original response.
	 *
	 * @return filter function
	 */
	static HandlerFilterFunction<ServerResponse, ServerResponse> filter() {
		return (request, next) -> sanitize(next.handle(request));
	}

	/**
	 * @param response response of the MCP transport
	 * @return the same response, or a JSON-RPC error without internals if the body is an exception
	 */
	static ServerResponse sanitize(ServerResponse response) {
		if (!(response instanceof EntityResponse<?> entityResponse)
				|| !(entityResponse.entity() instanceof Throwable throwable)) {
			return response;
		}
		var error = throwable instanceof McpError mcpError && mcpError.getJsonRpcError() != null
				? mcpError.getJsonRpcError()
				: new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.INTERNAL_ERROR,
						"Internal server error", null);
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("jsonrpc", McpSchema.JSONRPC_VERSION);
		body.put("id", null);
		body.put("error", Map.of("code", error.code(), "message", error.message()));
		return ServerResponse.status(response.statusCode()).contentType(MediaType.APPLICATION_JSON).body(body);
	}

}
