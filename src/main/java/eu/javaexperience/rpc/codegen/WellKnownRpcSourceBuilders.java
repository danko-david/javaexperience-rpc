package eu.javaexperience.rpc.codegen;

public enum WellKnownRpcSourceBuilders
{
	javascript(JavascriptRpcSourceGenerator.BASIC_JAVASCRIPT_SOURCE_BUILDER),
	php(PhpRpcInterfaceGenerator.BASIC_PHP_SOURCE_BUILDER),
	java(JavaRpcInterfaceGenerator.BASIC_JAVA_SOURCE_BUILDER),
	ast(AstInterfaceGenerator.BASIC_AST_SOURCE_BUILDER),
	
	;
	
	private final RpcSourceBuilder builder;

	public RpcSourceBuilder getBuilder()
	{
		return builder;
	}
	
	private WellKnownRpcSourceBuilders(RpcSourceBuilder builder)
	{
		this.builder = builder;
	}
}
