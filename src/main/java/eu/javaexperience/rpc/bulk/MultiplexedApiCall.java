package eu.javaexperience.rpc.bulk;

import java.util.List;

import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.rpc.RpcTools;
import eu.javaexperience.rpc.RpcFacility;
import eu.javaexperience.rpc.RpcRequest;

public class MultiplexedApiCall extends BulkApiRequestApi
{
	protected final List<RpcFacility> apis;
	protected final GetBy1<RpcRequest, DataObject> requestCreator;
	
	public MultiplexedApiCall(List<RpcFacility> apis, GetBy1<RpcRequest, DataObject> requestCreator)
	{
		this.apis = apis;
		this.requestCreator = requestCreator;
	}

	@Override
	public DataObject handleSingleRequest(DataObject obj)
	{
		String ns = obj.optString("N");
		for(RpcFacility api:apis)
		{
			if(Mirror.equals(ns, api.getRpcName()))
			{
				return api.dispatch(requestCreator.getBy(obj));
			}
		}
		RpcTools.throwUnknownNamespace(ns);
		return null;
	}
}
