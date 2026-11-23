package pt.goncalo.aigateway.registry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Gateway configuration and registry seed.
 *
 * @param enabled global kill switch for all AI traffic
 * @param discoveryInterval how often upstream tool lists are read
 * @param upstreamTimeout per-call timeout towards upstreams
 * @param upstreams internal MCP servers by id (target: discovered by Kubernetes labels)
 * @param clients known AI clients and whether they are enabled (in production: OAuth client id from the token)
 * @param tools tools approved up front. Any other discovered tool starts as {@link ToolStatus#DISCOVERED}.
 */
@ConfigurationProperties("ai-gateway")
public record AiGatewayProperties(@DefaultValue("true") boolean enabled,
		@DefaultValue("30s") Duration discoveryInterval, @DefaultValue("60s") Duration upstreamTimeout,
		@DefaultValue Map<String, Upstream> upstreams, @DefaultValue Map<String, Boolean> clients,
		@DefaultValue List<ToolSeed> tools) {

	/**
	 * Internal MCP server.
	 *
	 * @param url base URL
	 * @param endpoint MCP endpoint path
	 * @param domain prefix for public tool names
	 */
	public record Upstream(String url, @DefaultValue("/mcp") String endpoint, String domain) {
	}

	/**
	 * Tool approval known at startup.
	 *
	 * @param upstream upstream id
	 * @param internalName tool name on the upstream
	 * @param publicName optional public name, default {@code <domain>_<internalName>}
	 * @param enabled whether the tool is exposed
	 * @param clients granted clients, empty = every enabled client
	 * @param description optional description override
	 */
	public record ToolSeed(String upstream, String internalName, String publicName,
			@DefaultValue("true") boolean enabled, @DefaultValue List<String> clients, String description) {
	}

}
