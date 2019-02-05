package eu.javaexperience.rpc;

import eu.javaexperience.rpc.bidirectional.RpcClientProtocolHandler;
import eu.javaexperience.semantic.references.MayNotNull;

public interface RpcClientSession
{
	public @MayNotNull RpcClientProtocolHandler getClientRpcProtocolHandler();
}