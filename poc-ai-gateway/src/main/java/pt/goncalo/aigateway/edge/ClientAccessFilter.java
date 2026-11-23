package pt.goncalo.aigateway.edge;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import pt.goncalo.aigateway.audit.AuditEntry;
import pt.goncalo.aigateway.audit.AuditLogger;
import pt.goncalo.aigateway.caller.Caller;
import pt.goncalo.aigateway.registry.ToolRegistry;

/**
 * First filter of the gateway route: global kill switch ({@code 503}) and client allowlist ({@code 403}). Being
 * outermost, it also audits rejections of the filters and the transport after it (rate limit {@code 429}, request
 * size {@code 413}, invalid request {@code 400}, internal error {@code 500}).
 */
@Component
@RequiredArgsConstructor
class ClientAccessFilter implements HandlerFilterFunction<ServerResponse, ServerResponse> {

	private final ToolRegistry registry;

	private final AuditLogger auditLogger;

	@Override
	public ServerResponse filter(ServerRequest request, HandlerFunction<ServerResponse> next) throws Exception {
		var caller = Caller.from(request);
		if (!registry.isGatewayEnabled()) {
			return reject(caller, "DENIED_GATEWAY_DISABLED", HttpStatus.SERVICE_UNAVAILABLE, "AI gateway is disabled");
		}
		if (!registry.isClientEnabled(caller.clientId())) {
			return reject(caller, "DENIED_CLIENT", HttpStatus.FORBIDDEN, "Client is not allowed");
		}
		try {
			var response = next.handle(request);
			auditEdgeRejection(caller, response.statusCode());
			return response;
		}
		catch (ResponseStatusException ex) {
			auditEdgeRejection(caller, ex.getStatusCode());
			throw ex;
		}
	}

	private void auditEdgeRejection(Caller caller, HttpStatusCode status) {
		var outcome = switch (status.value()) {
			case 429 -> "DENIED_RATE_LIMIT";
			case 413 -> "DENIED_REQUEST_SIZE";
			case 400 -> "DENIED_INVALID_REQUEST";
			case 500 -> "ERROR";
			default -> null;
		};
		if (outcome != null) {
			audit(caller, outcome);
		}
	}

	private ServerResponse reject(Caller caller, String outcome, HttpStatus status, String message) {
		audit(caller, outcome);
		return ServerResponse.status(status).contentType(MediaType.APPLICATION_JSON).body("""
				{"jsonrpc":"2.0","id":null,"error":{"code":-32000,"message":"%s"}}""".formatted(message));
	}

	private void audit(Caller caller, String outcome) {
		auditLogger.record(new AuditEntry(caller.clientId(), caller.userId(), null, null, null, outcome, 0));
	}

}
