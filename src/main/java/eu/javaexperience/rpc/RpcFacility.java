package eu.javaexperience.rpc;

import java.util.Collection;

import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.interfaces.simple.getBy.GetBy1;
 
public interface RpcFacility<Req extends RpcRequest> extends GetBy1<DataObject, Req>
{
	public String getRpcName();
	public DataObject dispatch(Req req);
	public Collection<? extends RpcFunction<Req, ?>> getWrappedFunctions();
}
