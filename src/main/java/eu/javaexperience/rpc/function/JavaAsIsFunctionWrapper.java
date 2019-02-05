package eu.javaexperience.rpc.function;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import eu.javaexperience.rpc.RpcRequest;

public class JavaAsIsFunctionWrapper<C extends RpcRequest> extends JavaFunctionRpcWrapper<C>
{
	public static JavaAsIsFunctionWrapper<RpcRequest> wrapJavaFunctionAsIs(Method m)
	{
		JavaAsIsFunctionWrapper ret = new JavaAsIsFunctionWrapper();
		ret.javaMethod = m;
		ret.name = m.getName();
		ret.returningType = createByType(m.getGenericReturnType());
		
		Type[] params = m.getGenericParameterTypes();
		ret.params = new RpcFunctionParameter[params.length];
		for(int i=0;i<params.length;++i)
		{
			ret.params[i] = createByType(params[i]);
		}
		
		return ret;
	}
	
	protected Object[] assembleJavaFunctionParameters(C ctx, Object thisContext, String functionName, Object... params)
	{
		return params;
	}
}
