package eu.javaexperience.rpc;

import java.io.IOException;

import eu.javaexperience.asserts.AssertArgument;
import eu.javaexperience.datareprez.DataCommon;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReceiver;
import eu.javaexperience.datareprez.DataSender;
import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.exceptions.OperationSuccessfullyEnded;
import eu.javaexperience.io.IOStream;
import eu.javaexperience.io.IOStreamServer;
import eu.javaexperience.io.IOTools;
import eu.javaexperience.semantic.references.MayNull;
import eu.javaexperience.server.AbstractServer;

public abstract class SocketRpcServer<SOCK extends IOStream, SESS extends RpcSession> extends AbstractServer<SOCK>
{
	protected final RpcProtocolHandler handler;
	
	public SocketRpcServer
	(
		IOStreamServer<SOCK> srv,
		int initialWorkerCount,
		RpcProtocolHandler handler
	)
	{
		super(srv, initialWorkerCount);
		AssertArgument.assertNotNull(this.handler = handler, "rpc protocolhandler");
	}
	
	public SocketRpcServer
	(
		IOStreamServer<SOCK> srv,
		int initialWorkerCount
	)
	{
		this(srv, initialWorkerCount, new RpcDefaultProtocol(new DataObjectJsonImpl()));
	}
	
	@Override
	protected void execute(SOCK sock)
	{
		manageLoad();
		DataCommon prototype = handler.getDefaultCommunicationProtocolPrototype();
		DataSender ds = null;
		DataReceiver rec = null;
		try 
		{
			ds = prototype.newDataSender(sock.getOutputStream());
			rec = prototype.newDataReceiver(sock.getInputStream());
			SESS session = init(sock);
			RpcSessionTools.setCurrentRpcSession((RpcSession)session);
			while(!sock.isClosed())
			{
				try
				{
					DataObject req = rec.receiveDataObject();
					if(null == req)
					{
						break;
					}
					DataObject resp = handleRequest(session, req);
					if(null != resp)
					{
						ds.send(resp);
					}
				}
				catch(OperationSuccessfullyEnded skk)
				{
					continue;
				}
			}
		}
		catch(Throwable e)
		{
			onException(e);
		}
		finally
		{
			RpcSessionTools.setCurrentRpcSession(null);
			IOTools.silentClose(sock);
		}
		manageLoad();
	}
	
	protected void onException(Throwable t)
	{
		if(!(t instanceof IOException))
		{
			t.printStackTrace();
		}
	}
	
	protected abstract SESS init(SOCK socket);
	
	/**
	 * At this level we don't care about calling conventions: theres no magic,
	 * preserved key for method name and not specified argument passing method
	 * applied.
	 * if null returned we don't send any answer to the endpoint, just start
	 * receiving a new call.
	 * 
	 * no catch or any error handling method applied, if exception throwed
	 * (other than {@link OperationSuccessfullyEnded}) the connection will be
	 * terminated.
	 * */
	protected abstract @MayNull DataObject handleRequest(SESS sess, DataObject request);
}