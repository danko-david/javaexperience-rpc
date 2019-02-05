package eu.javaexperience.rpc.primitives;

import eu.javaexperience.interfaces.simple.getBy.GetBy1;

public class SymbolDescriptor
{
	public int id;
	public String symbol;
	public String description;

	public static SymbolDescriptor[] emptySymbolDescriptorArray = new SymbolDescriptor[0];
	
	public static final GetBy1<SymbolDescriptor, SymbolEnumSource> CONVERTER = new GetBy1<SymbolDescriptor, SymbolEnumSource>()
	{
		@Override
		public SymbolDescriptor getBy(SymbolEnumSource a)
		{
			SymbolDescriptor sd = new SymbolDescriptor();
			sd.id = a.ordinal();
			sd.symbol = a.name();
			sd.description = a.getSymbolDescription();
			return sd;
		}
	};
}
