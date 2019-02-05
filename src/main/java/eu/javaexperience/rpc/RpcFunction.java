package eu.javaexperience.rpc;

import eu.javaexperience.rpc.function.RpcFunctionParameter;

public interface RpcFunction<C extends RpcRequest, T extends RpcFunctionParameter>
{
	public String getMethodName();
	public T[] getParameterClasses();
	public T getReturningClass();
	public Object call(C ctx, Object thisContext, String functionName, Object... params) throws Throwable;
}
