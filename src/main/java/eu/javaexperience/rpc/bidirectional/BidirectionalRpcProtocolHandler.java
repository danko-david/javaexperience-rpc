package eu.javaexperience.rpc.bidirectional;

import eu.javaexperience.rpc.RpcProtocolHandler;
import eu.javaexperience.rpc.RpcSession;

public interface BidirectionalRpcProtocolHandler<S extends RpcSession> extends RpcProtocolHandler, RpcClientProtocolHandler
{
}
