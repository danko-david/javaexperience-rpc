package eu.javaexperience.rpc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import eu.javaexperience.arrays.ArrayTools;
import eu.javaexperience.asserts.AssertArgument;
import eu.javaexperience.collection.CollectionTools;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.exceptions.OperationSuccessfullyEnded;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.getBy.GetBy2;
import eu.javaexperience.interfaces.simple.publish.SimplePublish1;
import eu.javaexperience.io.IOStream;
import eu.javaexperience.io.IOStreamServer;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;
import eu.javaexperience.rpc.bidirectional.RpcClientProtocolHandler;
import eu.javaexperience.rpc.bulk.MultiplexedApiCall;
import eu.javaexperience.rpc.codegen.PhpRpcInterfaceGenerator;
import eu.javaexperience.rpc.codegen.RpcSourceBuilder;
import eu.javaexperience.rpc.discover.DiscoverRpc;
import eu.javaexperience.rpc.function.RpcFunctionParameter;
import eu.javaexperience.semantic.references.MayNull;

public class RpcTools
{
	//TODO replace with discover API
	
	public static final RpcFacility[] emptyRpcFacilityArray = new RpcFacility[0];
	
	public static <C extends RpcRequest, F extends RpcFunction<C, ?>>
		DataObject 
	callFunction
	(
		C ctx,
		F function
	)
	{
		return callFunction(ctx, null, function);
	}
	
	
	public static <C extends RpcRequest, F extends RpcFunction<C, ?>>
		DataObject 
	callFunction
	(
		C ctx,
		Object javaMethodThisParam,
		F function
	)
	{
		RpcProtocolHandler protocol = ctx.getProtocolHandler();
		
		String functionName = protocol.getRequestFunctionName(ctx);
		try
		{
			//TODO conflict
			Object thisContext = javaMethodThisParam;//protocol.extractThisContext(ctx);
			Object[] incoming = protocol.extractParameters(ctx);
			
			RpcFunctionParameter[] ps = function.getParameterClasses();
			
			Object[] callParam = new Object[Math.min(incoming.length, ps.length)];
			
			for(int i=0;i<callParam.length;++i)
			{
				callParam[i] = ps[i].getCaster().cast(incoming[i]);
			}
			
			Object result = function.call(ctx, thisContext, functionName, callParam);
			return protocol.createReturningValue(ctx, result);
		}
		catch(Throwable e)
		{
			e.printStackTrace();
			return protocol.createException(ctx, e);
		}
	}
	
	public static DataObject wrapReturningValue
	(
		RpcRequest ctx,
		Object ret
	)
	{
		RpcProtocolHandler protocol = ctx.getProtocolHandler();
		return protocol.createReturningValue(ctx, ret);
	}

	public static DataObject wrapReturningValue
	(
		RpcProtocolHandler protocol,
		Object ret
	)
	{
		return protocol.createReturningValue(null, ret);
	}
	
	
	public static DataObject wrapException
	(
		RpcRequest ctx,
		Throwable ret
	)
	{
		RpcProtocolHandler protocol = ctx.getProtocolHandler();
		return protocol.createException(ctx, ret);
	}
	
	public static <S extends RpcSession> RpcRequest createClientInvocation
	(
		S session,
		long trace,
		Object _this,
		String method,
		Object... args
	)
	{
		BidirectionalRpcDefaultProtocol<S> PROTO = (BidirectionalRpcDefaultProtocol<S>) session.getDefaultRpcProtocolHandler();
		RpcRequest req = PROTO.createClientRequest(session);
		fillClientInvocation(req, trace, null, _this, method, args);
		return req;
	}
	

	
	public static <S extends RpcSession> RpcRequest createClientNamespaceInvocation
	(
		S session,
		long trace,
		@MayNull String namespace,
		Object _this,
		String method,
		Object... args
	)
	{
		BidirectionalRpcDefaultProtocol<S> PROTO = (BidirectionalRpcDefaultProtocol<S>) session.getDefaultRpcProtocolHandler();
		RpcRequest req = PROTO.createClientRequest(session);
		fillClientInvocation(req, trace, namespace, _this, method, args);
		return req;
	}
	
	public static <S extends RpcSession> void fillClientInvocation
	(
		RpcRequest req,
		long trace,
		String namspace,
		Object _this,
		String method,
		Object[] args
	)
	{
		RpcClientProtocolHandler PROTO = (RpcClientProtocolHandler) req.getProtocolHandler();
		PROTO.putNamespace(req, namspace);
		PROTO.putPacketTraceId(req, String.valueOf(trace));
		PROTO.putThisParameter(req, _this);
		PROTO.putRequestFunctionName(req, method);
		PROTO.putParameters(req, args);
	}
	
	
	
