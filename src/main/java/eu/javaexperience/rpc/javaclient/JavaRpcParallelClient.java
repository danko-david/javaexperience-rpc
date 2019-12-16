package eu.javaexperience.rpc.javaclient;

import java.io.Closeable;
import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import eu.javaexperience.asserts.AssertArgument;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReceiver;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.datareprez.DataSender;
import eu.javaexperience.interfaces.simple.SimpleGet;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.multithread.notify.WaitForSingleEvent;
import eu.javaexperience.patterns.behavioral.mediator.EventMediator;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.rpc.RpcProtocolHandler;
import eu.javaexperience.semantic.references.MayNull;

public class JavaRpcParallelClient
{
	protected EventMediator<DataObject> serverEvents = new EventMediator<>();
	protected RpcProtocolHandler proto;

	protected AtomicLong trasactionId = new AtomicLong();
	
	protected LinkedList<JavaClientRpcPendingRequest> pendingRequests = new LinkedList<>();
	
	SimplePublish1<DataObject> send;
	SimpleGet<DataObject> receive;
	
	public JavaRpcParallelClient(DataSender send, DataReceiver rec, RpcProtocolHandler proto)
	{
		this.send = (o)->
		{
			try
			{
				send.send(o);
			}
			catch (IOException e)
			{
				Mirror.propagateAnyway(e);
			}
		};
		this.receive = ()->
		{
			try
			{
				return rec.receiveDataObject();
			}
			catch (IOException e)
			{
				Mirror.propagateAnyway(e);
				return null;
			}
		};
		this.proto = proto;
	}
	
	public JavaRpcParallelClient(SimplePublish1<DataObject> send, SimpleGet<DataObject> rec, RpcProtocolHandler proto)
	{
		this.send = send;
		this.receive = rec;
		this.proto = proto;
	}

	
	protected class JavaClientRpcPendingRequest implements Closeable
	{
		protected Object originalTid;
		protected final long tid;
		protected DataObject response = null;
		protected boolean revoked = false;
		
		public JavaClientRpcPendingRequest(DataObject req)
		{
			this.tid = trasactionId.incrementAndGet();
			originalTid = req.opt("t");
			req.putLong("t", tid);
		}
		
		@Override
		public void close() throws IOException
		{
			revoked = true;
			revokePendingRequest(this);
		}
		
		@Override
		protected void finalize() throws Throwable
		{
			//safety net
			close();
		}
		
		protected WaitForSingleEvent wait = new WaitForSingleEvent();
		
		public boolean isResponsed()
		{
			return null != response;
		}
		
		public boolean isRevoked()
		{
			return null != response && revoked;
		}
		
		public boolean waitResponse(long timeout, TimeUnit unit) throws InterruptedException
		{
			AssertArgument.assertTrue(!isRevoked(), "Request has been revoked");
			wait.waitForEvent(timeout, unit);
			AssertArgument.assertTrue(!isRevoked(), "Request has been revoked");
			synchronized(this)
			{
				return null != response;
			}
		}
		
		public DataObject ensureResponse(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException
		{
			return ensureResponse(timeout, unit, null);
		}
		
		public DataObject ensureResponse(long timeout, TimeUnit unit, String errAppendMsg) throws InterruptedException, TimeoutException
		{
			if(!waitResponse(timeout, unit))
			{
				throw new TimeoutException("Request (`"+tid+"`) not responded within "+timeout+" "+unit+" for the request. "+errAppendMsg);
			}
			
			return response;
		}
		
		protected void receiveResponse(DataObject data)
		{
			AssertArgument.assertNotNull(data, "data");
			if(null != originalTid)
			{
				DataReprezTools.put(data, "t", originalTid);
			}
			synchronized(this)
			{
				response = data;
			}
			wait.evenOcurred();
		}
		
		public boolean tryAcceptResponse(DataObject a)
		{
			//check response by address
			if(tid == a.optLong("t", -1))
			{
				//we got the response.
				try
				{
					receiveResponse(a);
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
				
				return true;
			}
			return false;
		}

		public void revoke()
		{
			revoked = true;
			wait.evenOcurred();
		}

		/**
		 * Similar to the root's readResponse, but this checks request responded
		 * before reading a new packet. This is mandantory, because other thread
		 * also tries to take the receive's lock. The bad scenario when we
		 * waiting for the lock to acquire, other thread receives this answer,
		 * but after, this thread acquires the locks, ang get stucked because of
		 * blocking.
		 * 
		 * Unstrucking happens when this gets other's response. This thread go
		 * trought the receive.get() call and go back to check this request
		 * responded.
		 * */
		public void tryReadResponse()
		{
			DataObject rec = null;
			synchronized(receive)
			{
				if(!isResponsed())
				{
					rec = receive.get();
				}
			}
			AssertArgument.assertNotNull(rec, "Received packet");
			publishResponse(rec);
		}
	}
	
	public EventMediator<DataObject> getServerEventMediator()
	{
		return serverEvents;
	}
	
	protected void sendPacket(DataObject req)
	{
		synchronized (send)
		{
			send.publish(req);	
		}
	}
	
	protected GetBy1<DataObject, DataObject> transactionHandler = (req)->
	{
		JavaClientRpcPendingRequest p = new JavaClientRpcPendingRequest(req);
		synchronized(pendingRequests)
		{
			pendingRequests.addFirst(p);
		}
		
		//actually send the package
		sendPacket(req);
		
		while(!p.isResponsed() && !p.isRevoked())
		{
			p.tryReadResponse();
		}
		
		if(p.isRevoked())
		{
			throw new RuntimeException("Request has been revoked.");
		}
		
		if(!p.isResponsed())
		{
			throw new RuntimeException("Response error: null response returned.");
		}
		
		return p.response;
	};
	
	public void publishResponse(DataObject resp)
	{
		synchronized(pendingRequests)
		{
			for(JavaClientRpcPendingRequest p:pendingRequests)
			{
				if(p.tryAcceptResponse(resp))
				{
					pendingRequests.remove(p);
					return;
				}
			}
		}
		
		synchronized(serverEvents)
		{
			serverEvents.dispatchEvent(resp);
		}
	}
	
	
	/**
	 * You can call this function to receiving server events without calling any
	 * client functions.
	 * */
	public void readResponse() throws IOException
	{
		DataObject rec = null;
		synchronized(receive)
		{
			rec = receive.get();
		}
		AssertArgument.assertNotNull(rec, "Received packet");
		publishResponse(rec);
	}
	
	protected boolean revokePendingRequest(JavaClientRpcPendingRequest req)
	{
		synchronized(pendingRequests)
		{
			boolean ret = pendingRequests.remove(req);
			if(ret)
			{
				req.revoke();
			}
			return ret;
		}
	}
	
	public <T> T createApiObject(Class<T> cls, @MayNull String namespace)
	{
		return JavaRpcClientTools.createApiWithTransactionHandler(cls, transactionHandler, namespace, proto);
	}
	
	/**
	 * Revokes all pending request without closing the communication lines.
	 * */
	public void shudown()
	{
		synchronized(pendingRequests)
		{
			for(JavaClientRpcPendingRequest p:pendingRequests)
			{
				p.revoke();
			}
		}
	}
}
