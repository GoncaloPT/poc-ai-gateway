package pt.goncalo.aigateway.registry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRegistryTest {

	private final List<Object> events = new ArrayList<>();

	private ToolRegistry registry;

	@BeforeEach
	void setUp() {
		var properties = new AiGatewayProperties(true, Duration.ofSeconds(30), Duration.ofSeconds(60),
				Map.of("svc", new AiGatewayProperties.Upstream("http://localhost", "/mcp", "tasks")),
				Map.of("chat", true, "ide", true, "old", false),
				List.of(new AiGatewayProperties.ToolSeed("svc", "create_task", "tasks_create", true, List.of("chat"),
						"Create a task")));
		registry = new ToolRegistry(properties, events::add);
	}

	@Test
	void seededToolIsEnabledAndUnknownToolIsDiscovered() {
		registry.syncUpstream("svc", List.of(tool("create_task"), tool("delete_task")));

		var create = registry.findByPublicName("tasks_create").orElseThrow();
		assertThat(create.status()).isEqualTo(ToolStatus.ENABLED);
		assertThat(create.publicDefinition().description()).isEqualTo("Create a task");
		var delete = registry.findByPublicName("tasks_delete_task").orElseThrow();
		assertThat(delete.status()).isEqualTo(ToolStatus.DISCOVERED);
		assertThat(delete.isCallableBy("chat")).isFalse();
		assertThat(events).hasSize(1);
	}

	@Test
	void grantRestrictsClientsAndEmptyGrantAllowsAll() {
		registry.syncUpstream("svc", List.of(tool("create_task"), tool("list_tasks")));
		registry.updateTool("svc/list_tasks", ToolStatus.ENABLED, List.of(), null);

		assertThat(registry.findByPublicName("tasks_create").orElseThrow().isCallableBy("ide")).isFalse();
		assertThat(registry.findByPublicName("tasks_create").orElseThrow().isCallableBy("chat")).isTrue();
		assertThat(registry.findByPublicName("tasks_list_tasks").orElseThrow().isCallableBy("ide")).isTrue();
	}

	@Test
	void vanishedToolIsRemovedAndRulesSurviveDefinitionChange() {
		registry.syncUpstream("svc", List.of(tool("create_task"), tool("list_tasks")));
		registry.updateTool("svc/list_tasks", ToolStatus.ENABLED, List.of(), "custom");

		registry.syncUpstream("svc", List.of(new McpSchema.Tool("list_tasks", null, "changed upstream",
				Map.of("type", "object"), null, null, null, null)));

		assertThat(registry.findByPublicName("tasks_create")).isEmpty();
		var list = registry.findByPublicName("tasks_list_tasks").orElseThrow();
		assertThat(list.status()).isEqualTo(ToolStatus.ENABLED);
		assertThat(list.publicDefinition().description()).isEqualTo("custom");
		assertThat(list.definition().description()).isEqualTo("changed upstream");
	}

	@Test
	void unknownOrDisabledClientIsNotEnabled() {
		assertThat(registry.isClientEnabled("chat")).isTrue();
		assertThat(registry.isClientEnabled("old")).isFalse();
		assertThat(registry.isClientEnabled("nobody")).isFalse();
		assertThat(registry.isClientEnabled(null)).isFalse();

		registry.setClientEnabled("chat", false);

		assertThat(registry.isClientEnabled("chat")).isFalse();
	}

	private static McpSchema.Tool tool(String name) {
		return new McpSchema.Tool(name, null, name + " description", Map.of("type", "object"), null, null, null,
				null);
	}

}
