package eu.javaexperience.rpc.cli;

import java.util.Arrays;

import eu.javaexperience.collection.map.NullMap;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.rpc.RpcFacility;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.RpcSession;
import eu.javaexperience.rpc.RpcSessionTools;
import eu.javaexperience.rpc.SimpleRpcSession;
import eu.javaexperience.rpc.bidirectional.BidirectionalRpcDefaultProtocol;
import eu.javaexperience.rpc.codegen.JavaRpcInterfaceGenerator;
import eu.javaexperience.semantic.references.MayNull;

public class RpcCliTools
{
	public static DataObject cliExecute(@MayNull RpcSession session, RpcFacility rpc, String... args)
	{
		if(null == session)
		{
			session = new SimpleRpcSession(BidirectionalRpcDefaultProtocol.DEFAULT_PROTOCOL_HANDLER_WITH_CLASS);
		}
		BidirectionalRpcDefaultProtocol proto = (BidirectionalRpcDefaultProtocol) session.getDefaultRpcProtocolHandler();
		RpcRequest req = proto.createClientRequest(session);
		
		DataObject rd = req.getRequestData();
		if(args.length > 0)
		{
			proto.putRequestFunctionName(req, args[0]);
			proto.putParameters(req, Arrays.copyOfRange(args, 1, args.length));
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
}
