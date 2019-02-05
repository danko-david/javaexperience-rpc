package eu.javaexperience.rpc;

import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.semantic.references.MayNotNull;

/**
 * 
 * */
public interface RpcRequest
{
	public @MayNotNull RpcSession getRpcSession();
	
	public @MayNotNull RpcProtocolHandler getProtocolHandler();
	
	public DataObject getRequestData();
	public DataObject getResponseData();
	public DataObject fillResponse(DataObject response);
}