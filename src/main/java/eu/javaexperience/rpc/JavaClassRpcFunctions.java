package eu.javaexperience.rpc;

public class JavaClassRpcFunctions<REQ extends RpcRequest> extends JavaClassRpcCollector<REQ>
{
	public JavaClassRpcFunctions(Class<?> toWrap)
	{
		super(toWrap);
		initalize();
	}
}