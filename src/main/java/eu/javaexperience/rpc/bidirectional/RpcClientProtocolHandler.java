package eu.javaexperience.rpc.bidirectional;

import eu.javaexperience.rpc.RpcProtocolCommon;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.RpcSession;

public interface RpcClientProtocolHandler<S extends RpcSession> extends RpcProtocolCommon
{
	public RpcRequest createClientRequest(S session);
	public void putPacketTraceId(RpcRequest req, String tid);
	public void putThisParameter(RpcRequest req, Object thisParam);
	public void putNamespace(RpcRequest req, String namespace);
	public void putRequestFunctionName(RpcRequest req, String functName);
	public void putParameters(RpcRequest req, Object[] params);
	public Object extractReturningValue(RpcRequest req);
	public Throwable extractException(RpcRequest req);
}
