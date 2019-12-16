package eu.javaexperience.rpc;

import java.io.IOException;

import eu.javaexperience.asserts.AssertArgument;
import eu.javaexperience.datareprez.DataCommon;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReceiver;
import eu.javaexperience.datareprez.DataSender;
import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.exceptions.OperationSuccessfullyEnded;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.io.IOStream;
import eu.javaexperience.io.IOStreamServer;
import eu.javaexperience.io.IOTools;
import eu.javaexperience.reflect.Mirror;
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
	
	protected Object createExtraContext(SOCK sock, SESS session)
	{
		return null;
	}
	
	protected void destoryExtraContext(Object o)
	{
	}
	
	@Override
	protected void execute(SOCK sock)
	{
		manageLoad();
		DataCommon prototype = handler.getDefaultCommunicationProtocolPrototype();
		DataReceiver rec = null;
		SESS session = null;
		Object ctx = null;
		try 
		{
			DataSender ds = prototype.newDataSender(sock.getOutputStream());
			SimplePublish1<DataObject> response = resp->
			{
				synchronized (ds)
				{
					try
					{
						ds.send(resp);
					}
					catch (IOException e)
					{
						Mirror.propagateAnyway(e);
					}
				}
			};
			rec = prototype.newDataReceiver(sock.getInputStream());
			session = init(sock);
			RpcSessionTools.setCurrentRpcSession((RpcSession)session);
			ctx = createExtraContext(sock, session);
			
			while(!sock.isClosed())
			{
				try
				{
					DataObject req = rec.receiveDataObject();
					if(null == req)
					{
						break;
					}
					responseRequest(response, session, req, ctx);
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
			try
			{
				if(null != session)
				{
					session.destroy();
				}
			}
			catch(Exception e)
			{}
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
	
	protected void responseRequest(SimplePublish1<DataObject> response, SESS sess, DataObject request, Object extraCtx)
	{
		DataObject resp = handleRequest(sess, request);
		if(null != resp)
		{
			response.publish(resp);
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