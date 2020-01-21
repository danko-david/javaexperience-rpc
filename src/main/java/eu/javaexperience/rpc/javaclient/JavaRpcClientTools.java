package eu.javaexperience.rpc.javaclient;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.Socket;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReceiver;
import eu.javaexperience.datareprez.DataSender;
import eu.javaexperience.interfaces.simple.SimpleGet;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.io.IOStream;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.NotatedCaster;
import eu.javaexperience.rpc.RpcCastTools;
import eu.javaexperience.rpc.RpcProtocolHandler;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.RpcTools;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.bidirectional.RpcClientProtocolHandler;
import eu.javaexperience.semantic.references.MayNull;
import eu.javaexperience.url.UrlDownloadTools;

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
						return a.objectFromBlob(UrlDownloadTools.download(null, url, null, a.toBlob()));
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
	
	protected static final ConcurrentMap<String, Class> CLASS_LOOKUP = new ConcurrentHashMap<>();
	
	protected static Object extractReturnOrThrow(RpcRequest req, Class<?> retType, RpcProtocolHandler proto) throws Throwable
	{
		RpcClientProtocolHandler<?> hand = (RpcClientProtocolHandler<?>) req.getProtocolHandler();
		Throwable t = hand.extractException(req);
		if(null != t)
		{
			throw t;
		}
		
		Object ret = hand.extractReturningValue(req);
		return proto.extract(retType, ret);
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
				Object ex = extractReturnOrThrow(req, method.getReturnType(), proto);
				
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
	
	public static JavaRpcParallelClient createClientWithIpPort(String ip, int port, RpcProtocolHandler proto) throws IOException
	{
		Socket s = new Socket(ip, port);
		return createClientWithSocket(s, proto);
	}
	
	public static JavaRpcParallelClient createClientWithSocket(Socket s, RpcProtocolHandler proto) throws IOException
	{
		return createClientWithTxRx
		(
			proto.getDefaultCommunicationProtocolPrototype().newDataSender(s.getOutputStream()),
			proto.getDefaultCommunicationProtocolPrototype().newDataReceiver(s.getInputStream()),
			proto
		);
	}
	
	public static JavaRpcParallelClient createClientWithIOAndProto(IOStream io, RpcProtocolHandler proto) throws IOException
	{
		return createClientWithTxRx
		(
			proto.getDefaultCommunicationProtocolPrototype().newDataSender(io.getOutputStream()),
			proto.getDefaultCommunicationProtocolPrototype().newDataReceiver(io.getInputStream()),
			proto
		);
	}
	
	public static JavaRpcParallelClient createClientWithTxRx(DataSender send, DataReceiver rec, RpcProtocolHandler proto)
	{
		return new JavaRpcParallelClient(send, rec, proto);
	}
	
	/**
	 * post and longpoll
	 * */
	public static JavaRpcParallelClient createClientHttp(URL url, RpcProtocolHandler proto)
	{
		return new JavaRpcParallelClient
		(
			(SimplePublish1<DataObject>)obj->
			{
				try
				{
					UrlDownloadTools.download(null, url, null, obj.toBlob());
				}
				catch (IOException e)
				{
					Mirror.propagateAnyway(e);
				}
			}, 
			(SimpleGet<DataObject>)()->
			{
				try
				{
					return proto.getDefaultCommunicationProtocolPrototype().objectFromBlob(UrlDownloadTools.download(null, url, null));
				}
				catch (IOException e)
				{
					Mirror.propagateAnyway(e);
					return null;
				}
			},
			proto
		);
	}
}
