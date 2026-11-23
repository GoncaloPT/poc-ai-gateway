package pt.goncalo.aigateway.edge;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.EntityResponse;
import org.springframework.web.servlet.function.ServerResponse;

class McpErrorSanitizerTest {

	@Test
	void mcpErrorBodyBecomesPlainJsonRpcError() {
		var original = ServerResponse.badRequest()
			.body(McpError.builder(McpSchema.ErrorCodes.INVALID_REQUEST).message("Invalid message format").build());

		var sanitized = McpErrorSanitizer.sanitize(original);

		assertThat(sanitized.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(entity(sanitized)).containsEntry("jsonrpc", "2.0")
			.containsEntry("id", null)
			.containsEntry("error", Map.of("code", McpSchema.ErrorCodes.INVALID_REQUEST, "message",
					"Invalid message format"))
			.doesNotContainKey("stackTrace");
	}

	@Test
	void otherExceptionBodyBecomesGenericInternalError() {
		var original = ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(new IllegalStateException("secret detail"));

		var sanitized = McpErrorSanitizer.sanitize(original);

		assertThat(entity(sanitized)).containsEntry("error",
				Map.of("code", McpSchema.ErrorCodes.INTERNAL_ERROR, "message", "Internal server error"));
	}

	@Test
	void regularResponsesAreUntouched() {
		var ok = ServerResponse.ok().body("{\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}");
		var empty = ServerResponse.accepted().build();

		assertThat(McpErrorSanitizer.sanitize(ok)).isSameAs(ok);
		assertThat(McpErrorSanitizer.sanitize(empty)).isSameAs(empty);
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> entity(ServerResponse response) {
		return (Map<String, Object>) ((EntityResponse<?>) response).entity();
	}

}
