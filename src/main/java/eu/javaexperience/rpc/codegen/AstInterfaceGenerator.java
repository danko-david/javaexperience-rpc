package eu.javaexperience.rpc.codegen;

import java.util.Collection;
import java.util.Map;

import eu.javaexperience.datareprez.DataArray;
import eu.javaexperience.datareprez.DataCommon;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.jsonImpl.DataObjectJsonImpl;
import eu.javaexperience.datareprez.xmlImpl.DataObjectXmlImpl;
import eu.javaexperience.rpc.RpcFunction;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.function.RpcFunctionParameter;

public class AstInterfaceGenerator
{
	protected static DataObject renderParameter(DataCommon proto, RpcFunctionParameter param)
	{
		DataObject ret = proto.newObjectInstance();
		ret.putString("type", param.getTypeFullQualifiedName());
		return ret;
	}
	
	public static RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>> BASIC_AST_SOURCE_BUILDER = new RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>>()
	{
		@Override
		public String buildRpcClientSource(String compilationUnitName, Collection<RpcFunction<RpcRequest, RpcFunctionParameter>> functions, Map<String, Object> options)
		{
			DataObject ret = null;
			
			Object _dr = options.get("dr");
			String dr =  null;
			if(null == _dr)
			{
				dr = "json";
			}
			else
			{
				dr = _dr.toString();
			}
			
			switch(dr)
			{
				case "xml":
					ret = new DataObjectXmlImpl();
					break;
				
				case "json":
				default:
					ret = new DataObjectJsonImpl();
			}
			
			ret.putString("unit", compilationUnitName);
			
			
			DataArray methods = ret.newArrayInstance();
			
			//metódusok
			for(RpcFunction<RpcRequest, RpcFunctionParameter> f:functions)
			{
				DataObject m = ret.newObjectInstance();
				
				String comment = JavaRpcExportTools.renderFunctionComment(f);
				if(null != comment)
				{
					m.putString("comment", comment);
				}
				
				m.putString("methodName", f.getMethodName());
				m.putObject("returns", renderParameter(ret, f.getReturningClass()));
				
				DataArray params = ret.newArrayInstance();
				for(RpcFunctionParameter p:f.getParameterClasses())
				{
					params.putObject(renderParameter(ret, p));
				}
				
				m.putArray("parameters", params);
				
				methods.putObject(m);
			}
				
			ret.putArray("methods", methods);
			
			return new String(ret.toBlob());
		}
	};
}
