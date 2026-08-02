package llms.devs.a2a.server;

import io.grpc.*;
import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.Client;
import org.a2aproject.sdk.client.MessageEvent;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransport;
import org.a2aproject.sdk.client.transport.jsonrpc.JSONRPCTransportConfigBuilder;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallContext;
import org.a2aproject.sdk.client.transport.spi.interceptors.ClientCallInterceptor;
import org.a2aproject.sdk.client.transport.spi.interceptors.PayloadAndHeaders;
import org.a2aproject.sdk.common.A2AHeaders;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.A2AError;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.spec.UnsupportedOperationError;
import org.a2aproject.sdk.transport.grpc.context.GrpcContextKeys;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.Locale.ROOT;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true")
public class SendImmediateResponseMessageIntTest {
    @Value("${server.port}")
    private int port;

    private ManagedChannel grpcChannel;

    @BeforeEach
    public void setup() {
        grpcChannel = ManagedChannelBuilder.forAddress("localhost", port)
                .usePlaintext()
                .build();
    }

    @Test
    public void testSendMessage() throws Exception {

        final CompletableFuture<String> messageResponse = new CompletableFuture<>();

        AgentCard card = A2A.getAgentCard("http://localhost:" + port);

        try (Client client = Client.builder(card)
                .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> grpcChannel))
                .addConsumer((event, agentCard) -> {
                    if (event instanceof MessageEvent me) {
                        messageResponse.complete("" + me.getMessage().parts());
                    }
                })
                .streamingErrorHandler((error) -> {
                    System.err.println("Streaming error occurred: " + error.getMessage());
                    error.printStackTrace();
                    messageResponse.completeExceptionally(error);
                })
                .build()) {

            // Send messages - client automatically closed when done
            client.sendMessage(A2A.toUserMessage("Tell me a joke"));

            String responseText = messageResponse.get();
            System.out.println("Response: " + responseText);
        }
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public AgentExecutor agentExecutor() {
            return new AgentExecutor() {
                @Override
                public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
                    emitter.sendMessage("Hello World");
                }

                @Override
                public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
                    throw new UnsupportedOperationError();
                }
            };
        }
    }
}
