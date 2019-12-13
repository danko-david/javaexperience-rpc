package eu.javaexperience.rpc.external_lang.java;

import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;
import eu.javaexperience.rpc.javaclient.JavaRpcClientTools;
import eu.javaexperience.rpc.javaclient.JavaRpcParallelClient;
import eu.javaexperience.text.StringTools;

public class TestRpcQueue
{
	public interface QueueStorageExampleApi
	{
		public String sessionTryGetOrNull();
		public String staticPeek();
		public void staticClear();
		public void sessionAdd(String a);
		public String sessionPeek();
		public String staticGetValuable();
		public void staticAdd(String a);
		public String sessionGetValuable();
		public String[] sessionGetAll();
		public String[] staticGetAll();
		public void sessionClear();
		public String staticTryGetOrNull();
	}	
	
	public static void main(String[] args) throws Throwable
	{
		JavaRpcParallelClient cli = JavaRpcClientTools.createClientWithIpPort("127.0.0.1", 3000, BidirectionalRpcDefaultProtocol.DEFAULT_PROTOCOL_HANDLER_WITH_CLASS);
		
		cli.getServerEventMediator().addEventListener((o)->System.out.println("Unhandled package: "+o.getImpl()));
		QueueStorageExampleApi api = cli.createApiObject(QueueStorageExampleApi.class, "QueueStorageExampleApi");
		
		for(int i=0;i<30;++i)
		{
			startPopThread(cli);
			startAddThread(api);
		}
	}
	
	protected static void startAddThread(QueueStorageExampleApi api)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				while(true)
				{
					try
					{
						Thread.sleep(500);
					}
					catch (InterruptedException e)
					{
						Mirror.propagateAnyway(e);
					}
					api.staticAdd(StringTools.randomString(10));
				}			
			}
		}.start();
	}
	
	protected static void startPopThread(JavaRpcParallelClient cli)
	{
		new Thread()
		{
			public void run()
			{
				while(true)
				{
					QueueStorageExampleApi api = cli.createApiObject(QueueStorageExampleApi.class, "QueueStorageExampleApi");
					try
					{
						Thread.sleep(500);
					}
					catch (InterruptedException e)
					{
						Mirror.propagateAnyway(e);
					}
					String val = api.staticTryGetOrNull();
					//if(null != val)
					{
						System.out.println("Pop value: "+val);
					}
				}
			};
		}.start();
	}
}
