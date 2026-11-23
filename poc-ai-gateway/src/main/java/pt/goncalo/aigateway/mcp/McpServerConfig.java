package pt.goncalo.aigateway.mcp;

import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.spec.McpStatelessServerTransport;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import pt.goncalo.aigateway.audit.AuditLogger;
import pt.goncalo.aigateway.caller.Caller;
import pt.goncalo.aigateway.registry.ToolRegistry;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wires the outward MCP server (Spring AI, stateless Streamable HTTP). Overrides two Spring AI auto-configured beans:
 * <ul>
 * <li>the HTTP transport, to put the {@link Caller} into every request's MCP transport context;</li>
 * <li>the transport used by the MCP server, decorated with {@link ClientScopedToolsTransport}.</li>
 * </ul>
 * The router (third override) is the gateway route in {@code edge.EdgeConfig}.
 */
@Configuration(proxyBeanMethods = false)
class McpServerConfig {

	/**
	 * Replaces Spring AI's default transport ({@code @ConditionalOnMissingBean}).
	 */
	@Bean
	WebMvcStatelessServerTransport webMvcStatelessServerTransport(
			@Qualifier("mcpServerJsonMapper") JsonMapper jsonMapper, McpServerStreamableHttpProperties properties) {
		return WebMvcStatelessServerTransport.builder()
			.jsonMapper(new JacksonMcpJsonMapper(jsonMapper))
			.messageEndpoint(properties.getMcpEndpoint())
			.contextExtractor(request -> Caller.from(request).toTransportContext())
			.build();
	}

	/**
	 * Transport handed to the MCP server. {@code @Primary} so the server picks the decorator, while the router keeps
	 * using the concrete HTTP transport.
	 */
	@Bean
	@Primary
	McpStatelessServerTransport clientScopedToolsTransport(WebMvcStatelessServerTransport transport,
			ToolRegistry registry, AuditLogger auditLogger) {
		return new ClientScopedToolsTransport(transport, registry, auditLogger);
	}

}
