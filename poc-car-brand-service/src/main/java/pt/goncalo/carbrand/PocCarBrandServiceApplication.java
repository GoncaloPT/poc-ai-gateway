package pt.goncalo.carbrand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Second sample microservice: manages a list of car brands over REST and MCP.
 */
@SpringBootApplication
public class PocCarBrandServiceApplication {

	/**
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(PocCarBrandServiceApplication.class, args);
	}

}
