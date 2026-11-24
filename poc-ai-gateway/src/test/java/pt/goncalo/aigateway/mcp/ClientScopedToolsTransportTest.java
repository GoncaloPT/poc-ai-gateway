package pt.goncalo.aigateway.mcp;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpStatelessServerHandler;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import pt.goncalo.aigateway.audit.AuditLogger;
import pt.goncalo.aigateway.caller.Caller;
import pt.goncalo.aigateway.registry.AiGatewayProperties;
import pt.goncalo.aigateway.registry.ToolRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class ClientScopedToolsTransportTest {

	private final AtomicReference<McpStatelessServerHandler> installed = new AtomicReference<>();

	private McpStatelessServerHandler handler;

	@BeforeEach
	void setUp() {
		var properties = new AiGatewayProperties(true, Duration.ofSeconds(30), Duration.ofSeconds(60),
				Map.of("svc", new AiGatewayProperties.Upstream("http://localhost", "/mcp", "tasks")),
				Map.of("chat", true, "ide", true),
				List.of(new AiGatewayProperties.ToolSeed("svc", "create_task", "tasks_create", true, List.of("chat"),
						null),
						new AiGatewayProperties.ToolSeed("svc", "list_tasks", "tasks_list", true, List.of(), null)));
		var registry = new ToolRegistry(properties, event -> {
		});
		registry.syncUpstream("svc", List.of(tool("create_task"), readOnlyTool("list_tasks")));

		var transport = new ClientScopedToolsTransport(new CapturingTransport(), registry, new AuditLogger());
		transport.setMcpHandler(new FakeServerHandler());
		handler = installed.get();
	}

	@Test
	void toolsListIsFilteredPerClient() {
		assertThat(listToolNames("chat")).containsExactly("tasks_create", "tasks_list");
		assertThat(listToolNames("ide")).containsExactly("tasks_list");
	}

	@Test
	void callOfNotGrantedToolIsRejectedBeforeReachingServer() {
		var response = handler.handleRequest(context("ide"), new McpSchema.JSONRPCRequest("2.0",
				McpSchema.METHOD_TOOLS_CALL, 1, Map.of("name", "tasks_create"))).block();

		assertThat(response.error()).isNotNull();
		assertThat(response.error().code()).isEqualTo(McpSchema.ErrorCodes.INVALID_PARAMS);
	}

	@Test
	void callOfGrantedToolReachesServer() {
		var response = handler.handleRequest(context("chat"), new McpSchema.JSONRPCRequest("2.0",
				McpSchema.METHOD_TOOLS_CALL, 1, Map.of("name", "tasks_create"))).block();

		assertThat(response.error()).isNull();
		assertThat(response.result()).isEqualTo("called");
	}

	private List<String> listToolNames(String clientId) {
		var response = handler
			.handleRequest(context(clientId), new McpSchema.JSONRPCRequest("2.0", McpSchema.METHOD_TOOLS_LIST, 1, null))
			.block();
		return ((McpSchema.ListToolsResult) response.result()).tools().stream().map(McpSchema.Tool::name).toList();
	}

	private static McpTransportContext context(String clientId) {
		return new Caller(clientId, "alice", null).toTransportContext();
	}

	private static McpSchema.Tool readOnlyTool(String name) {
		return new McpSchema.Tool(name, null, null, Map.of("type", "object"), null,
				new McpSchema.ToolAnnotations(null, true, false, true, false, null), null, null);
	}

	private static McpSchema.Tool tool(String name) {
		return new McpSchema.Tool(name, null, null, Map.of("type", "object"), null, null, null, null);
	}

	/** Stands in for the SDK server: lists every published tool, answers every call. */
	private static final class FakeServerHandler implements McpStatelessServerHandler {

		@Override
		public Mono<McpSchema.JSONRPCResponse> handleRequest(McpTransportContext context,
				McpSchema.JSONRPCRequest request) {
			Object result = McpSchema.METHOD_TOOLS_LIST.equals(request.method())
					? new McpSchema.ListToolsResult(List.of(tool("tasks_create"), tool("tasks_list")), null)
					: "called";
			return Mono.just(McpSchema.JSONRPCResponse.result(request.id(), result));
		}

		@Override
		public Mono<Void> handleNotification(McpTransportContext context,
				McpSchema.JSONRPCNotification notification) {
			return Mono.empty();
		}

	}

	private final class CapturingTransport implements McpStatelessServerTransport {

		@Override
		public void setMcpHandler(McpStatelessServerHandler mcpHandler) {
			installed.set(mcpHandler);
		}

		@Override
		public Mono<Void> closeGracefully() {
			return Mono.empty();
		}

	}

}
