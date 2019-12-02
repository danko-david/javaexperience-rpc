package eu.javaexperience.rpc.cli;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;

import eu.javaexperience.cli.CliEntry;
import eu.javaexperience.cli.CliTools;
import eu.javaexperience.collection.map.NullMap;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.rpc.RpcFacility;
import eu.javaexperience.rpc.RpcFunction;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.RpcSession;
import eu.javaexperience.rpc.RpcSessionTools;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;
import eu.javaexperience.rpc.codegen.JavaRpcInterfaceGenerator;
import eu.javaexperience.rpc.function.JavaFunctionRpcWrapper;
import eu.javaexperience.semantic.references.MayNull;

public class RpcCliTools
{
	private RpcCliTools() {}
	
	public static DataObject cliExecute(@MayNull RpcSession session, RpcFacility rpc, String... args)
	{
		if(null == session)
		{
			session = new SimpleRpcSession(BidirectionalRpcDefaultProtocol.DEFAULT_PROTOCOL_HANDLER_WITH_CLASS);
		}
		
		BidirectionalRpcDefaultProtocol proto = (BidirectionalRpcDefaultProtocol) session.getDefaultRpcProtocolHandler();
		RpcRequest req = proto.createClientRequest(session);
		
		if(args.length > 0)
		{
			RpcFunction func = null;
			
			String name = args[0];
			//find function
			for(RpcFunction f:((Collection<RpcFunction>)rpc.getWrappedFunctions()))
			{
				if(f.getMethodName().equals(name))
				{
					func = f;
					break;
				}
			}
			
			proto.putRequestFunctionName(req, name);
			
			Object[] params = Arrays.copyOfRange(args, 1, args.length);
			
			if(func instanceof JavaFunctionRpcWrapper)
			{
				Method m = ((JavaFunctionRpcWrapper) func).getJavaMethod();
				
				Parameter[] pcs = m.getParameters();
				if(0 != pcs.length)
				{
					if(pcs[pcs.length-1].isVarArgs())
					{
						//wrap varargs
						Object[] p2 = Arrays.copyOfRange(args, 1, pcs.length+1, Object[].class);
						Object[] va = Mirror.emptyObjectArray;
						if(params.length >= pcs.length)
						{
							va = Arrays.copyOfRange(args, pcs.length, params.length+1, Object[].class);
						}
						p2[p2.length-1] = va;
						params = p2;
					}
				}
			}
			
			proto.putParameters(req, params);
		}
		
		RpcSessionTools.setCurrentRpcSession(session);
		try
		{
			return rpc.dispatch(req);
		}
		finally
		{
			RpcSessionTools.setCurrentRpcSession(null);
		}
	}
	
	public static String generateCliHelp(RpcFacility facility)
	{
		return JavaRpcInterfaceGenerator.BASIC_JAVA_SOURCE_BUILDER.buildRpcClientSource("Cli", facility.getWrappedFunctions(), NullMap.instance);
	}
	
	public static DataObject cliExecOrHelp(@MayNull RpcSession session, RpcFacility rpc, String... args)
	{
		if(0 == args.length)
		{
			System.out.println(generateCliHelp(rpc));
			return null;
		}
		
		return cliExecute(session, rpc, args);
	}
	
	public static void printHelpAndExit(String programName, int exitCode, CliEntry... entries)
	{
		System.err.println("Usage of "+prograName+":\n");
		System.err.println(CliTools.renderListAllOption(entries));
		System.exit(exitCode);
	}
}
