package eu.javaexperience.rpc.javaclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import eu.javaexperience.collection.map.KeyVal;
import eu.javaexperience.datareprez.DataArray;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReceiver;
import eu.javaexperience.datareprez.DataSender;
import eu.javaexperience.interfaces.simple.SimpleGet;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.io.IOStream;
import eu.javaexperience.reflect.CastTo;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.Mirror.ClassData;
import eu.javaexperience.reflect.NotatedCaster;
import eu.javaexperience.rpc.RpcCastTools;
import eu.javaexperience.rpc.RpcProtocolHandler;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.RpcTools;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.bidirectional.RpcClientProtocolHandler;
import eu.javaexperience.semantic.references.MayNull;

public class JavaRpcClientTools
{
	public static <T> T createApiWithIpPort(Class<T> type, String ip, int port, @MayNull String namespace, RpcProtocolHandler proto) throws IOException
	{
		Socket s = new Socket(ip, port);
		return createApiWithSocket
		(
			type,
			s,
			namespace,
			proto
		);
	}
	
	public static <T> T createApiWithSocket(Class<T> type, Socket s, @MayNull String namespace, RpcProtocolHandler proto) throws IOException
	{
		return createApiWithTxRx
		(
			type,
			proto.getDefaultCommunicationProtocolPrototype().newDataSender(s.getOutputStream()),
			proto.getDefaultCommunicationProtocolPrototype().newDataReceiver(s.getInputStream()),
			namespace,
			proto
		);
	}
	
	public static <T> T createApiWithIOAndProto(Class<T> type, IOStream io,  @MayNull String namespace, final RpcProtocolHandler proto) throws IOException
	{
		return createApiWithTxRx
		(
			type,
			proto.getDefaultCommunicationProtocolPrototype().newDataSender(io.getOutputStream()),
			proto.getDefaultCommunicationProtocolPrototype().newDataReceiver(io.getInputStream()),
			namespace,
			proto
		);
	}
	
	public static <T> T createApiWithTxRx(Class<T> type, final DataSender send, final DataReceiver rec, @MayNull final String namespace, final RpcProtocolHandler proto)
	{
		return createApiWithTransactionHandler
		(
			type,
			new GetBy1<DataObject, DataObject>()
			{
				@Override
				public DataObject getBy(DataObject a)
				{
					try
					{
						send.send(a);
						return rec.receiveDataObject();
					} catch (IOException e)
					{
						Mirror.propagateAnyway(e);
						return null;
					}
				}
			},
			namespace,
			proto
		);
	}
	
	public static <T> T createApiHttp(Class<T> type, final URL url, @MayNull final String namespace, final RpcProtocolHandler proto)
	{
		return createApiWithTransactionHandler
		(
			type,
			new GetBy1<DataObject, DataObject>()
			{
				@Override
				public DataObject getBy(DataObject a)
				{
					try
					{
						URLConnection connection = url.openConnection();
						byte[] post = a.toBlob();
						
						if(null != post)
						{
							connection.addRequestProperty("Content-Length", String.valueOf(post.length));
							connection.setDoOutput(true);
							try(OutputStream os = connection.getOutputStream())
							{
								os.write(post);
								os.flush();
							}
						}
						
						try(InputStream is = connection.getInputStream())
						{
							int ep = 0;
							int read = 0;
							byte[] ret = new byte[10240];
							
							while((read = is.read(ret, ep, ret.length-ep))>0)
							{
								if(ep + read == ret.length)
								{
									ret = Arrays.copyOf(ret, ret.length*2);
								}
								
								ep+= read;
							}
							
							return a.objectFromBlob(Arrays.copyOf(ret, ep));
						}
					}
					catch(IOException e)
					{
						Mirror.propagateAnyway(e);
						return null;
					}
				}
			},
			namespace,
			proto
		);
	}
	
