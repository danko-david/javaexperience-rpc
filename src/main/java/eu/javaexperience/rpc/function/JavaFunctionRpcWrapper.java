package eu.javaexperience.rpc.function;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

import eu.javaexperience.reflect.NotatedCaster;
import eu.javaexperience.rpc.RpcCastTools;
import eu.javaexperience.rpc.RpcFunction;
import eu.javaexperience.rpc.RpcRequest;

//TODO make abstract parent for this and JavaAsIsFunctionWrapper
public class JavaFunctionRpcWrapper<C extends RpcRequest> implements RpcFunction<C, RpcFunctionParameter>
{
	protected Method javaMethod;
	protected String name;
	
	protected RpcFunctionParameter returningType;
	protected RpcFunctionParameter[] params;
	
	public static JavaFunctionRpcWrapper<RpcRequest> wrapJavaFunction(Method m)
	{
		JavaFunctionRpcWrapper ret = new JavaFunctionRpcWrapper<>();
		ret.javaMethod = m;
		ret.javaMethod.setAccessible(true);
		ret.name = m.getName();
		ret.returningType = createByType(m.getGenericReturnType());
		
		Type[] params = m.getGenericParameterTypes();
		ret.params = new RpcFunctionParameter[params.length-1];
		for(int i=1;i<params.length;++i)
		{
			ret.params[i-1] = createByType(params[i]);
		}
		
		return ret;
	}
	
	@Override
	public String getMethodName()
	{
		return name;
	}
	
	@Override
	public RpcFunctionParameter[] getParameterClasses()
	{
		return params;
	}
	
	@Override
	public RpcFunctionParameter getReturningClass()
	{
		return returningType;
	}
	
	public Method getJavaMethod()
	{
		return javaMethod;
	}
	
	protected Object[] assembleJavaFunctionParameters(C ctx, Object thisContext, String functionName, Object... params)
	{
		Object[] cll = new Object[params.length+1];
		cll[0] = ctx;
		for(int i=0;i<params.length;++i)
		{
			cll[i+1] = params[i];
		}
		
		return cll;
	}
	
	@Override
	public Object call(C ctx, Object thisContext, String functionName, Object... params) throws Throwable
	{
		Object[] cll = assembleJavaFunctionParameters(ctx, thisContext, functionName, params);
		
		try
		{
			return javaMethod.invoke(thisContext, cll);
		}
		catch(InvocationTargetException ex)
		{
			if(ex.getCause() instanceof Throwable)
			{
				throw ex.getCause();
			}
			throw ex;
		}
	}
	
	public static RpcFunctionParameter createByType(Type type)
	{
		Class<?> cls = RpcCastTools.extractClass(type);
		
		
		if(void.class == cls || Void.class == cls)
		{
			return new RpcFunctionParameter(new NotatedCaster()
			{
				@Override
				public Object cast(Object in)
				{
					return null;
				}

				@Override
				public String getTypeShortName()
				{
					return "void";
				}

				@Override
				public String getTypeFullQualifiedName()
				{
					return "void";
				}
			});
		}
		
		NotatedCaster c = RpcCastTools.tryCreateCaster(type);
		if(null != c)
		{
			return new RpcFunctionParameter(c);
		}
		
		if(null == cls)
		{
			throw new RuntimeException("No suitable automatic caster for type: "+type);
		}
		
		return new RpcFunctionParameter(RpcCastTools.getDirectCaster(cls));
	}
	
	public static void teszt(RpcRequest req, int[] values)
	{
		System.out.println("Values: "+Arrays.toString((int[])values));
	}
	
	public static void main(String[] args) throws Throwable
	{
		Method m = JavaFunctionRpcWrapper.class.getMethod("teszt", RpcRequest.class, int[].class);
		JavaFunctionRpcWrapper func = wrapJavaFunction(m);
		
		func.call(null, null, "", new int[]{10,20});
		//System.out.println(m.getReturnType());
	}
}
