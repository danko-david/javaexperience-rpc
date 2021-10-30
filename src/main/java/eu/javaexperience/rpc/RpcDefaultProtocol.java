package eu.javaexperience.rpc;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import eu.javaexperience.asserts.AssertArgument;
import eu.javaexperience.collection.map.KeyVal;
import eu.javaexperience.datareprez.DataArray;
import eu.javaexperience.datareprez.DataCommon;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.datareprez.convertFrom.DataWrapper;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.Mirror.BelongTo;
import eu.javaexperience.reflect.Mirror.ClassData;
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
		return extract(null, in);
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

	@Override
	public Object extract(Class retType, Object src)
	{
		if(null == src)
		{
			return null;
		}
		
		if(null != retType && retType.isAssignableFrom(src.getClass()))
		{
			return src;
		}
		
		if(src instanceof DataArray)
		{
			try
			{
				DataArray arr = (DataArray) src;
				
				if(null != retType && retType.isArray())
				{
					ArrayList ret = new ArrayList<>();
					Class cls = retType.getComponentType();
					for(int i=0;i<arr.size();++i)
					{
						ret.add(extract(cls, arr.get(i)));
					}
					
					return ret.toArray((Object[]) Array.newInstance(cls, 0));
				}
				else
				{
					Collection ret = null;
					
					if(null != retType)
					{
						for(Entry<Class, Class> a:collImpl)
						{
							if(retType.isAssignableFrom(a.getKey()))
							{
								ret = (Collection) a.getValue().newInstance();
							}
						}
					}
					
					if(null == ret)
					{
						ret = new ArrayList<>();
					}
					
					for(int i=0;i<arr.size();++i)
					{
						ret.add(extract(null, arr.get(i)));
					}
					return ret;
				}
			}
			catch(Exception e)
			{
				Mirror.propagateAnyway(e);
			}
		}
		else if(src instanceof DataObject)
		{
			try
			{
				DataObject obj = (DataObject) src;
				
				if(null != retType)
				{
					for(Entry<Class, Class> a:mapImpl)
					{
						if(retType.isAssignableFrom(a.getKey()))
						{
							Map ret = (Map) a.getValue().newInstance();
							
							for(String k:obj.keys())
							{
								ret.put(k, extract(null, obj.get(k)));
							}
							
							return ret;
						}
					}
					
					//if not a map type, try wrap into an object
					Object ret = createObject(retType, obj);
					if(null != ret)
					{
						ClassData cd = Mirror.getClassData(ret.getClass());
						
						for(String k:obj.keys())
						{
							Field f = cd.getFieldByName(k);
							if(null != f)
							{
								f.set(ret, extract(f.getType(), obj.get(k)));
							}
						}
						
						return ret;
					}
				}
				
				//otherwise return raw without any wrap.
			}
			catch(Exception e)
			{
				Mirror.propagateAnyway(e);
			}
		}
		
		return src;
	}
	
	protected static List<Entry<Class,Class>> mapImpl  = new ArrayList<>();
	protected static List<Entry<Class,Class>> collImpl  = new ArrayList<>();
	static
	{
		mapImpl.add(new KeyVal(ConcurrentMap.class, ConcurrentHashMap.class));
		mapImpl.add(new KeyVal(Map.class, HashMap.class));
		collImpl.add(new KeyVal(Set.class, HashSet.class));
		collImpl.add(new KeyVal(List.class, ArrayList.class));
		collImpl.add(new KeyVal(Collection.class, ArrayList.class));
		//implementation.add(new KeyVal(Entry.class, KeyVal.class));
	}
	
	protected Object createObject(Class request, DataObject obj) throws Exception
	{
		if(obj.has("class"))
		{
			return Class.forName(obj.getString("class")).newInstance();
		}
		else
		{
			return request.newInstance();
		}
	}
}
