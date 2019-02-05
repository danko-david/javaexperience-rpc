package eu.javaexperience.rpc.discover;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import eu.javaexperience.arrays.ArrayTools;
import eu.javaexperience.collection.CollectionTools;
import eu.javaexperience.collection.enumerations.EnumTools;
import eu.javaexperience.collection.map.NullMap;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.interfaces.simple.getBy.GetBy2;
import eu.javaexperience.rpc.JavaClassRpcUnboundFunctionsInstance;
import eu.javaexperience.rpc.RpcFacility;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.SimpleRpcRequest;
import eu.javaexperience.rpc.codegen.WellKnownRpcSourceBuilders;

public class DiscoverRpc extends JavaClassRpcUnboundFunctionsInstance implements GetBy2<DataObject, SimpleRpcRequest, String>, DiscoverRpcInterface
{
	protected List<RpcFacility> facilities = new ArrayList<>();
	
	public DiscoverRpc(RpcFacility... fs)
	{
		super(DiscoverRpcInterface.class);
		facilities.add(this);
		CollectionTools.copyInto(ArrayTools.withoutNulls(fs), facilities);
	}
	
	@Override
	public String[] getNamespaces()
	{
		String[] ret = new String[facilities.size()];
		for(int i=0;i<ret.length;++i)
		{
			ret[i] = facilities.get(i).getRpcName();
		}
		
		return ret;
	}
	
	@Override
	public String help()
	{
		return getRpcName()+": Helps you to discover the RPC API facilities.\n"
				+ "String help(): this help message.\n"
				+ "String[] getNamespaces(): returns the available namespaces.\n"
				+ "source(String language, String namespace, Map<String, String> params): generates the api wrapper class for the requested language and namespace. (langs: see error message of an invalid language)";
	}
	
	//TODO function description: explain param usage, common options, and add behavior: if print_help present in options, print the options of the specific renderer. 
	@Override
	public String source(String language, String namespace, Map<String, String> params)
	{
		StringBuilder sb = new StringBuilder();
		WellKnownRpcSourceBuilders builder = EnumTools.recogniseSymbol(WellKnownRpcSourceBuilders.class, language);
		if(null == builder)
		{
			sb.append("Invalid target language `"+language+"` supported languages: "+ArrayTools.toString(WellKnownRpcSourceBuilders.values()));
		}
		
		RpcFacility fac = null;
		
		for(RpcFacility f:facilities)
		{
			if(f.getRpcName().equals(namespace))
			{
				fac = f;
				break;
			}
		}
		
		if(null == fac)
		{
			if(sb.length() > 0)
			{
				sb.append("\n");
			}
			sb.append("Unknown namespace: "+namespace);
		}
		
		if(sb.length() > 0)
		{
			throw new RuntimeException(sb.toString());
		}
		
		if(null == params)
		{
			params = NullMap.instance;
		}
		
		return builder.getBuilder().buildRpcClientSource(fac.getRpcName(), fac.getWrappedFunctions(), params);
	}
	
	//TODO connectors source (php_socket; javacript ajax/websocket; java proxy class name, function)
	
	@Override
	protected DataObject onMethodNotFound(RpcRequest ctx, String name)
	{
		return ctx.getProtocolHandler().createException(ctx, new RuntimeException("Unknown function: "+name+". You can get help in the `"+this.getRpcName()+"` namespace with the `String help()` method"));
	}
	
	@Override
	public boolean ping()
	{
		return true;
	}
	
	@Override
	public DataObject getBy(SimpleRpcRequest req, String ns)
	{
		return req.getProtocolHandler().createException(req, new RuntimeException("Unknown RPC namespace: "+ns+". You can list the namespaces in the `"+this.getRpcName()+"` namespace with the `String[] getNamespaces()` method"));
	}
}
