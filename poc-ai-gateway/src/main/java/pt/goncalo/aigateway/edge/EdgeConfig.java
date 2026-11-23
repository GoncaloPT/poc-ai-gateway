package pt.goncalo.aigateway.edge;

import java.time.Duration;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.AsyncProxyManager;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStatelessServerTransport;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;
import pt.goncalo.aigateway.caller.Caller;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.requestSize;
import static org.springframework.cloud.gateway.server.mvc.filter.Bucket4jFilterFunctions.rateLimit;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

/**
 * HTTP edge: a Spring Cloud Gateway (Server MVC) route whose handler is the MCP server itself. No proxying, no
 * forward: the gateway filters wrap the MCP endpoint directly.
 */
@Configuration(proxyBeanMethods = false)
class EdgeConfig {

	/**
	 * Gateway route on the MCP endpoint. Uses Spring AI's router bean name, so it replaces the default (unfiltered)
	 * MCP router. Filters run in the order added; {@link ClientAccessFilter} must stay first, {@link MutableHeadersResponse}
	 * and {@link McpErrorSanitizer} last, so the sanitizer sees the transport's own response.
	 */
	@Bean
	RouterFunction<ServerResponse> webMvcStatelessServerRouterFunction(WebMvcStatelessServerTransport transport,
			ClientAccessFilter clientAccessFilter, EdgeProperties properties) {
		return route("mcp").add(transport.getRouterFunction())
			.filter(clientAccessFilter)
			.filter(rateLimit(config -> config.setCapacity(properties.rateLimit().capacity())
				.setPeriod(properties.rateLimit().period())
				.setKeyResolver(EdgeConfig::callerKey)))
			.before(requestSize(properties.maxRequestSize()))
			.filter(MutableHeadersResponse.filter())
			.filter(McpErrorSanitizer.filter())
			.build();
	}

	/**
	 * Bucket store of the rate limit. POC: in memory per instance. Target: Bucket4j Redis (Lettuce), shared by all
	 * replicas.
	 */
	@Bean
	AsyncProxyManager<String> rateLimitProxyManager() {
		return Bucket4jCaffeine.<String>builderFor(Caffeine.newBuilder().maximumSize(100_000))
			.expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofMinutes(1)))
			.build()
			.asAsync();
	}

	/**
	 * Rate limit key: one bucket per AI client and user. Unknown clients never get here ({@link ClientAccessFilter}).
	 */
	static String callerKey(ServerRequest request) {
		var caller = Caller.from(request);
		return caller.clientId() + ":" + caller.userId();
	}

}
