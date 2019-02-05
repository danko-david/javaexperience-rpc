package eu.javaexperience.rpc.codegen;

import java.util.Collection;
import java.util.Map;

import eu.javaexperience.rpc.RpcFunction;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.function.RpcFunctionParameter;

public interface RpcSourceBuilder<P extends RpcFunctionParameter, F extends RpcFunction<? extends RpcRequest, P>>
{
	public String buildRpcClientSource(String compilationUnitName, Collection<F> functions, Map<String, Object> options);
}
