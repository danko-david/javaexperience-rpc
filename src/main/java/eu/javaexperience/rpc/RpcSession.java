package eu.javaexperience.rpc;

import eu.javaexperience.interfaces.ExternalDataAttached;
import eu.javaexperience.semantic.references.MayNotNull;

/**
 * An RpcSession represents an "established" session the the client.
 * 	This established session can be connection based, like TCP connection
 * 	or casual request mode (like RPC calls encapsulated in Http)
 * 
 * */
public interface RpcSession extends ExternalDataAttached
{
	public long getCreationTime();
	public long getLastAccessTime();
	
	public @MayNotNull RpcProtocolHandler getDefaultRpcProtocolHandler();
	
	public void destroy();
}
