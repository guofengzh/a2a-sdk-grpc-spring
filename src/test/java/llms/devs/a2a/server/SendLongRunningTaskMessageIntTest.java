package llms.devs.a2a.server;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.a2aproject.sdk.A2A;
import org.a2aproject.sdk.client.*;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransport;
import org.a2aproject.sdk.client.transport.grpc.GrpcTransportConfigBuilder;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.tasks.AgentEmitter;
import org.a2aproject.sdk.spec.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
        properties = "spring.main.allow-bean-definition-overriding=true")
public class SendLongRunningTaskMessageIntTest {
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
        AgentCard card = A2A.getAgentCard("http://localhost:" + port);

        final CompletableFuture<String> messageResponse = new CompletableFuture<>();

        // Create consumers list for handling client events
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();
        consumers.add((event, agentCard) -> {
            if (event instanceof TaskUpdateEvent tue) {
                TaskState state = tue.getTask().status().state();
                switch (state) {
                    case TaskState.TASK_STATE_SUBMITTED -> System.out.println("Task created");
                    case TaskState.TASK_STATE_WORKING -> System.out.println("Agent is processing...");
                    case TaskState.TASK_STATE_COMPLETED -> System.out.println("Task finished");
                    case TaskState.TASK_STATE_FAILED -> System.err.println("Task failed: " + tue.getTask().status().message());
                }

                // Check for new artifacts
                if (tue.getUpdateEvent() instanceof TaskArtifactUpdateEvent update) {
                    Artifact artifact = update.artifact();
                    messageResponse.complete(extractTextFromParts(artifact.parts()));
                }
            }
        });

        // Create error handler for streaming errors
        Consumer<Throwable> streamingErrorHandler = (error) -> {
            System.err.println("Streaming error occurred: " + error.getMessage());
            error.printStackTrace();
            messageResponse.completeExceptionally(error);
        };

        try (Client client = Client.builder(card)
                .withTransport(GrpcTransport.class, new GrpcTransportConfigBuilder().channelFactory(target -> grpcChannel))
                .addConsumers(consumers)
                .streamingErrorHandler(streamingErrorHandler)
                .build()) {

            client.sendMessage(A2A.toUserMessage("Process this data"));

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
                    if (context.getTask() == null) {
                        emitter.submit();
                    }
                    emitter.startWork();

                    String userMessage = extractTextFromParts(context.getMessage().parts());

                    emitter.addArtifact(List.of(new TextPart(userMessage + " - completed")));
                    emitter.complete();
                }

                @Override
                public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
                    final Task task = context.getTask();
                    if (task == null) {
                        emitter.cancel();
                        return;
                    }

                    final TaskState state = task.status().state();
                    if ( state == TaskState.TASK_STATE_CANCELED || state == TaskState.TASK_STATE_COMPLETED) {
                        // task already canceled
                        throw new TaskNotCancelableError();
                    }

                    emitter.cancel();
                }
            };
        }
    }

    private static String extractTextFromParts(List<Part<?>> parts) {
        StringBuilder textBuilder = new StringBuilder();
        for (Part<?> part : parts) {
            if (part instanceof TextPart textPart) {
                textBuilder.append(textPart.text());
            }
        }
        return textBuilder.toString();
    }

}
