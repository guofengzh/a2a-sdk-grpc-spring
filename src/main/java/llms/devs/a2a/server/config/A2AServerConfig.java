package llms.devs.a2a.server.config;

import java.util.concurrent.Executor;

import llms.devs.a2a.server.context.DefaultCallContextFactory;
import llms.devs.a2a.server.grpc.AgentGrpcHandler;
import org.a2aproject.sdk.server.agentexecution.AgentExecutor;
import org.a2aproject.sdk.server.agentexecution.RequestContext;
import org.a2aproject.sdk.server.config.A2AConfigProvider;
import org.a2aproject.sdk.server.config.DefaultValuesConfigProvider;
import org.a2aproject.sdk.server.events.InMemoryQueueManager;
import org.a2aproject.sdk.server.events.MainEventBus;
import org.a2aproject.sdk.server.events.MainEventBusProcessor;
import org.a2aproject.sdk.server.events.QueueManager;
import org.a2aproject.sdk.server.requesthandlers.DefaultRequestHandler;
import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.server.tasks.*;
import org.a2aproject.sdk.spec.*;
import org.a2aproject.sdk.transport.grpc.handler.CallContextFactory;
import org.a2aproject.sdk.transport.grpc.handler.GrpcHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.server.service.GrpcService;

@Configuration
public class A2AServerConfig {

	private static final Logger logger = LoggerFactory.getLogger(A2AServerConfig.class);

	@Bean
	public TaskStore taskStore() {
		return new InMemoryTaskStore();
	}

	@Bean
	public MainEventBus mainEventBus() {
		return new MainEventBus();
	}

	@Bean
	public QueueManager queueManager(TaskStore taskStore, MainEventBus mainEventBus) {
		return new InMemoryQueueManager((TaskStateProvider) taskStore, mainEventBus);
	}

	@Bean
	public PushNotificationConfigStore pushNotificationConfigStore() {
		return new InMemoryPushNotificationConfigStore();
	}

	@Bean
	public PushNotificationSender pushNotificationSender(PushNotificationConfigStore pushNotificationConfigStore) {
		return new BasePushNotificationSender(pushNotificationConfigStore);
	}

	@Bean
    public A2AConfigProvider configProvider() {
		return new DefaultValuesConfigProvider();
	}

	@Bean
	public MainEventBusProcessor mainEventBusProcessor(MainEventBus mainEventBus,TaskStore taskStore,
													   PushNotificationSender pushSender,
													   QueueManager queueManager) {
		return new MainEventBusProcessor(mainEventBus, taskStore, pushSender, queueManager);
	}

	@Bean
	public RequestHandler requestHandler(AgentExecutor agentExecutor, TaskStore taskStore, QueueManager queueManager,
	                                     PushNotificationConfigStore pushConfigStore,
	                                     MainEventBusProcessor mainEventBusProcessor,
	                                     @Qualifier("a2aInternal") Executor executor) {
		return new DefaultRequestHandler(agentExecutor, taskStore, queueManager,
				pushConfigStore, mainEventBusProcessor,
				executor, executor);
	}

	@Bean
    public CallContextFactory callContextFactory() {
		return new DefaultCallContextFactory();
	}

	@Bean
	@GrpcService
    public GrpcHandler agentGrpcHandler(
			AgentCard agentCard,
			RequestHandler requestHandler,
			CallContextFactory callContextFactory,
			@Qualifier("a2aInternal") Executor executor
	) {
		return new AgentGrpcHandler(agentCard, requestHandler, callContextFactory, executor);
	}

	// Placeholder bean registration
	@Bean
	public AgentExecutor agentExecutor() {
		return new AgentExecutor() {
			@Override
			public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
				throw new UnsupportedOperationError();
			}

			@Override
			public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
				throw new UnsupportedOperationError();
			}
		};
	}
}
