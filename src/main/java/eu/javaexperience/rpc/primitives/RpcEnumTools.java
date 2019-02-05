package eu.javaexperience.rpc.primitives;

import java.util.Collection;

import eu.javaexperience.collection.CollectionTools;
import eu.javaexperience.collection.enumerations.EnumTools;
import eu.javaexperience.datareprez.DataArray;
import eu.javaexperience.functional.BoolFunctions;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.reflect.CastTo;
import eu.javaexperience.transform.ConverterPublisher;

public class RpcEnumTools
{
	protected static final GetBy1<Boolean, ExportableEnum> EXPORTABLE_ENUM_FILTER = new GetBy1<Boolean, ExportableEnum>()
	{
		@Override
		public Boolean getBy(ExportableEnum a)
		{
			return a.isWordwideVisible();
		}
	};
	
	public static SymbolDescriptor[] exportPublicEnums(Class enumClass)
	{
		return exportFilteredEnums(enumClass, EXPORTABLE_ENUM_FILTER);
	}
	
	public static SymbolDescriptor[] exportFilteredEnums(Class enumClass, GetBy1<Boolean, ExportableEnum>... filter_false_drop)
	{
		ConverterPublisher conv =
		ConverterPublisher.createFromGetByWithPreFilter
		(
			SymbolDescriptor.CONVERTER,
			0 == filter_false_drop.length?
				BoolFunctions.always()
			:
			(GetBy1) BoolFunctions.and(filter_false_drop)
		);
		
		CollectionTools.copyInto(enumClass.getEnumConstants(), conv);
		
		return (SymbolDescriptor[]) conv.getDestination().toArray(SymbolDescriptor.emptySymbolDescriptorArray);
	}
	
	public static <E extends Enum> void copyNamesToArray(Collection<E> enumVals, DataArray arr)
	{
		for(E e:enumVals)
		{
			arr.putString(e.name());
		}
	}

}
