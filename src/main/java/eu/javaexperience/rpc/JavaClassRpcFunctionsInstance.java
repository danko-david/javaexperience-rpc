package eu.javaexperience.rpc;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import eu.javaexperience.database.annotations.Ignore;

public class JavaClassRpcFunctionsInstance<REQ extends RpcRequest> extends JavaClassRpcCollector<REQ>
{
	public JavaClassRpcFunctionsInstance(Object rpcServiceInstance)
	{
		super(rpcServiceInstance.getClass());
		javaMethodThisParam = rpcServiceInstance;
		initalize();
	}
	
	@Override
	public boolean mayRegister(Method m)
	{
		int mod = m.getModifiers();
		if(Modifier.isPublic(mod))
		{
			if(null == m.getAnnotation(Ignore.class))
			{
				Class[] clss = m.getParameterTypes();
				if(clss.length > 0)
				{
					if(RpcRequest.class.isAssignableFrom(clss[0]))
					{
						return true;
					}
				}
			}
		}
		return false;
	}
}