	public static <T> T createApiHttp(Class<T> type, final SimpleGet<URLConnection> connectionCreator, @MayNull final String namespace, final RpcProtocolHandler proto)
	{
		return createApiWithTransactionHandler
		(
			type,
			new GetBy1<DataObject, DataObject>()
			{
				@Override
				public DataObject getBy(DataObject a)
				{
					try
					{
						URLConnection connection = connectionCreator.get();
						byte[] post = a.toBlob();
						
						if(null != post)
						{
							connection.addRequestProperty("Content-Length", String.valueOf(post.length));
							connection.setDoOutput(true);
							try(OutputStream os = connection.getOutputStream())
							{
								os.write(post);
								os.flush();
							}
						}
						
						try(InputStream is = connection.getInputStream())
						{
							int ep = 0;
							int read = 0;
							byte[] ret = new byte[10240];
							
							while((read = is.read(ret, ep, ret.length-ep))>0)
							{
								if(ep + read == ret.length)
								{
									ret = Arrays.copyOf(ret, ret.length*2);
								}
								
								ep+= read;
							}
							
							return a.objectFromBlob(Arrays.copyOf(ret, ep));
						}
					}
					catch(IOException e)
					{
						Mirror.propagateAnyway(e);
						return null;
					}
				}
			},
			namespace,
			proto
		);
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
	
	protected static final ConcurrentMap<String, Class> CLASS_LOOKUP = new ConcurrentHashMap<>();
	

	public static <T> Object extractToJavaObject(Object src, Class retType) throws ClassNotFoundException, InstantiationException, IllegalAccessException, NoSuchFieldException, SecurityException
	{
		if(null == src)
		{
			return null;
		}
		
		/*if(retType.isAssignableFrom(src.getClass()))
		{
			return src;
		}*/
		
		if(retType.isAssignableFrom(src.getClass()))
		{
			return src;
		}

		if(src instanceof DataArray)
		{
			DataArray arr = (DataArray) src;
			
			if(retType.isArray())
			{
				ArrayList ret = new ArrayList<>();
				Class cls = retType.getComponentType();
				for(int i=0;i<arr.size();++i)
				{
					ret.add(extractToJavaObject(arr.get(i), cls));
				}
				
				return ret.toArray((T[]) Array.newInstance(cls, 0));
			}
			else
			{
				for(Entry<Class, Class> a:collImpl)
				{
					if(retType.isAssignableFrom(a.getKey()))
					{
						Collection ret = (Collection) a.getValue().newInstance();
						
						for(int i=0;i<arr.size();++i)
						{
							ret.add(extractToJavaObject(arr.get(i), Object.class));
						}
						
						return ret;
					}
				}
			}
		}
		else if(src instanceof DataObject)
		{
			DataObject obj = (DataObject) src;
			for(Entry<Class, Class> a:mapImpl)
			{
				if(retType.isAssignableFrom(a.getKey()))
				{
					Map ret = (Map) a.getValue().newInstance();
					
					for(String k:obj.keys())
					{
						ret.put(k, extractToJavaObject(obj.get(k), Object.class));
					}
					
					return ret;
				}
			}
			
			Object ret = null;
			if(obj.has("class"))
			{
				ret = Class.forName(obj.getString("class")).newInstance();
			}
			else
			{
				ret = retType.newInstance();
			}
			
			ClassData cd = Mirror.getClassData(ret.getClass());
			
			for(String k:obj.keys())
			{
				Field f = cd.getFieldByName(k);
				if(null != f)
				{
					f.set(ret, extractToJavaObject(obj.get(k), f.getType()));
				}
			}
			
			return ret;
		}
		
		return src;
	}
	
	protected static Object extractReturnOrThrow(RpcRequest req, Class<?> retType) throws Throwable
	{
		RpcClientProtocolHandler<?> hand = (RpcClientProtocolHandler<?>) req.getProtocolHandler();
		Throwable t = hand.extractException(req);
		if(null != t)
		{
			throw t;
		}
		
		Object ret = hand.extractReturningValue(req);
		
		return extractToJavaObject(ret, retType);
	}
	
	public static <T> T createApiWithTransactionHandler(Class<T> type, final GetBy1<DataObject, DataObject> transact, @MayNull final String namespace, final RpcProtocolHandler proto)
	{
		return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{type}, new InvocationHandler()
		{
			AtomicLong tid = new AtomicLong();
			SimpleRpcSession session = new SimpleRpcSession(proto);
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
			{
				if(null == args)
				{
					args = Mirror.emptyObjectArray;
				}
				String func = method.getName();
				
				long id = tid.incrementAndGet();
				RpcRequest req = RpcTools.createClientNamespaceInvocation(session, id, namespace, null, method.getName(), args);
				DataObject ret = transact.getBy(req.getRequestData());
				req.fillResponse(ret);
				Object ex = extractReturnOrThrow(req, method.getReturnType());
				
				if(null != ex)
				{
					Class r = method.getReturnType();
					if(r.isAssignableFrom(ex.getClass()))
					{
						return ex;
					}
					
					NotatedCaster caster = RpcCastTools.tryCreateCaster(r);
					if(null != caster)
					{
						 Object e = caster.cast(ex);
						 if(null != e)
						 {
							 return e;
						 }
					}
				}
				return ex;
			}
		});
	}
}
