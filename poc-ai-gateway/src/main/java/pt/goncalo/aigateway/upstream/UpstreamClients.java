package pt.goncalo.aigateway.upstream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import pt.goncalo.aigateway.caller.Caller;
import pt.goncalo.aigateway.registry.AiGatewayProperties;

/**
 * MCP clients towards the internal MCP servers, one per upstream (MCP Java SDK, Streamable HTTP).
 * <p>
 * The caller's identity is forwarded on every tool call: the SDK passes a {@link McpTransportContext} to the HTTP
 * request customizer, which copies the caller's {@code Authorization} header. The upstream therefore runs its own
 * access checks for the real user.
 */
@Slf4j
@Component
public class UpstreamClients implements DisposableBean {

	// The SDK reads the context through a Supplier on the calling thread, so a ThreadLocal hands it the caller.
	private static final ThreadLocal<Caller> CURRENT_CALLER = new ThreadLocal<>();

	private final AiGatewayProperties properties;

	private final Map<String, McpSyncClient> clients = new ConcurrentHashMap<>();

	/**
	 * @param properties gateway configuration
	 */
	public UpstreamClients(AiGatewayProperties properties) {
		this.properties = properties;
	}

	/**
	 * @return ids of all configured upstreams
	 */
	public List<String> upstreamIds() {
		return List.copyOf(properties.upstreams().keySet());
	}

	/**
	 * Reads all tools of an upstream, following pagination. Runs with the gateway's own identity.
	 *
	 * @param upstreamId upstream id
	 * @return upstream tool definitions
	 */
	public List<McpSchema.Tool> listTools(String upstreamId) {
		return withClient(upstreamId, client -> {
			var tools = new ArrayList<McpSchema.Tool>();
			String cursor = null;
			do {
				var page = client.listTools(cursor);
				tools.addAll(page.tools());
				cursor = page.nextCursor();
			}
			while (cursor != null);
			return tools;
		});
	}

	/**
	 * Calls a tool on an upstream on behalf of a caller.
	 *
	 * @param upstreamId upstream id
	 * @param caller caller whose identity is forwarded
	 * @param request call with the internal tool name
	 * @return upstream result
	 */
	public McpSchema.CallToolResult callTool(String upstreamId, Caller caller, McpSchema.CallToolRequest request) {
		CURRENT_CALLER.set(caller);
		try {
			return withClient(upstreamId, client -> client.callTool(request));
		}
		finally {
			CURRENT_CALLER.remove();
		}
	}

	@Override
	public void destroy() {
		clients.values().forEach(McpSyncClient::closeGracefully);
	}

	private <T> T withClient(String upstreamId, Function<McpSyncClient, T> action) {
		var client = clients.computeIfAbsent(upstreamId, this::connect);
		try {
			return action.apply(client);
		}
		catch (RuntimeException ex) {
			// Drop the client so the next call reconnects, e.g. after the upstream restarted.
			clients.remove(upstreamId, client);
			client.closeGracefully();
			throw ex;
		}
	}

	private McpSyncClient connect(String upstreamId) {
		var upstream = properties.upstreams().get(upstreamId);
		if (upstream == null) {
			throw new IllegalArgumentException("Unknown upstream " + upstreamId);
		}
		var transport = HttpClientStreamableHttpTransport.builder(upstream.url())
			.endpoint(upstream.endpoint())
			.httpRequestCustomizer((builder, method, uri, body, context) -> {
				var caller = Caller.from(context);
				if (caller.authorization() != null) {
					builder.header(HttpHeaders.AUTHORIZATION, caller.authorization());
				}
				if (caller.userId() != null) {
					builder.header(Caller.USER_ID_HEADER, caller.userId());
				}
			})
			.build();
		var client = McpClient.sync(transport)
			.clientInfo(McpSchema.Implementation.builder("ai-gateway", "0.0.1").build())
			.requestTimeout(properties.upstreamTimeout())
			.transportContextProvider(() -> {
				var caller = CURRENT_CALLER.get();
				return caller != null ? caller.toTransportContext() : McpTransportContext.EMPTY;
			})
			.build();
		client.initialize();
		log.info("Connected to upstream {} at {}{}", upstreamId, upstream.url(), upstream.endpoint());
		return client;
	}

}
