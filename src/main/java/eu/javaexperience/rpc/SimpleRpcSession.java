package eu.javaexperience.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import eu.javaexperience.semantic.references.MayNotNull;

public class SimpleRpcSession implements RpcSession
{
	protected RpcProtocolHandler handler;
	protected long creationTime = System.currentTimeMillis();
	protected long lastAccess;
	
	protected Map<String, Object> attr = new ConcurrentHashMap<String, Object>();
	
	public Object get(String key)
	{
		return attr.get(key);
	}
	
	public void put(String key, Object value)
	{
		if(null == value)
		{
			attr.remove(key);
		}
		else
		{
			attr.put(key, value);
		}
	}
	
	public SimpleRpcSession(RpcProtocolHandler handler)
	{
		this.handler = handler;
	}
	
	public long getLastAccess()
	{
		return lastAccess;
	}
	
	@Override
	public long getCreationTime()
	{
		return creationTime;
	}

	@Override
	public long getLastAccessTime()
	{
		return lastAccess;
	}

	@Override
	public @MayNotNull RpcProtocolHandler getDefaultRpcProtocolHandler()
	{
		return handler;
	}

	@Override
	public void destroy()
	{
		
	}

	public void setProtocolHandler(RpcProtocolHandler defaultProtocol)
	{
		handler = defaultProtocol;
	}

	@Override
	public Map<String, Object> getExtraDataMap()
	{
		return attr;
	}
}
