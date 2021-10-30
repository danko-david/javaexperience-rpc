package eu.javaexperience.rpc.external_lang;

import java.io.IOException;
import java.net.ServerSocket;

import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.getBy.GetBy2;
import eu.javaexperience.io.IOStream;
import eu.javaexperience.io.fd.IOStreamFactory;
import eu.javaexperience.rpc.RpcTools;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.SocketRpcServer;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;

public class ExampleRpcServer
{
	public static void main(String[] args) throws IOException
	{
		final GetBy1<DataObject, SimpleRpcRequest> dispatcher = RpcTools.createSimpleNamespaceDispatcherWithDiscoverApi
		(
			QueueStorageExampleApi.instance
		);
		
		SocketRpcServer<IOStream, SimpleRpcSession> srv = RpcTools.newServer
		(
			IOStreamFactory.fromServerSocket(new ServerSocket(3000)),
			5,
			BidirectionalRpcDefaultProtocol.DEFAULT_PROTOCOL_HANDLER_WITH_CLASS,
			RpcTools.getSimpleSessionCreator(),
			
			new GetBy2<DataObject, SimpleRpcSession, DataObject>()
			{
				@Override
				public DataObject getBy(SimpleRpcSession a, DataObject b)
				{
					return dispatcher.getBy(new SimpleRpcRequest(a, b));
				}
			}
		);
		
		srv.start();
		
	}
}
