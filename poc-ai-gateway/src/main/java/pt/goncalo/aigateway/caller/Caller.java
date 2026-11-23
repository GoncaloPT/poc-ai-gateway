package pt.goncalo.aigateway.caller;

import java.util.HashMap;
import java.util.Map;

import io.modelcontextprotocol.common.McpTransportContext;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.function.ServerRequest;

/**
 * Who is calling the gateway. Extracted once from the inbound HTTP request and carried through the MCP stack in the
 * {@link McpTransportContext}.
 * <p>
 * POC simplification: client and user come from headers. In production both come from the validated OAuth access
 * token (e.g. {@code azp} and {@code sub} claims).
 *
 * @param clientId AI client id
 * @param userId end user on whose behalf the client calls
 * @param authorization inbound {@code Authorization} header, forwarded unchanged to upstreams
 */
public record Caller(String clientId, String userId, String authorization) {

	/** Header carrying the AI client id (POC stand-in for the token's client claim). */
	public static final String CLIENT_ID_HEADER = "X-Client-Id";

	/** Header carrying the user id (POC stand-in for the token's subject claim). */
	public static final String USER_ID_HEADER = "X-User-Id";

	private static final String CLIENT_ID = "clientId";

	private static final String USER_ID = "userId";

	private static final String AUTHORIZATION = "authorization";

	/**
	 * @param request inbound HTTP request
	 * @return caller of the request
	 */
	public static Caller from(ServerRequest request) {
		var headers = request.headers();
		var userId = headers.firstHeader(USER_ID_HEADER);
		return new Caller(headers.firstHeader(CLIENT_ID_HEADER), userId != null ? userId : "anonymous",
				headers.firstHeader(HttpHeaders.AUTHORIZATION));
	}

	/**
	 * @param context MCP transport context filled by {@link #toTransportContext()}
	 * @return caller stored in the context
	 */
	public static Caller from(McpTransportContext context) {
		return new Caller((String) context.get(CLIENT_ID), (String) context.get(USER_ID),
				(String) context.get(AUTHORIZATION));
	}

	/**
	 * @return MCP transport context holding this caller
	 */
	public McpTransportContext toTransportContext() {
		var metadata = new HashMap<String, Object>();
		putIfPresent(metadata, CLIENT_ID, clientId);
		putIfPresent(metadata, USER_ID, userId);
		putIfPresent(metadata, AUTHORIZATION, authorization);
		return McpTransportContext.create(Map.copyOf(metadata));
	}

	private static void putIfPresent(Map<String, Object> metadata, String key, String value) {
		if (value != null) {
			metadata.put(key, value);
		}
	}

}
