package eu.javaexperience.rpc;

import static eu.javaexperience.log.LogLevel.TRACE;
import static eu.javaexperience.log.LogLevel.WARNING;
import static eu.javaexperience.log.LoggingTools.tryLogFormat;
import static eu.javaexperience.log.LoggingTools.tryLogSimple;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import eu.javaexperience.asserts.AssertArgument;
import eu.javaexperience.database.annotations.Ignore;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.log.JavaExperienceLoggingFacility;
import eu.javaexperience.log.LogLevel;
import eu.javaexperience.log.Loggable;
import eu.javaexperience.log.Logger;
import eu.javaexperience.rpc.function.JavaFunctionRpcWrapper;
import eu.javaexperience.rpc.function.RpcFunctionParameter;

public class JavaClassRpcCollector<REQ extends RpcRequest> implements RpcFacility<REQ>
{	
	protected static final Logger DEFAULT_LOG = JavaExperienceLoggingFacility.getLogger(new Loggable("RpcFunctionStorage"));
	
	public static JavaClassRpcFunctions[] emptyJavaClassRpcFunctionsArray = new JavaClassRpcFunctions[0];
	
	public static JavaClassRpcCollector[] emptyJavaClassRpcCollector = new JavaClassRpcCollector[0];
	
	public static RpcFacility[] emptyRpcFacilityArray = new RpcFacility[0];
	
	protected Logger LOG = DEFAULT_LOG;
	
	protected Class<?> requestClass;
	
	protected Map<String, JavaFunctionRpcWrapper<REQ>> METHODS;
	
	protected Object javaMethodThisParam;
	
	protected String rpcName;
	
	/*public Class<?> getWrappedClass()
	{
		return requestClass;
	}*/
	
	public JavaClassRpcCollector(Class<?> toWrap)
	{
 		requestClass = toWrap;
	}
	
	protected JavaClassRpcCollector()
	{
 		requestClass = getClass();
	}
	
	protected void initalize()
	{
		Map<String, JavaFunctionRpcWrapper<REQ>> methods = new HashMap<>();
		Method[] ms = requestClass.getMethods();
		for(Method m:ms)
		{
			try
			{
				if(mayRegister(m))
				{	
					JavaFunctionRpcWrapper func = wrapFunction(m);
					methods.put(func.getMethodName(), func);
				}
			}
			catch(Exception e)
			{
				tryLogFormat(LOG, WARNING, "Method: %s", m);
				tryLogSimple(LOG, WARNING, e);
			}
		}
		
		METHODS = Collections.unmodifiableMap(methods);
	}
	
	protected JavaFunctionRpcWrapper wrapFunction(Method m)
	{
		return JavaFunctionRpcWrapper.wrapJavaFunction(m);
	}

	protected void onException(Throwable t)
	{
		t.printStackTrace();
	}
	
	public Collection<JavaFunctionRpcWrapper<REQ>> getWrappedMethods()
	{
		return METHODS.values();
	}
		
	public Collection<JavaFunctionRpcWrapper<REQ>> getWrappedFunctions()
	{
		return METHODS.values();
	}
	
	public void fillFunctionList(Collection<RpcFunction<REQ, RpcFunctionParameter>> fill)
	{
		for(Entry<String, JavaFunctionRpcWrapper<REQ>> kv:METHODS.entrySet())
		{
			fill.add(kv.getValue());
		}
	}
	
	public Map<String, JavaFunctionRpcWrapper<REQ>> getWrappedMethodsWithName()
	{
		return METHODS;
	}
	
	public List<RpcFunction<REQ, RpcFunctionParameter>> getFunctionList()
	{
		ArrayList<RpcFunction<REQ, RpcFunctionParameter>> ret = new ArrayList<>();
		fillFunctionList(ret);
		return ret;
	}
	
	
	public void setLogger(Logger logger)
	{
		AssertArgument.assertNotNull(logger, "logger");
		this.LOG = logger;
	}
	
	public static boolean mayRegisterMethod(Method m, Class<?> rpcThisClass)
	{
		int mod = m.getModifiers();
		if(Modifier.isPublic(mod) && Modifier.isStatic(mod))
		{
			if(null == m.getAnnotation(Ignore.class))
			{
				Class[] clss = m.getParameterTypes();
				if(clss.length > 0)
				{
					if(rpcThisClass.isAssignableFrom(clss[0]))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
	
	/**
	 * You may reject the function invocation by throwing an exception
	 * that will be shown (relayed) on the caller end.
	 * */
	protected void beforeCall(REQ ctx, JavaFunctionRpcWrapper<REQ> m) throws Throwable
	{
	}
	
	protected boolean mayRegister(Method m)
	{
		return mayRegisterMethod(m, RpcRequest.class);
	}
	
	protected DataObject onMethodNotFound(REQ ctx, String name)
	{
		throw new RuntimeException("Method not found: "+name);
	}
	
	protected static final AtomicInteger REQUEST_ID = new AtomicInteger(0);
	
	public DataObject dispatch(REQ ctx)
	{
		RpcProtocolHandler protocol = ctx.getProtocolHandler();
		
		int req_id = -1;
		if(LOG.mayLog(LogLevel.TRACE))
		{
			req_id = REQUEST_ID.incrementAndGet();
			LOG.logFormat(TRACE, "%d Request: %s", req_id, ctx.getRequestData().getImpl().toString());
		}
		
		try
		{
			String name = ctx.getProtocolHandler().getRequestFunctionName(ctx);
			JavaFunctionRpcWrapper<REQ> func = METHODS.get(name);
			
			if(null != func)
			{
				beforeCall(ctx, func);
				Object ret = RpcTools.callFunction(ctx, javaMethodThisParam, func);
				if(ret instanceof DataObject)
				{
					return (DataObject) ret;
				}
				
				return RpcTools.wrapReturningValue(ctx, ret);
			}
			
			return onMethodNotFound(ctx, name);
		}
		catch(Throwable t)
		{
			onException(t);
			DataObject ret = protocol.createException(ctx, t);
			
			if(-1 != req_id && LOG.mayLog(LogLevel.TRACE))
			{
				LOG.logFormat(TRACE, "%d Exception response: %s", req_id, ret.getImpl().toString());
			}
			
			return ret;
		}
	}

	@Override
	public DataObject getBy(REQ a)
	{
		return dispatch(a);
	}

	public String getRpcName()
	{
		if(null != rpcName)
		{
			return rpcName;
		}
		return requestClass.getSimpleName();
	}
	
	public Class getWrappedClass()
	{
		return requestClass;
	}
}
