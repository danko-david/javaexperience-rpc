package eu.javaexperience.rpc.external_lang;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.rpc.JavaClassRpcFunctions;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.SimpleRpcSession;

public class QueueStorageExampleApi extends JavaClassRpcFunctions<SimpleRpcRequest>
{
	public QueueStorageExampleApi()
	{
		super(QueueStorageExampleApi.class);
	}
	
	protected static QueueStorageExampleApi instance = new QueueStorageExampleApi();
	
	protected static Queue<String> QUEUE = new LinkedBlockingQueue<String>();
	
	public static void staticAdd(SimpleRpcRequest req, String add)
	{
		if(null == add)
		{
			throw new RuntimeException("Value may not null");
		}
		
		QUEUE.add(add);
	}
	
	public static String staticGetValuable(SimpleRpcRequest req)
	{
		String ret = QUEUE.poll();
		if(null == ret)
		{
			throw new RuntimeException("No value in the queue.");
		}
		return ret;
	}
	
	public static String staticTryGetOrNull(SimpleRpcRequest req)
	{
		return QUEUE.poll(); 
	}
	
	public static String staticPeek(SimpleRpcRequest req)
	{
		return QUEUE.peek();
	}
	
	public static String[] staticGetAll(SimpleRpcRequest req)
	{
		return QUEUE.toArray(Mirror.emptyStringArray);
	}
	
	public static void staticClear(SimpleRpcRequest req)
	{
		QUEUE.clear();
	}

	
	protected static Queue<String> getOrCreateSessionQueue(SimpleRpcRequest req)
	{
		SimpleRpcSession sess = (SimpleRpcSession) req.getRpcSession();
		if(null == sess)
		{
			throw new RuntimeException("No session associated with this request.");
		}
		
		Queue<String> q = (Queue<String>) sess.get("QUEUE");
		if(null != q)
		{
			sess.put("QUEUE", q = new LinkedBlockingQueue<String>());
		}
		
		return q;
	}
	
	public static void sessionAdd(SimpleRpcRequest req, String add)
	{
		if(null == add)
		{
			throw new RuntimeException("Value may not null");
		}
		
		getOrCreateSessionQueue(req).add(add);
	}
	
	public static String sessionGetValuable(SimpleRpcRequest req)
	{
		String ret = getOrCreateSessionQueue(req).poll();
		if(null == ret)
		{
			throw new RuntimeException("No value in the getOrCreateSessionQueue(req).");
		}
		return ret;
	}
	
	public static String sessionTryGetOrNull(SimpleRpcRequest req)
	{
		return getOrCreateSessionQueue(req).poll(); 
	}
	
	public static String sessionPeek(SimpleRpcRequest req)
	{
		return getOrCreateSessionQueue(req).peek();
	}
	
	public static String[] sessionGetAll(SimpleRpcRequest req)
	{
		return getOrCreateSessionQueue(req).toArray(Mirror.emptyStringArray);
	}
	
	public static void sessionClear(SimpleRpcRequest req)
	{
		getOrCreateSessionQueue(req).clear();
	}
}
