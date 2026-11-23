package pt.goncalo.aigateway.edge;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * Settings of the HTTP edge (Spring Cloud Gateway route on {@code /mcp}).
 *
 * @param rateLimit per client+user request budget
 * @param maxRequestSize larger request bodies get {@code 413}
 */
@ConfigurationProperties("ai-gateway.edge")
record EdgeProperties(RateLimit rateLimit, DataSize maxRequestSize) {

	/**
	 * @param capacity requests allowed per period
	 * @param period refill period
	 */
	record RateLimit(long capacity, Duration period) {
	}

}
