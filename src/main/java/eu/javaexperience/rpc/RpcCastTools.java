package eu.javaexperience.rpc;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import eu.javaexperience.arrays.ArrayTools;
import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.datareprez.convertFrom.ArrayLike;
import eu.javaexperience.reflect.CastTo;
import eu.javaexperience.reflect.FieldSelectTools;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.NotatedCaster;
import eu.javaexperience.text.Format;

public class RpcCastTools
{
	public static NotatedCaster arrayCaster(final Class<?> cls)
	{
		if(cls.isArray())
		{
			final Class<?> component = cls.getComponentType();
			final NotatedCaster c = tryCreateCaster(component);
			if(null != c)
			{
				return new NotatedCaster()
				{
					@Override
					public Object cast(Object in)
					{
						if(null == in)
						{
							return null;
						}
						
						if(cls.isAssignableFrom(in.getClass()))
						{
							return in;
						}
						
						
						//need convert
						int len = tryGetLength(in);
						
						if(-1 == len)
						{
							return null;
						}
						
						Object ret = Array.newInstance(component, len);
						for(int i=0;i<len;++i)
						{
							Array.set(ret, i, c.cast(tryGetIndex(in, i)));
						}
						
						return ret;
					}

					@Override
					public String getTypeShortName()
					{
						return c.getTypeShortName()+"[]";
					}

					@Override
					public String getTypeFullQualifiedName()
					{
						return c.getTypeFullQualifiedName()+"[]";
					}
				};
			}
		}
		
		return null;
	}

	protected static ConcurrentMap<Class, NotatedCaster> WELL_KNOWN_CASTERS = new ConcurrentHashMap<>();
	
	static
	{
		WELL_KNOWN_CASTERS.put(Map.class, new NotatedCaster()
		{
			@Override
			public Object cast(Object in)
			{
				if(in instanceof Map)
				{
					return in;
				}
				
				if(in instanceof DataObject)
				{
					return DataReprezTools.extractToJavaPrimitiveTypes(in);
				}
				
				SmallMap ret = new SmallMap<>();
				try
				{
					Mirror.extractFieldsToMap(in, ret, FieldSelectTools.SELECT_ALL_INSTANCE_FIELD);
				}
				catch(Exception e){}
				
				return ret;
			}
			
			@Override
			public String getTypeShortName()
			{
				return "Map";
			}
			
			@Override
			public String getTypeFullQualifiedName()
			{
				return "java.util.Map";
			}
		});
		
		WELL_KNOWN_CASTERS.put(byte[].class, new NotatedCaster()
		{
			@Override
			public Object cast(Object in)
			{
				if(in instanceof byte[])
				{
					return in;
				}
				
				if(in instanceof String)
				{
					return Format.base64Decode((String) in);
				}
				
				return null;
			}
			
			@Override
			public String getTypeShortName()
			{
				return "byte[]";
			}
			
			@Override
			public String getTypeFullQualifiedName()
			{
				return "byte[]";
			}
		});
	}
	
	protected static int tryGetLength(Object o)
	{
		if(null != o)
		{
			if(o.getClass().isArray())
			{
				return Array.getLength(o);
			}
			else if(o instanceof ArrayLike)
			{
				return ((ArrayLike)o).size();
			}
			else if(o instanceof List)
			{
				return ((List)o).size();
			}
		}
		
		return -1;
	}
	
	protected static Object tryGetIndex(Object o, int i)
	{
		if(null != o)
		{
			if(o.getClass().isArray())
			{
				return Array.get(o, i);
			}
			else if(o instanceof ArrayLike)
			{
				return ((ArrayLike)o).get(i);
			}
			else if(o instanceof List)
			{
				return ((List)o).get(i);
			}
		}
		
		return -1;
	}
	
	public static NotatedCaster tryCreateCaster(Type type)
	{
		Class cls = extractClass(type);
		
		NotatedCaster ret = WELL_KNOWN_CASTERS.get(cls);
		if(null != ret)
		{
			return ret;
		}
		
		if(cls.isArray())
		{
			NotatedCaster c = arrayCaster(cls);
			if(null != c)
			{
				return c;
			}
		}
		else
		{
			NotatedCaster c = CastTo.getCasterRestrictlyForTargetClass(cls);
			if(null != c)
			{
				return c;
			}
		}
		
		return null;
	}
	
	protected static ConcurrentMap<Class<?>, NotatedCaster> CASTERS = new ConcurrentHashMap<>();
	
	public static NotatedCaster getDirectCaster(final Type type)
	{
		final Class<?> clss = extractClass(type);
		NotatedCaster ret = CASTERS.get(clss);
		if(null == ret)
		{
			ret = new NotatedCaster()
			{
				@Override
				public Object cast(Object in)
				{
					if(null == in)
					{
						return null;
					}
					
					if(clss.isAssignableFrom(in.getClass()))
					{
						return in;
					}
					
					return null;
				}
				
				@Override
				public String getTypeShortName()
				{
					return clss.getSimpleName();
				}
				
				@Override
				public String getTypeFullQualifiedName()
				{
					return clss.getName();
				}
			}; 
			
			CASTERS.put(clss, ret);
		}
		
		return ret;
	}
	
	public static Class<?> extractClass(Type type)
	{
		if(type instanceof Class)
		{
			return (Class<?>) type;
		}
		else if(type instanceof ParameterizedType)
		{
			ParameterizedType t = (ParameterizedType) type;
			return extractClass(t.getRawType());
		}
		else if(type instanceof GenericArrayType)
		{
			GenericArrayType t = (GenericArrayType) type;
			return extractClass(t.getGenericComponentType());
		}
		else if(type instanceof TypeVariable)
		{
			TypeVariable tv = (TypeVariable) type;
			return extractClass(tv.getBounds()[0]);
		}
		else if(type instanceof GenericArrayType)
		{
			return extractClass(((GenericArrayType)type).getGenericComponentType());
		}
		else if(type instanceof WildcardType)
		{
			//WildcardType wt = (WildcardType) type;
		}
		else
		{
			//What's other?
		}
		
		throw new RuntimeException("Can't extract class of type: "+type);
	}

}
