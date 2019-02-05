package eu.javaexperience.rpc;

import java.util.Map;

import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcProtocolHandler;
import eu.javaexperience.semantic.references.MayNotNull;

public class SimpleRpcRequest implements RpcRequest
{
	protected final RpcSession session;
	protected final DataObject request;
	protected DataObject response;
	
	protected final Map<String, Object> reqProps = new SmallMap<>();
	
	public SimpleRpcRequest(RpcSession session)
	{
		this.session = session;
		this.request = session.getDefaultRpcProtocolHandler().getDefaultCommunicationProtocolPrototype().newObjectInstance();
	}
	
	public SimpleRpcRequest(RpcSession session, DataObject request)
	{
		this.session = session;
		this.request = request;
	}
	
	@Override
	public @MayNotNull RpcSession getRpcSession()
	{
		return session;
	}

	@Override
	public @MayNotNull RpcProtocolHandler getProtocolHandler()
	{
		return session.getDefaultRpcProtocolHandler();
	}
	
	@Override
	public DataObject getRequestData()
	{
		return request;
	}

	@Override
	public DataObject getResponseData()
	{
		return response;
	}
	
	public Map<String, Object> getRequestProperties()
	{
		return reqProps;
	}

	@Override
	public DataObject fillResponse(DataObject response)
	{
		DataObject prev = this.response;
		this.response = response;
		return prev;
	}
}
