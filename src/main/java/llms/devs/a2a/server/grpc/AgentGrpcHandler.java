package llms.devs.a2a.server.grpc;

import org.a2aproject.sdk.server.requesthandlers.RequestHandler;
import org.a2aproject.sdk.spec.AgentCard;
import org.a2aproject.sdk.transport.grpc.handler.CallContextFactory;
import org.a2aproject.sdk.transport.grpc.handler.GrpcHandler;

import java.util.concurrent.Executor;

public class AgentGrpcHandler extends GrpcHandler {
    private final AgentCard agentCard;
    private final RequestHandler requestHandler;
    private final CallContextFactory callContextFactory;
    private final Executor executor;

    public AgentGrpcHandler(
            AgentCard agentCard,
            RequestHandler requestHandler,
            CallContextFactory callContextFactory,
            Executor executor
    ) {
        this.agentCard = agentCard;
        this.requestHandler = requestHandler;
        this.callContextFactory = callContextFactory;
        this.executor = executor;
    }

    @Override
    protected RequestHandler getRequestHandler() {
        return requestHandler;
    }

    @Override
    protected AgentCard getAgentCard() {
        return agentCard;
    }

    @Override
    protected AgentCard getExtendedAgentCard() {
        return null;
    }

    @Override
    protected CallContextFactory getCallContextFactory() {
        return callContextFactory;
    }

    @Override
    protected Executor getExecutor() {
        return executor;
    }
}
