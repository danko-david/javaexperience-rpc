package eu.javaexperience.rpc.external_lang;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

import eu.javaexperience.collection.CollectionTools;
import eu.javaexperience.collection.map.BulkTransitMap;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.getBy.GetBy2;
import eu.javaexperience.io.IOStream;
import eu.javaexperience.io.fd.IOStreamFactory;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.Mirror.BelongTo;
import eu.javaexperience.reflect.Mirror.FieldSelector;
import eu.javaexperience.reflect.Mirror.Select;
import eu.javaexperience.reflect.Mirror.Visibility;
import eu.javaexperience.rpc.RpcDefaultProtocol;
import eu.javaexperience.rpc.RpcFacility;
import eu.javaexperience.rpc.RpcTools;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.SocketRpcServer;

public class ExampleRpcServer
{
	public static void main(String[] args) throws IOException
	{
		RpcDefaultProtocol proto = new RpcDefaultProtocol(new DataObjectJsonImpl())
		{
			protected FieldSelector fs = new FieldSelector(true, Visibility.Public, BelongTo.Instance, Select.All, Select.IsNot, Select.IsNot);
			
			@Override
			public Object wrap(Object in)
			{
				Object ret = super.wrap(in);
				
				//ha az alapértelmezett megoldás nem tudta konvertálni.
				if(null != in && null == ret)
				{
					try
					{
						BulkTransitMap<String, Object> values = new BulkTransitMap<String, Object>();
						Mirror.extractFieldsToMap(in, values, fs);
						return wrap(values);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
				
				return ret;
			}
		};
		
		
		final GetBy1<DataObject, SimpleRpcRequest> dispatcher = RpcTools.createSimpleNamespaceDispatcherWithDiscoverApi(QueueStorageExampleApi.instance);
		
		SocketRpcServer<IOStream, SimpleRpcSession> srv = RpcTools.newServer
		(
			IOStreamFactory.fromServerSocket(new ServerSocket(3000)),
			5,
			proto,
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