	public static <SOCK extends IOStream, SESS extends RpcSession> SocketRpcServer<SOCK, SESS>
	newServer
	(
		IOStreamServer<SOCK> serverSocket,
		int concurrency,
		RpcProtocolHandler proto,
		final GetBy2<SESS, SOCK, RpcProtocolHandler> sessionCreator,
		final GetBy2<DataObject, SESS, DataObject> requestHandler
	)
	{
		AssertArgument.assertNotNull(sessionCreator, "session_creator");
		AssertArgument.assertNotNull(requestHandler, "request_handler");
		return new SocketRpcServer<SOCK, SESS>(serverSocket, concurrency, proto)
		{
			@Override
			protected SESS init(SOCK socket)
			{
				return sessionCreator.getBy(socket, handler);
			}

			@Override
			protected @MayNull DataObject handleRequest(SESS sess, DataObject request)
			{
				return requestHandler.getBy(sess, request);
			}
		};
	}
	
	public static <SOCK extends IOStream, SESS extends RpcSession> SocketRpcServer<SOCK, SESS> newParallelCallServer
	(
		IOStreamServer<SOCK> serverSocket,
		int concurrency,
		RpcProtocolHandler proto,
		final GetBy2<SESS, SOCK, RpcProtocolHandler> sessionCreator,
		final GetBy2<DataObject, SESS, DataObject> requestHandler
	)
	{
		ThreadPoolExecutor exec =
			new ThreadPoolExecutor(10, 300, 60, TimeUnit.SECONDS, new LinkedBlockingDeque<>());

		return new SocketRpcServer<SOCK, SESS>
		(
			serverSocket,
			concurrency,
			proto
		)
		{
			@Override
			protected SESS init(SOCK socket)
			{
				return sessionCreator.getBy(socket, handler);
			}
			
			@Override
			protected void responseRequest(SimplePublish1<DataObject> response, SESS sess, DataObject request, Object extraCtx)
			{
				exec.execute
				(
					()->
					{
						RpcSessionTools.setCurrentRpcSession(sess);
						try
						{
							DataObject resp = handleRequest(sess, request);
							if(null != resp)
							{
								response.publish(resp);
							}
							else
								
							System.out.println("NULL response");
						}
						catch(Throwable t)
						{
							t.printStackTrace();
						}
						finally
						{
							RpcSessionTools.setCurrentRpcSession(null);
						}
					}
				)
				;
			}
			
			@Override
			protected @MayNull DataObject handleRequest(SESS sess, DataObject request)
			{
				return requestHandler.getBy(sess, request);
			}
			
			@Override
			public void close() throws IOException
			{
				super.close();
				exec.shutdown();
			}
		};
	}
	
	public static abstract class RpcEndpointUnitRpcHandler<SESS extends RpcSession, REQ extends RpcRequest>
	{
		protected RpcProtocolHandler protocolHandler;
		
		public abstract DataObject receive();
		public abstract void send(DataObject response);
		public abstract DataObject dispatch(REQ req);
		
		public RpcEndpointUnitRpcHandler()
		{
			protocolHandler = new BidirectionalRpcDefaultProtocol<>(new DataObjectJsonImpl());
		}
		
		public SESS createSession()
		{
			return (SESS) new SimpleRpcSession(protocolHandler);
		}
		
		public void destroySession(SESS session)
		{
			
		}
		
		public REQ createRequest(SESS sess, DataObject request)
		{
			return (REQ) new SimpleRpcRequest(sess, request);
		}
		
		public void execute()
		{
			SESS session = createSession();
			RpcSessionTools.setCurrentRpcSession((RpcSession)session);
			try
			{
				while(true)
				{
					try
					{
						DataObject req = receive();
						if(null == req)
						{
							break;
						}
						DataObject ret = dispatch(createRequest(session, req));
						
						if(null != ret)
						{
							send(ret);
						}
					}
					catch(OperationSuccessfullyEnded skk)
					{
						continue;
					}
				}
			}
			finally
			{
				RpcSessionTools.setCurrentRpcSession(null);
			}
		}
	}
	
	public static <P extends RpcFunctionParameter, F extends RpcFunction<? extends RpcRequest, P>> String generatePhpRpcClass
	(
		String unitName,
		Collection<F> functions
	)
	{
		return generateRpcClassWithBuilder
		(
			PhpRpcInterfaceGenerator.BASIC_PHP_SOURCE_BUILDER,
			unitName,
			functions
		);
	}
	
