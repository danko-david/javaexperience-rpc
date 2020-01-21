package eu.javaexperience.rpc;

import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezJavaConverter;
import eu.javaexperience.semantic.references.MayNull;

public interface RpcProtocolHandler extends DataReprezJavaConverter, RpcProtocolCommon
{
	public String getPacketTraceId(RpcRequest req);
	
	public String extractNamespace(RpcRequest req);
	
	public Object extractThisContext(RpcRequest req);
	
	public String getRequestFunctionName(RpcRequest req);
	
	public Object[] extractParameters(RpcRequest req);
	
	public DataObject createEmptyResponsePacket(RpcRequest req);
	
	public DataObject createReturningValue(RpcRequest req, Object result);
	
	public DataObject createException(RpcRequest req, Throwable exception);
	
	public Object extract(@MayNull Class request, Object in);
}