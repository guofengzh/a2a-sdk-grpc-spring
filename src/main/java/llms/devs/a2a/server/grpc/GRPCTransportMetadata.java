package llms.devs.a2a.server.grpc;

import org.a2aproject.sdk.server.TransportMetadata;
import org.a2aproject.sdk.spec.TransportProtocol;
import org.jspecify.annotations.NullMarked;

public class GRPCTransportMetadata implements TransportMetadata {
    @Override
    @NullMarked
    public String getTransportProtocol() {
        return TransportProtocol.GRPC.asString();
    }
}
