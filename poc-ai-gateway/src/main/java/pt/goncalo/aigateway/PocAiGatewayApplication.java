package pt.goncalo.aigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI gateway: MCP server towards external AI clients, MCP client towards internal MCP servers. Decides which internal
 * tools are exposed, to whom, and audits every call.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class PocAiGatewayApplication {

	/**
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(PocAiGatewayApplication.class, args);
	}

}
