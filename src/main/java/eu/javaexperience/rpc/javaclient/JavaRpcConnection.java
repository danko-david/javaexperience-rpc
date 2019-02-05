package eu.javaexperience.rpc.javaclient;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReceiver;
import eu.javaexperience.datareprez.DataSender;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.rpc.RpcProtocolHandler;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcProtocolHandler;

/**
 * T: object represents the connection, can be a socket or just an URL, String, FileDescriptor etc.
 * */
public class JavaRpcConnection<C>
{
	protected final C connection;
	protected final BidirectionalRpcDefaultProtocol protocol;
	protected final DataSender sender;
	protected final DataReceiver receiver;
	
	//TODO packet queue
	@Deprecated
	public final GetBy1<DataObject, DataObject> transaction;
	
	public JavaRpcConnection(C connection, BidirectionalRpcDefaultProtocol protocol, DataSender ds, DataReceiver rec)
	{
		this.connection = connection;
		this.sender = ds;
		this.receiver = rec;
		this.protocol = protocol;
		this.transaction = new GetBy1<DataObject, DataObject>()
		{
			@Override
			public DataObject getBy(DataObject a)
			{
				try
				{
					synchronized(this)
					{
						sender.send(a);
						return receiver.receiveDataObject();
					}
				}
				catch(Exception e)
				{
					Mirror.propagateAnyway(e);
					return null;
				}
			}
		}; 
	}
	
	public static JavaRpcConnection<Socket> tcpConnect(String ip, int port, BidirectionalRpcDefaultProtocol proto) throws UnknownHostException, IOException
	{
		Socket socket = new Socket(ip, port);
		
		final DataReceiver rec = proto.getDefaultCommunicationProtocolPrototype().newDataReceiver(socket.getInputStream());
		final DataSender send = proto.getDefaultCommunicationProtocolPrototype().newDataSender(socket.getOutputStream());
		
		return new JavaRpcConnection<Socket>(socket, proto, send, rec);
	}

	public BidirectionalRpcProtocolHandler getProtocol()
	{
		return protocol;
	}
}
