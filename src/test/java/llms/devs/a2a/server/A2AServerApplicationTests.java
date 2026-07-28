package llms.devs.a2a.server;

import org.springframework.boot.test.context.SpringBootTest;

// Start the real embedded Tomcat server on a random port so native Servlets initialize
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class A2AServerApplicationTests {
}