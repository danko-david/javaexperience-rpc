package eu.javaexperience.rpc.function;

import eu.javaexperience.reflect.Caster;
import eu.javaexperience.reflect.NotatedCaster;

public class RpcFunctionParameter
{
	protected final NotatedCaster caster;
	
	public String getTypeName()
	{
		return caster.getTypeShortName();
	}
	
	public String getTypeFullQualifiedName()
	{
		return caster.getTypeFullQualifiedName();
	}
	
	public RpcFunctionParameter(NotatedCaster type)
	{
		this.caster = type;
	}
	
	public Caster getCaster()
	{
		return caster;
	}
}
