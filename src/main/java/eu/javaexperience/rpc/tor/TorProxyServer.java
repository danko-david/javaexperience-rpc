package eu.javaexperience.rpc.tor;

import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.io.IOStream;
import eu.javaexperience.io.fd.IOStreamFactory;
import eu.javaexperience.parse.ParsePrimitive;
import eu.javaexperience.proxy.TorProxySpawner;
import eu.javaexperience.rpc.JavaClassRpcCollector;
import eu.javaexperience.rpc.JavaClassRpcFunctions;
import eu.javaexperience.rpc.RpcDefaultProtocol;
import eu.javaexperience.rpc.RpcTools;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.SocketRpcServer;
import eu.javaexperience.rpc.codegen.JavaRpcInterfaceGenerator;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;

public class TorProxyServer
{
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
					getTorAtOffset(i);
				}
				catch (InterruptedException e)
				{
					e.printStackTrace();
				}
			}
		}
	}
	
	protected static TorProxySpawner TOR_SPAWN = TorProxySpawner.runtimeThrowCreate("./tor");
	protected static final HashMap<Integer, TorProxySpawner.TorProxy> TORS = new HashMap();
	
	protected static synchronized TorProxySpawner.TorProxy getTorAtOffset(int offset) throws InterruptedException, IOException
	{
		TorProxySpawner.TorProxy tp = TORS.get(Integer.valueOf(offset));
		if (tp == null)
		{
			tp = TOR_SPAWN.spawnWithOffset(offset);
			TORS.put(Integer.valueOf(offset), tp);
		}
		return tp;
	}
	
	public static int get_proxy_offset(SimpleRpcRequest req, int index) throws InterruptedException, IOException
	{
		return getTorAtOffset(index).getPort();
	}
}