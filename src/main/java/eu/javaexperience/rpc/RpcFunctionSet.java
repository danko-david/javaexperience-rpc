package eu.javaexperience.rpc;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import eu.javaexperience.collection.map.RWLockMap;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.rpc.function.RpcFunctionParameter;
import eu.javaexperience.semantic.references.MayNotNull;
import eu.javaexperience.semantic.references.MayNull;

public class RpcFunctionSet<C extends RpcRequest, P extends RpcFunctionParameter, F extends RpcFunction<C, RpcFunctionParameter>>
{
	protected RWLockMap<String, F> functions = new RWLockMap<>(new HashMap<String, F>());

	protected @MayNull F findFunction(C ctx, String func)
	{
		return functions.get(func);
	}

	protected @MayNotNull Object unservedRequest(C ctx, String functionName, DataObject request)
	{
		throw new RuntimeException("Method "+functionName+" not found.");
	}
	
	public void addFunction(F function)
	{
		this.functions.put(function.getMethodName(), function);
	}
	
	public Set<Entry<String,F>> getMethodSet()
	{
		return functions.entrySet();
	}
}
