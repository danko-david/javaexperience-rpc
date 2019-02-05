package eu.javaexperience.rpc.primitives;

//compatible with enum by it's signature.
public interface SymbolEnumSource
{
	public String name();
	public int ordinal();
	public String getSymbolDescription();
}