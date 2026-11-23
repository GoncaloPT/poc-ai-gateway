package pt.goncalo.aigateway.upstream;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import pt.goncalo.aigateway.registry.AiGatewayProperties;
import pt.goncalo.aigateway.registry.ToolRegistry;

/**
 * Periodically reads {@code tools/list} of every upstream and applies it to the registry.
 * <p>
 * If an upstream is unreachable its tools are left untouched: they stay listed and calls fail with "temporarily
 * unavailable" until it is back.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class UpstreamDiscovery {

	private final UpstreamClients upstreamClients;

	private final ToolRegistry registry;

	private final AiGatewayProperties properties;

	private final TaskScheduler taskScheduler;

	@EventListener(ApplicationReadyEvent.class)
	void start() {
		taskScheduler.scheduleWithFixedDelay(this::refreshAll, Instant.now(), properties.discoveryInterval());
	}

	/**
	 * Refreshes all upstreams. One failing upstream does not affect the others.
	 */
	void refreshAll() {
		for (var upstreamId : upstreamClients.upstreamIds()) {
			try {
				registry.syncUpstream(upstreamId, upstreamClients.listTools(upstreamId));
			}
			catch (RuntimeException ex) {
				log.warn("Discovery of upstream {} failed, keeping last known tools: {}", upstreamId, ex.getMessage());
			}
		}
	}

}
