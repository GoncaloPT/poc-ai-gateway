package pt.goncalo.aigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Starts the full context. Discovery against the (absent) upstream fails and is only logged.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PocAiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
