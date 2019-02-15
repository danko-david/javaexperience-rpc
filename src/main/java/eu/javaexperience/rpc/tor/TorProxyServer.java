package eu.javaexperience.rpc.tor;

import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.io.IOStream;
import eu.javaexperience.io.IOTools;
import eu.javaexperience.io.fd.IOStreamFactory;
import eu.javaexperience.parse.ParsePrimitive;
import eu.javaexperience.proxy.TorProxySpawner;
import eu.javaexperience.proxy.TorSpawnerStorage;
import eu.javaexperience.rpc.JavaClassRpcCollector;
import eu.javaexperience.rpc.JavaClassRpcFunctions;
import eu.javaexperience.rpc.RpcDefaultProtocol;
import eu.javaexperience.rpc.RpcTools;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.SocketRpcServer;
import eu.javaexperience.rpc.codegen.JavaRpcInterfaceGenerator;
import eu.javaexperience.time.TimeCalc;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;

public class TorProxyServer
{
	protected static TorSpawnerStorage TOR_SPAWN =
			new TorSpawnerStorage(TorProxySpawner.runtimeThrowCreate("./tor"), 1024, 10*TimeCalc.minutes_ms);
	
	static
	{
		IOTools.closeOnExit(TOR_SPAWN);
	}
	
	public static void main(String[] args) throws IOException
	{
		new File("./tor").mkdirs();
		RpcDefaultProtocol proto = new RpcDefaultProtocol(new DataObjectJsonImpl());
		
		JavaClassRpcCollector api = new JavaClassRpcFunctions(TorProxyServer.class);
		
		SocketRpcServer<IOStream, SimpleRpcSession> srv = RpcTools.newServer
		(
			IOStreamFactory.fromServerSocket(new ServerSocket(9049)), 
			5, 
			proto, 
			RpcTools.getSimpleSessionCreator(), 
			(a, b)->api.dispatch(new SimpleRpcRequest(a, b))
		);
		srv.start();
		
		System.out.println
		(
			RpcTools.generateRpcClassWithBuilder
			(
				JavaRpcInterfaceGenerator.BASIC_JAVA_SOURCE_BUILDER, 
				"TorProxyServer", 
				api.getFunctionList()
			)
		);
		
		String sp = System.getProperties().getProperty("start_proxies");
		Integer pre = ParsePrimitive.tryParseInt(sp);
		System.out.println("-Dstart_proxies: " + sp + " " + pre);
		if (pre != null)
		{
			for(int i=0;i<pre.intValue();i++)
			{
				try
				{
					get_proxy_offset(null, i);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	public static int get_proxy_offset(SimpleRpcRequest req, int index) throws InterruptedException, IOException
	{
		synchronized (TOR_SPAWN)
		{
			return TOR_SPAWN.getAtOffset(index).getPort();
		}
	}
}