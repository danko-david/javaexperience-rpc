package eu.javaexperience.rpc.codegen;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import eu.javaexperience.rpc.RpcFunction;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.function.RpcFunctionParameter;
import eu.javaexperience.text.StringTools;

public class JavaRpcInterfaceGenerator
{
	public static void fillFunctionShortInterfaceSignature(StringBuilder sb, RpcFunction<? extends RpcRequest, RpcFunctionParameter> f, Collection<String> types_full_name)
	{
		fillFunctionShortInterfaceSignature(sb, f, types_full_name, null, null);
	}
	
	protected static final HashMap<String, String> CLASSIZE_TYPES = new HashMap<String, String>()
	{
		{
			put("void", "Void");
			put("boolean", "Boolean");
			put("byte", "Byte");
			put("char", "Character");
			put("short", "Short");
			put("int", "Integer");
			put("long", "Long");
			put("float", "Float");
			put("double", "Double");
		}
	};
	
	public static String renderCallbackType(String retType, String callbackClass, String replace)
	{
		if(null == replace)
		{
			return callbackClass;
		}
		String atype = CLASSIZE_TYPES.get(retType);
		if(null != atype)
		{
			retType = atype;
		}
		return StringTools.replaceAllStrings(callbackClass, replace, retType);
	}
	
	public static void fillFunctionShortInterfaceSignature
	(
		StringBuilder sb,
		RpcFunction<? extends RpcRequest, RpcFunctionParameter> f,
		Collection<String> types_full_name,
		String callbackClass,
		String callbackReplaceName
	)
	{
		String comment = JavaRpcExportTools.renderFunctionComment(f);
		if(null != comment)
		{
			sb.append(comment);
		}
		
		if(null == callbackClass)
		{
			sb.append(f.getReturningClass().getTypeName());
		}
		else
		{
			sb.append("void");
		}
		
		if(null != types_full_name)
		{
			types_full_name.add(f.getReturningClass().getTypeFullQualifiedName());
		}
		sb.append(" ");
		sb.append(f.getMethodName());
		sb.append("(");
		
		RpcFunctionParameter[] params = f.getParameterClasses();
		if(null != callbackClass)
		{
			sb.append(renderCallbackType(f.getReturningClass().getTypeName(), callbackClass, callbackReplaceName));
			sb.append(" cb");
		}
		
		for(int p=0;p<params.length;p++)
		{
			if(p != 0 || null != callbackClass)
			{
				sb.append(", ");
			}
			
			sb.append(params[p].getTypeName());
			if(null != types_full_name)
			{
				types_full_name.add(params[p].getTypeFullQualifiedName());
			}
			sb.append(" ");
			sb.append((char)('a'+p));
		}
		
		sb.append(");");
	}
	
	public static String getFunctionShortInterfaceSignature(RpcFunction<? extends RpcRequest, RpcFunctionParameter> f, Collection<String> types_full_name)
	{
		StringBuilder sb = new StringBuilder();
		fillFunctionShortInterfaceSignature(sb, f, types_full_name);
		return sb.toString();
	}
	
	
	public static RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>> BASIC_JAVA_SOURCE_BUILDER = new RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>>()
	{
		@Override
		public String buildRpcClientSource(String compilationUnitName, Collection<RpcFunction<RpcRequest, RpcFunctionParameter>> functions, Map<String, Object> options)
		{
			//InvocationHandler
			StringBuilder sb = new StringBuilder();
			
			sb.append("public interface ");
			sb.append(compilationUnitName);
			sb.append("\n{\n");
			
			String cb = null;
			if(null != options)
			{
				cb = (String) options.get("async");
			}
			
			String cbt = null;
			
			if(null != cb)
			{
				cbt = (String) options.get("async_rettype_replace");
			}
			
			//opts.put("async", "org.stjs.javascript.functions.Callback2<$rettype$,ClientException>");
			//opts.put("async_rettype_replace", "$rettype$");
			
			
			HashSet<String> types = new HashSet<>();
			
			//metódusok
			for(RpcFunction<RpcRequest, RpcFunctionParameter> f:functions)
			{
				sb.append("\tpublic ");
				fillFunctionShortInterfaceSignature(sb, f, types, cb, cbt);
				sb.append("\n");
			}
				
			sb.append("}");
			
			return sb.toString();
		}
	};
}