	public static <P extends RpcFunctionParameter, F extends RpcFunction<? extends RpcRequest, P>> String generateRpcClassWithBuilder
	(
		RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>> builder,
		String unitName,
		Collection<F> functions
	)
	{
		return generateRpcClassWithBuilder(builder, unitName, functions, new HashMap<String, Object>());
	}
	
	public static <P extends RpcFunctionParameter, F extends RpcFunction<? extends RpcRequest, P>> String generateRpcClassWithBuilder
	(
		RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>> builder,
		String unitName,
		Collection<F> functions,
		Map<String, Object> settings
	)
	{
		return builder.buildRpcClientSource
		(
			unitName,
			(Collection<RpcFunction<RpcRequest, RpcFunctionParameter>>) functions,
			settings
		);
	}
	
	public static String generateRpcClassesWithBuilder
	(
		RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>> builder,
		Map<String, Object> settings,
		Entry<String, JavaClassRpcFunctions<SimpleRpcRequest>>... rpcs
	)
	{
		StringBuilder sb = new StringBuilder();
		
		for(Entry<String, JavaClassRpcFunctions<SimpleRpcRequest>> rpc:rpcs)
		{
			sb.append(generateRpcClassWithBuilder(builder, rpc.getKey(), rpc.getValue().getFunctionList(), settings));
		}
		
		return sb.toString();
	}

	public static GetBy2<SimpleRpcSession, IOStream, RpcProtocolHandler> getSimpleSessionCreator()
	{
		return new GetBy2<SimpleRpcSession, IOStream, RpcProtocolHandler>()
		{
			@Override
			public SimpleRpcSession getBy(IOStream a, RpcProtocolHandler b)
			{
				return new SimpleRpcSession(b);
			}
		};
	}

	public static Object extractReturnOrThrow(RpcRequest req) throws Throwable
	{
		RpcClientProtocolHandler<?> hand = (RpcClientProtocolHandler<?>) req.getProtocolHandler();
		Throwable t = hand.extractException(req);
		if(null != t)
		{
			throw t;
		}
		
		return hand.extractReturningValue(req);
	}

	public static <R extends RpcRequest> GetBy1<DataObject, R> createSimpleNamespaceDispatcher
	(
		final GetBy2<DataObject, R, String> _default,
		final Collection<RpcFacility> rpcs
	)
	{
		return createSimpleNamespaceDispatcher(_default, (RpcFacility[]) rpcs.toArray(JavaClassRpcFunctions.emptyRpcFacilityArray));
	}
	
	public static <R extends RpcRequest> GetBy1<DataObject, R> createSimpleNamespaceDispatcher
	(
		final GetBy2<DataObject, R, String> _default,
		final RpcFacility... rpcs
	)
	{
		return new GetBy1<DataObject, R>()
		{
			@Override
			public DataObject getBy(R a)
			{
				String ns = a.getProtocolHandler().extractNamespace(a);
				for(RpcFacility<R> rpc:rpcs)
				{
					if(Mirror.equals(ns, rpc.getRpcName()))
					{
						return rpc.dispatch(a);
					}
				}
				
				if(null != _default)
				{
					return _default.getBy(a, ns);
				}
				
				return null;
			}
		};
	}


	public static List<RpcFacility> wrapApis(RpcFacility... apis)
	{
		ArrayList<RpcFacility> dst = new ArrayList<>();
		CollectionTools.copyInto(apis, dst);
		return Collections.unmodifiableList(dst);
	}


	public static GetBy1<DataObject, SimpleRpcRequest> createSimpleNamespaceDispatcherWithDiscoverApi(RpcFacility... instances)
	{
		DiscoverRpc discover = new DiscoverRpc(instances);
		RpcFacility[] add = ArrayTools.arrayAppend(discover, instances);
		return createSimpleNamespaceDispatcher(discover, add);
	}


	public static List<RpcFacility> addMultiplexer(List<RpcFacility> apis, GetBy1<RpcRequest, DataObject> requestCreator)
	{
		List<RpcFacility> rpc = new ArrayList<>();
		CollectionTools.copyInto(apis, rpc);
		rpc.add(new JavaClassRpcUnboundFunctionsInstance<RpcRequest>(new MultiplexedApiCall(apis, requestCreator), MultiplexedApiCall.class)
		{
			@Override
			public String getRpcName()
			{
				return "MultiplexedApiCall";
			}
		});
		return rpc;
	}


	public static void throwUnknownNamespace(String ns)
	{
		throw new RuntimeException("Unknown RPC namespace: "+ns);
	}
}
