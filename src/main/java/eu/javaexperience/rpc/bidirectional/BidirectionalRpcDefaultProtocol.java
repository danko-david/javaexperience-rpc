package eu.javaexperience.rpc.bidirectional;

import eu.javaexperience.datareprez.DataArray;
import eu.javaexperience.datareprez.DataCommon;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.datareprez.convertFrom.DataWrapper;
import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.reflect.Mirror.BelongTo;
import eu.javaexperience.reflect.Mirror.FieldSelector;
import eu.javaexperience.reflect.Mirror.Select;
import eu.javaexperience.reflect.Mirror.Visibility;
import eu.javaexperience.rpc.RpcDefaultProtocol;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.RpcSession;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.SimpleRpcSession;

public class BidirectionalRpcDefaultProtocol<S extends RpcSession> extends RpcDefaultProtocol implements BidirectionalRpcProtocolHandler<S>
{
	public BidirectionalRpcDefaultProtocol(DataCommon proto)
	{
		this(proto, DEFAULT_RPC_DATA_WRAPPER);
	}
	
	public BidirectionalRpcDefaultProtocol(DataCommon proto, DataWrapper dataWrapper)
	{
		super(proto, dataWrapper);
	}
	
	@Override
	public void putPacketTraceId(RpcRequest req, String tid)
	{
		req.getRequestData().putString("t", tid);
	}

	@Override
	public void putRequestFunctionName(RpcRequest req, String functName)
	{
		req.getRequestData().putString("f", functName);
	}

	@Override
	public void putParameters(RpcRequest req, Object[] params)
	{
		req.getRequestData().putArray("p", (DataArray) wrap(params));
	}

	@Override
	public Object extractReturningValue(RpcRequest req)
	{
		if(!req.getResponseData().has("r"))
		{
			return null;
		}
		
		return extract(req.getResponseData().get("r"));
	}

	@Override
	public Throwable extractException(RpcRequest req)
	{
		if(!req.getResponseData().has("e"))
		{
			return null;
		}
		
		Object ret = extract(req.getResponseData().get("e"));
		if(null == ret)
		{
			return null;
		}
		
		return new ClientRequestException(((DataObject)ret).getImpl());
	}

	@Override
	public void putThisParameter(RpcRequest req, Object thisParam)
	{
		DataReprezTools.put(req.getRequestData(), "_", thisParam);
	}
	
	@Override
	public RpcRequest createClientRequest(RpcSession session)
	{
		return new SimpleRpcRequest(session);
	}
	
	public static final DataWrapper DEFAULT_RPC_DATA_WRAPPER = DataReprezTools.combineWrappers
	(
		DataReprezTools.WRAP_ARRAY_COLLECTION_MAP,
		DataReprezTools.WRAP_DATA_LIKE,
		DataReprezTools.WRAP_CLASS__OBJECT_WITH_PROPERTY,
		DataReprezTools.createClassInstanceWrapper(new FieldSelector(true, Visibility.Public, BelongTo.Instance, Select.All, Select.IsNot, Select.All))
	);
	
	public static final DataWrapper DEFAULT_RPC_DATA_WRAPPER_WITH_CLASS = DataReprezTools.combineWrappers
	(
		DataReprezTools.WRAP_ARRAY_COLLECTION_MAP,
		DataReprezTools.WRAP_ENUM,
		DataReprezTools.WRAP_DATA_LIKE,
		DataReprezTools.WRAP_CLASS__OBJECT_WITH_PROPERTY,
		DataReprezTools.createClassInstanceWrapper(new FieldSelector(true, Visibility.Public, BelongTo.Instance, Select.All, Select.IsNot, Select.All), "class")
	);
	
	public static final BidirectionalRpcDefaultProtocol<SimpleRpcSession> DEFAULT_PROTOCOL_HANDLER = new BidirectionalRpcDefaultProtocol<>(new DataObjectJsonImpl(), DEFAULT_RPC_DATA_WRAPPER);
	
	public static final BidirectionalRpcDefaultProtocol<SimpleRpcSession> DEFAULT_PROTOCOL_HANDLER_WITH_CLASS = new BidirectionalRpcDefaultProtocol<>(new DataObjectJsonImpl(), DEFAULT_RPC_DATA_WRAPPER_WITH_CLASS);

	@Override
	public void putNamespace(RpcRequest req, String namespace)
	{
		DataReprezTools.put(req.getRequestData(), "N", namespace);
	}
}
