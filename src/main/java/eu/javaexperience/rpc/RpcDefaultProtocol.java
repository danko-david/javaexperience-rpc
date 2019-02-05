package eu.javaexperience.rpc;

import eu.javaexperience.asserts.AssertArgument;
import eu.javaexperience.datareprez.DataArray;
import eu.javaexperience.datareprez.DataCommon;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.datareprez.convertFrom.DataWrapper;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.Mirror.BelongTo;
import eu.javaexperience.reflect.Mirror.FieldSelector;
import eu.javaexperience.reflect.Mirror.Select;
import eu.javaexperience.reflect.Mirror.Visibility;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;
import eu.javaexperience.rpc.function.RpcFunctionParameter;
import eu.javaexperience.text.Format;

/**
 * for basic value extractions, for the exact parameter value tranformation
 * the function's cast is the responsible unit.
 * 
 * */
public class RpcDefaultProtocol implements RpcProtocolHandler
{
	protected final DataCommon transferDatatype;
	protected DataWrapper dataWrapper;
	
	public RpcDefaultProtocol(DataCommon proto)
	{
		this(proto, BidirectionalRpcDefaultProtocol.DEFAULT_RPC_DATA_WRAPPER);
	}
	
	public RpcDefaultProtocol(DataCommon proto, DataWrapper wrapper)
	{
		AssertArgument.assertNotNull(transferDatatype = proto, "transferDatatype");
		AssertArgument.assertNotNull(dataWrapper = wrapper, "dataWrapper");
	}
	
	protected static final FieldSelector fs = new FieldSelector(true, Visibility.Public, BelongTo.Instance, Select.All, Select.IsNot, Select.IsNot);
	
	protected FieldSelector object_mapper_field_selector = fs;
	
	public void setObjectMapperFieldSelector(FieldSelector fs)
	{
		AssertArgument.assertNotNull(fs, "fieldSelector");
		object_mapper_field_selector = fs;
	}
	
	@Override
	public Object wrap(Object in)
	{
		if(null == in)
		{
			return null;
		}
		
		if(null != DataReprezTools.isStorable(in))
		{
			return in;
		}
		
		return dataWrapper.wrap(dataWrapper, transferDatatype, in);
	}

	@Override
	public Object extract(Object in)
	{
		//TODO array unpack
		if(transferDatatype.isNull(in))
		{
			return null;
		}
		return in;
	}

	@Override
	public Object extractValue(DataObject object, String key)
	{
		return extract(object.get(key));
	}

	@Override
	public Object extractValue(DataArray array, int index)
	{
		return extract(array.get(index));
	}
	
	@Override
	public Object extractThisContext(RpcRequest req)
	{
		DataObject obj = req.getRequestData();
		if(obj.has("_"))
		{
			return extract(obj.get("_"));
		}
		
		return null;
	}

	@Override
	public boolean putValue(DataObject object, String key, Object value)
	{
		return null != DataReprezTools.put(object, key, wrap(value));
	}

	@Override
	public boolean putValue(DataArray arr, int index, Object value)
	{
		return null != DataReprezTools.put(arr, index, wrap(value));
	}

	@Override
	public DataCommon getDefaultCommunicationProtocolPrototype()
	{
		return transferDatatype;
	}
	
	@Override
	public String extractNamespace(RpcRequest requestObject)
	{
		return requestObject.getRequestData().optString("N");
	}
	
	@Override
	public String getPacketTraceId(RpcRequest requestObject)
	{
		DataObject obj = requestObject.getRequestData();
		if(obj.has("t"))
		{
			return requestObject.getRequestData().get("t").toString();
		}
		return null;
	}

	@Override
	public String getRequestFunctionName(RpcRequest requestObject)
	{
		return requestObject.getRequestData().optString("f");
	}
	
	protected void putPacketTrace(RpcRequest request, DataObject result)
	{
		String trace = getPacketTraceId(request);
		if(null != trace)
		{
			result.putString("t", trace);
		}
	}

	@Override
	public Object[] extractParameters(RpcRequest requestObject)
	{
		DataArray o = requestObject.getRequestData().optArray("p");
		if(null != o)
		{
			Object[] ret = new Object[o.size()];
			for(int i=0;i<ret.length;++i)
			{
				ret[i] = extract(o.get(i));
			}
			return ret;
		}
		
		return Mirror.emptyObjectArray;
	}

	@Override
	public DataObject createEmptyResponsePacket(RpcRequest request)
	{
		DataObject ret = transferDatatype.newObjectInstance();
		String trace = getPacketTraceId(request);
		if(null != trace)
		{
			ret.putString("t", trace);
		}
		return ret;
	}

	@Override
	public DataObject createReturningValue(RpcRequest req, Object result)
	{
		//TODO
		DataObject ret = transferDatatype.newObjectInstance();
		putObject(ret, "r", wrap(result));
		putPacketTrace(req, ret);
		return ret;
	}

	@Override
	public DataObject createException(RpcRequest req, Throwable exception)
	{
		DataObject ret = reportException(req.getRequestData(), exception);
		putPacketTrace(req, ret);
		return ret;
	}
	
	public static DataObject reportException(DataCommon proto, Throwable t)
	{
		DataObject ret = proto.newObjectInstance();
		DataObject ex = proto.newObjectInstance();
		String msg = t.getMessage();
		if(null != msg)
		{
			ex.putString("message", msg);
		}
		ex.putString("type", t.getClass().getSimpleName());
		ex.putString("detail", Format.getPrintedStackTrace(t));
		
		putObject(ret, "e", ex);
		return ret;
	}
	
	public static Object castObject(Object in, RpcFunctionParameter param)
	{
		return param.getCaster().cast(in);
	}

	public static void putObject(DataObject ret, String key, Object value)
	{
		DataReprezTools.put(ret, key, value);
	}

	/*
	Object t = in.opt("t");
	String f = in.optString("f");
	F func = findFunction(ctx, f);
	try
	{
		if(null != func)
		{
			Object[] params = extractParameters(ctx, in, "p", func.getParameterClasses());
			beforeCall(ctx, func, params, in);
			Object result = func.call(ctx, f, params);
			DataObject ret = wrapReturn(ctx, in, result, func.getReturningClass());
			if(null != t)
			{
				putObject(ret, "t", ret);
			}
			return ret;
		}
		else
		{
			DataObject ret = wrapReturn(ctx, in, unservedRequest(ctx, f, in), null);
			if(null != t)
			{
				putObject(ret, "t", ret);
			}
			return ret;
		}
	}
	catch(Throwable e)
	{
		DataObject ret = reportException(ctx, in, f, e);
		if(null != t)
		{
			putObject(ret, "t", ret);
		}
		return ret;
	}
	*/
	

}
