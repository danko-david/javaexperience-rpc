package eu.javaexperience.rpc;

import static eu.javaexperience.log.LogLevel.WARNING;
import static eu.javaexperience.log.LoggingTools.tryLogFormat;
import static eu.javaexperience.log.LoggingTools.tryLogSimple;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import eu.javaexperience.arrays.ArrayTools;
import eu.javaexperience.database.annotations.Ignore;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.rpc.function.JavaAsIsFunctionWrapper;
import eu.javaexperience.rpc.function.JavaFunctionRpcWrapper;

public class JavaClassRpcUnboundFunctionsInstance<REQ extends RpcRequest> extends JavaClassRpcCollector<REQ>
{
	public JavaClassRpcUnboundFunctionsInstance(Class... otherInterfaces)
	{
		super();
		javaMethodThisParam = this;
		initalize(null, otherInterfaces);
	}
	
	public JavaClassRpcUnboundFunctionsInstance(Object rpcServiceInstance, Class... otherInterfaces)
	{
		this(null, rpcServiceInstance, otherInterfaces);
	}
	
	public JavaClassRpcUnboundFunctionsInstance(String rpcName, Object rpcServiceInstance, Class... otherInterfaces)
	{
		super();
		this.rpcName = rpcName;
		javaMethodThisParam = rpcServiceInstance;
		initalize(null, otherInterfaces);
	}
	
	protected void initalize(Class root, Class[] otherInterfaces)
	{
		Map<String, JavaFunctionRpcWrapper<REQ>> methods = new HashMap<>();
		if(null != root)
		{
			otherInterfaces = ArrayTools.arrayAppend(root, otherInterfaces);
		}
		
		for(Class cls: otherInterfaces)
		{
			Method[] ms = cls.getMethods();
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
		}
		
		METHODS = Collections.unmodifiableMap(methods);
	}
	
	@Override
	public boolean mayRegister(Method m)
	{
		int mod = m.getModifiers();
		if(Modifier.isPublic(mod))
		{
			if(null == m.getAnnotation(Ignore.class) && null == m.getAnnotation(eu.javaexperience.generic.annotations.Ignore.class))
			{
				for(Method om:Mirror.getClassData(Object.class).getAllMethods())
				{
					if(om.equals(m))
					{
						return false;
					}
				}
				
				return true;
			}
		}
		return false;
	}
	
	protected JavaFunctionRpcWrapper wrapFunction(Method m)
	{
		return JavaAsIsFunctionWrapper.wrapJavaFunctionAsIs(m);
	}
}
