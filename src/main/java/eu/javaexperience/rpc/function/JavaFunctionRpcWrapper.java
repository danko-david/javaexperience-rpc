package eu.javaexperience.rpc.function;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import eu.javaexperience.collection.map.SmallMap;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.datareprez.convertFrom.ArrayLike;
import eu.javaexperience.reflect.CastTo;
import eu.javaexperience.reflect.FieldSelectTools;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.NotatedCaster;
import eu.javaexperience.rpc.RpcFunction;
import eu.javaexperience.rpc.RpcRequest;

//TODO make abstract parent for this and JavaAsIsFunctionWrapper
public class JavaFunctionRpcWrapper<C extends RpcRequest> implements RpcFunction<C, RpcFunctionParameter>
{
	protected Method javaMethod;
	protected String name;
	
	protected RpcFunctionParameter returningType;
	protected RpcFunctionParameter[] params;
	
	public static JavaFunctionRpcWrapper<RpcRequest> wrapJavaFunction(Method m)
	{
		JavaFunctionRpcWrapper ret = new JavaFunctionRpcWrapper<>();
		ret.javaMethod = m;
		ret.javaMethod.setAccessible(true);
		ret.name = m.getName();
		ret.returningType = createByType(m.getGenericReturnType());
		
		Type[] params = m.getGenericParameterTypes();
		ret.params = new RpcFunctionParameter[params.length-1];
		for(int i=1;i<params.length;++i)
		{
			ret.params[i-1] = createByType(params[i]);
		}
		
		return ret;
	}
	
	@Override
	public String getMethodName()
	{
		return name;
	}
	
	@Override
	public RpcFunctionParameter[] getParameterClasses()
	{
		return params;
	}
	
	@Override
	public RpcFunctionParameter getReturningClass()
	{
		return returningType;
	}
	
	public Method getJavaMethod()
	{
		return javaMethod;
	}
	
	protected Object[] assembleJavaFunctionParameters(C ctx, Object thisContext, String functionName, Object... params)
	{
		Object[] cll = new Object[params.length+1];
		cll[0] = ctx;
		for(int i=0;i<params.length;++i)
		{
			cll[i+1] = params[i];
		}
		
		return cll;
	}
	
	@Override
	public Object call(C ctx, Object thisContext, String functionName, Object... params) throws Throwable
	{
		Object[] cll = assembleJavaFunctionParameters(ctx, thisContext, functionName, params);
		
		try
		{
			return javaMethod.invoke(thisContext, cll);
		}
		catch(InvocationTargetException ex)
		{
			if(ex.getCause() instanceof Throwable)
			{
				throw ex.getCause();
			}
			throw ex;
		}
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
						
						Object[] ret = (Object[]) Array.newInstance(component, len);
						for(int i=0;i<len;++i)
						{
							ret[i] = c.cast(tryGetIndex(in, i));
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
				return "java.lang.Map";
			}
		});
	}
	
	public static NotatedCaster tryCreateCaster(Type type)
	{
		Class cls = extractClass(type);
		
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
		
		return WELL_KNOWN_CASTERS.get(cls);
	}
	
	protected static ConcurrentMap<Class<?>, NotatedCaster> CASTERS = new ConcurrentHashMap<>();
	
	protected static NotatedCaster getDirectCaster(final Type type)
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
	
	public static RpcFunctionParameter createByType(Type type)
	{
		Class<?> cls = extractClass(type);
		
		
		if(void.class == cls || Void.class == cls)
		{
			return new RpcFunctionParameter(new NotatedCaster()
			{
				@Override
				public Object cast(Object in)
				{
					return null;
				}

				@Override
				public String getTypeShortName()
				{
					return "void";
				}

				@Override
				public String getTypeFullQualifiedName()
				{
					return "void";
				}
			});
		}
		
		NotatedCaster c = tryCreateCaster(type);
		if(null != c)
		{
			return new RpcFunctionParameter(c);
		}
		
		if(null == cls)
		{
			throw new RuntimeException("No suitable automatic caster for type: "+type);
		}
		
		return new RpcFunctionParameter(getDirectCaster(cls));
	}
	
	public static void teszt(RpcRequest req, int[] values)
	{
		System.out.println("Values: "+Arrays.toString((int[])values));
	}
	
	public static void main(String[] args) throws Throwable
	{
		Method m = JavaFunctionRpcWrapper.class.getMethod("teszt", RpcRequest.class, int[].class);
		JavaFunctionRpcWrapper func = wrapJavaFunction(m);
		
		func.call(null, null, "", new int[]{10,20});
		//System.out.println(m.getReturnType());
	}
}
