package eu.javaexperience.rpc;

public class RpcSessionTools
{
	private final static ThreadLocal<RpcSession> SESSIONS = new ThreadLocal<>();
	
	public static RpcSession getCurrentRpcSession()
	{
		return SESSIONS.get();
	}
	
	public static RpcSession ensureGetCurrentRpcSession()
	{
		RpcSession ret = getCurrentRpcSession();
		if(null == ret)
		{
			throw new RuntimeException("No RPC session associated with the current request processor thread.");
		}
		
		return ret;
	}
	
	public static void setCurrentRpcSession(RpcSession session)
	{
		SESSIONS.set(session);
	}
}
