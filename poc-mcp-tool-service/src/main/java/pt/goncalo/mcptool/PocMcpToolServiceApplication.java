package pt.goncalo.mcptool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample microservice that exposes the same capability over REST and MCP.
 */
@SpringBootApplication
public class PocMcpToolServiceApplication {

	/**
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(PocMcpToolServiceApplication.class, args);
	}

}
