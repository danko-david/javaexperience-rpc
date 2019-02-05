package eu.javaexperience.rpc.codegen;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.javaexperience.annotation.FunctionDescription;
import eu.javaexperience.annotation.FunctionVariableDescription;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.interfaces.simple.getBy.GetBy2;
import eu.javaexperience.interfaces.simple.getBy.GetByTools;
import eu.javaexperience.io.IOTools;
import eu.javaexperience.regex.RegexTools;
import eu.javaexperience.rpc.JavaClassRpcCollector;
import eu.javaexperience.rpc.RpcFacility;
import eu.javaexperience.rpc.RpcFunction;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.function.JavaFunctionRpcWrapper;
import eu.javaexperience.rpc.function.RpcFunctionParameter;
import eu.javaexperience.semantic.references.MayNull;
import eu.javaexperience.text.RegexFunctions;
import eu.javaexperience.text.StringFunctions;
import eu.javaexperience.text.StringTools;

public class JavaRpcExportTools
{
	public static void exportInterfaceToFile
	(
		String dstFile,
		String compilationUnitName,
		Map<String, Object> buildOptions,
		Collection<RpcFunction<RpcRequest, RpcFunctionParameter>> functions,
		@MayNull GetBy1<String, String> sourceModifier
	)
		throws IOException
	{
		String ret = JavaRpcInterfaceGenerator.BASIC_JAVA_SOURCE_BUILDER.buildRpcClientSource
		(
			compilationUnitName,
			functions,
			buildOptions
		);
		
		if(null != sourceModifier)
		{
			ret = sourceModifier.getBy(ret);
		}
		
		IOTools.putFileContent(dstFile, ret.getBytes());
	}
	
	public static void exportInterfacesToDir
	(
		String dstDir,
		Map<String, Object> buildOptions,
		@MayNull GetBy1<String, String> sourceModifier,
		JavaClassRpcCollector... apis
	)
		throws IOException
	{
		for(JavaClassRpcCollector api:apis)
		{
			exportInterfaceToFile
			(
				dstDir+"/"+api.getRpcName()+".java",
				api.getRpcName(),
				buildOptions,
				api.getWrappedFunctions(),
				sourceModifier
			);
		}
	}
	
	public static void generateToDir
	(
		String dstDir,
		GetBy2<String, String, Collection<JavaFunctionRpcWrapper>> generator,
		List<RpcFacility> list
	)
		throws IOException
	{
		for(RpcFacility api:list)
		{
			String data = generator.getBy(api.getRpcName(), api.getWrappedFunctions());
			IOTools.putFileContent(dstDir+"/"+api.getRpcName()+".java", data.getBytes());
		}
	}
	
	public static GetBy1<String, String> addPackage(String pack)
	{
		return StringFunctions.withPrefix("package "+pack+";\n\n");
	}
	
	public static GetBy1<String, String> withImportedClasses(Class... importList)
	{
		StringBuilder imports = new StringBuilder();
		for(Class c:importList)
		{
			imports.append("\nimport ");
			imports.append(c.getName());
			imports.append(";");
		}
		imports.append("\n");
		
		return StringFunctions.withPrefix(imports.toString());
	}
	
	public static GetBy1<String, String> withInterfaceAnnotation(Class<? extends Annotation>... annot)
	{
		StringBuilder sb = new StringBuilder();
		for(Class c:annot)
		{
			sb.append("\n@");
			sb.append(c.getName());
			sb.append("\n");
		}
		sb.append("\npublic interface");
		
		return StringFunctions.replaceAll("public interface", sb.toString());
	}
	
	
	public static class SourceModifierBuilder
	{
		public List<GetBy1<String, String>> modifiers = new ArrayList<>();
		
		public SourceModifierBuilder addModifier(GetBy1<String, String> modder)
		{
			modifiers.add(modder);
			return this;
		}
		
		public GetBy1<String, String> build()
		{
			return GetByTools.chain((GetBy1[]) modifiers.toArray(GetByTools.emptyGetBy1Array));
		}
	}
	
	protected static final GetBy1<String, String> makeAbstract = RegexFunctions.regexReplace("(?!abstract)\\s+(class|interface)\\s+(.*)", false, " abstract class $2");
	
	public static GetBy1<String, String> makeClassAbstract()
	{
		return makeAbstract;
	}
	/*
	protected static final GetBy1<String, String> makePublicFunctions = RegexFunctions.regexReplace("public\\s+(abstract)?\\s+(class|interface)\\s+(.*)", true, " abstract class $3");
	
	public static GetBy1<String, String> makeClassAbstract()
	{
		return makeAbstract;
	}
	*/
	
	protected static GetBy1<String, String> afterClassName(String after)
	{
		return RegexFunctions.regexReplace("(class|interface)\\s+(.*)", true, " $1 $2"+after);
	}
	
	public static GetBy1<String, String> extendsClass(String cls)
	{
		return afterClassName(" extends "+cls);
	}
	
	public static GetBy1<String, String> implementsClass(String cls)
	{
		return afterClassName(" implements "+cls);
	}
	
	public static @MayNull FunctionDescription collectComments(RpcFunction func)
	{
		if(func instanceof JavaFunctionRpcWrapper)
		{
			return ((JavaFunctionRpcWrapper) func).getJavaMethod().getAnnotation(FunctionDescription.class);
		}
		
		return null;
	}
	
	public static String renderFunctionComment(RpcFunction desrc)
	{
		FunctionDescription f = collectComments(desrc);
		if(null != f)
		{
			return renderFunctionComment(f);
		}
		
		return null;
	}
	
	/**
	 * 
	 * 
	 * */
	public static String renderFunctionComment(FunctionDescription desrc)
	{
		if(null == desrc)
		{
			return "";
		}
		
		StringBuilder sb = new StringBuilder();
		
		sb.append("/**\n");
		
		{
			String d = desrc.functionDescription();
			if(null != d)
			{
				sb.append(" * ");
				printMultiline(sb, d);
				printSeparator(sb);
			}
		}
		
		{
			FunctionVariableDescription[] params = desrc.parameters();
			for(FunctionVariableDescription p:params)
			{
				printParamWithPrefix(p, sb, "@param");
			}
			
			printSeparator(sb);
		}
		
		printParamWithPrefix(desrc.returning(), sb, "@returns");
		
		sb.append(" * */\n");
		
		return sb.toString();
	}
	
	protected static void printParamWithPrefix(FunctionVariableDescription p, StringBuilder sb, String prefix)
	{
		sb.append(" * ");
		sb.append(prefix);
		sb.append(" ");
		if(p.mayNull())
		{
			sb.append("@MayNull ");
		}
		else
		{
			sb.append("@MayNotNull ");
		}
		
		sb.append(p.type().getName());
		sb.append(" ");
		
		sb.append(p.paramName());
		sb.append(": ");
		
		String d = p.description();
		if(null != d)
		{
			printMultiline(sb, d);
		}
	}
	
	protected static void printSeparator(StringBuilder sb)
	{
		sb.append(" *\n");
	}
	
	protected static void printMultiline(StringBuilder sb, String line)
	{
		String[] ds = RegexTools.LINUX_NEW_LINE.split(line);
		int i = 0;
		for(String s:ds)
		{
			if(++i > 1)
			{
				sb.append(" * ");
				sb.append("\t");
			}
			sb.append(s);
			sb.append("\n");
		}
	}
	
	public static String withTabIndent(String text, int tabs)
	{
		return StringTools.splitModifyJoin(text, RegexTools.LINUX_NEW_LINE, StringFunctions.withPrefix(StringTools.repeatChar('\t', tabs)),"\n");
	}
}
