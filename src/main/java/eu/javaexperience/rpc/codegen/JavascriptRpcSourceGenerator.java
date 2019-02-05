package eu.javaexperience.rpc.codegen;

import java.util.Collection;
import java.util.Map;

import eu.javaexperience.rpc.RpcFunction;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.function.RpcFunctionParameter;

public class JavascriptRpcSourceGenerator
{
	public static final String AJAX_TRANSMIT_SOURCE_CODE =
	"function ajaxTransmit(url)\n"+
	"{\n"+
	"	this.is_valuable = function(elem)\n"+
	"	{\n"+
	"		return undefined != elem && null != elem;\n"+
	"	};\n"+
	"\n"+
	"	this.rxtx = function(request, response)\n"+
	"	{\n"+
	"		var cb = this.is_valuable();\n"+
	"\n"+
	"		var xhr = new XMLHttpRequest();\n"+
	"		var ajaxTrans = this;\n"+
	"		if(this.is_valuable(response))\n"+
	"		{\n"+
	"			xhr.open('POST', url);\n"+
	"			var called = [false];\n"+
	"			xhr.onreadystatechange = function()\n"+
	"			{\n"+
	"				if(called[0])\n"+
	"				{\n"+
	"					return;\n"+
	"				}\n"+
	"\n"+
	"				called[0] = true;\n"+
	"\n"+
	"				if(xhr.status === 200)\n"+
	"				{\n"+
	"					var data = JSON.parse(xhr.responseText);\n"+
	"					if(ajaxTrans.is_valuable(data.r))\n"+
	"					{\n"+
	"						response(data.r, null);\n"+
	"					}\n"+
	"					else if(ajaxTrans.is_valuable(data.e))\n"+
	"					{\n"+
	"						response(null, data.e);\n"+
	"					}\n"+
	"					return null;\n"+
	"				}\n"+
	"			}\n"+
	"			xhr.send(JSON.stringify(request));\n"+
	"		}\n"+
	"		else\n"+
	"		{\n"+
	"			xhr.open('POST', url, false);\n"+
	"			xhr.send(JSON.stringify(request));\n"+
	"\n"+
	"			var data = JSON.parse(xhr.responseText);\n"+
	"			if(this.is_valuable(data.r))\n"+
	"			{\n"+
	"				return data.r;\n"+
	"			}\n"+
	"			else if(this.is_valuable(data.e))\n"+
	"			{\n"+
	"				throw data.e;\n"+
	"			}\n"+
	"			return null;\n"+
	"		}\n"+
	"	}\n"+
	"}\n"+
	"\n";
		
	//"ws://"+location.hostname+(location.port ? ':'+location.port: '')+"/system/socket/connect"
	public static final String WEBSOCKET_BIDIRECTIONAL_SOURCE = 
"	window.WebsocketTransfer = function(url, serverRequestHandler)\n"+
"	{\n"+
"		this.NEXT_PACKET_ID = 0;\n"+
"\n"+		//packet_trace_id => {id, request, handler}\n"+
"		this.PENDING_PACKETS = {};\n"+
"\n"+
"		this.is_valuable = function(o)\n"+
"		{\n"+
"			return null != o && undefined != o;\n"+
"		}\n"+
"\n"+
"		this.is_function = function(o)\n"+
"		{\n"+
"			return \"function\" === typeof o;\n"+
"		}\n"+
"\n"+
"		this.serverRequestHandler = serverRequestHandler;\n"+
"		this.SOCKET = new WebSocket(url);\n"+
"		this.SOCKET.onmessage = function(event)\n"+
"		{\n"+
"			var data = JSON.parse(event.data);\n"+
"			var id = data.t;\n"+
"			if(this.is_valuable(id))\n"+
"			{\n"+
"				var descriptor = this.PENDING_PACKETS[id];\n"+
"				delete this.PENDING_PACKETS[id];\n"+
"				if(this.is_function(descriptor.handler))\n"+
"				{\n"+
"					descriptor.handler(descriptor.request, data, event);\n"+
"				}\n"+
"			}\n"+
"			else\n"+
"			{\n"+
"				if(this.is_function(this.serverRequestHandler))\n"+
"				{\n"+
"					this.serverRequestHandler(event.data, event);\n"+
"				}\n"+
"			}\n"+
"		}.bind(this);\n"+
"\n"+
"		this.request = function(request, handler)\n"+
"		{\n"+
"			var id = \"c\"+(++this.NEXT_PACKET_ID);\n"+
"			request.t = id;\n"+
"			this.PENDING_PACKETS[id] =\n"+
"			{\n"+
"				id: id,\n"+
"				request: request,\n"+
"				handler: handler\n"+
"			};\n"+
"\n"+
"			var send = JSON.stringify(request);\n"+
"			this.SOCKET.send(send);\n"+
"		}\n"+
"\n"+
"		this.simple_request = function(send, opt_notify)\n"+
"		{\n"+
"			this.request\n"+
"			(\n"+
"				send,\n"+
"				function(req, response, event)\n"+
"				{\n"+
"					if(this.is_function(opt_notify))\n"+
"					{\n"+
"						opt_notify(req, response, event);\n"+
"					}\n"+
"				}.bind(this)\n"+
"			);\n"+
"		}\n"+
"\n"+
"		this.rxtx = function(request, cb)\n"+
"		{\n"+
"			this.simple_request\n"+
"			(\n"+
"				request,\n"+
"				function(req, response, event)\n"+
"				{\n"+
"					if(this.is_function(cb))\n"+
"					{\n"+
"						cb(response.r, response.e, req, event);\n"+
"					}\n"+
"				}.bind(this)\n"+
"			);\n"+
"		}\n"+
"}";
	
	/**
	 * ACCEPT OPTION: using_callback_return
	 * 
	 * 
	 * */
	public static RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>> BASIC_JAVASCRIPT_SOURCE_BUILDER = 
		new RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>>()
	{
		@Override
		public String buildRpcClientSource(String compilationUnitName, Collection<RpcFunction<RpcRequest, RpcFunctionParameter>> functions, Map<String, Object> options)
		{
			StringBuilder sb = new StringBuilder();
			
			boolean use_cb = false;
			if(null != options && (options.containsKey("using_callback_return") || options.containsKey("async")))
			{
				use_cb = true;
			}
			
			sb.append("function ");
			sb.append(compilationUnitName);
			sb.append("(conn)");
			
			boolean teavm = false;
			
			if(options.containsKey("teavm"))
			{
				teavm = true;
			}
			
			// konstruktor {
				sb.append("\n{\n\tthis.transfer = conn;\n\n");
				
			//	}
			
				// segéd metódusok	{
		/*		
				sb.append("\tfunction objectToArray($d)\n\t{\n\t\tif (is_object($d))\n\t\t\t$d = get_object_vars($d);\n\t\tif (is_array($d))\n\t\t\treturn array_map(__METHOD__, $d);\n\t\telse\n\t\t\treturn $d;\n\t}\n\n");
				
				sb.append("\tfunction arrayToObject($d)\n\t{\n\t\tif (is_array($d))\n\t\t\treturn (object) array_map(__METHOD__, $d);\n\t\telse\n\t\t\treturn $d;\n\t}\n\n");
				
				sb.append("\tprivate function sendArray($arr)\n\t{\n\t\tfwrite($this->socket, (json_encode( $this->arrayToObject($arr))).chr(10));\n\t}\n\n");
				
				sb.append("\tprivate function receiveArray()\n\t{\n\t\t$str = fgets($this->socket);\n\n\t\tif($str === false)\n\t\t\tthrow new Exception(\"A kacsolat bezárult!\");\n\t\t$array = $this->objectToArray(json_decode($str));\n\n\t\t if(isset($array['e']))\n\t\t\tthrow new Exception(var_export($array['e'],true));\n\n\t\treturn array_key_exists('r',$array)?$array['r']:null;\n\t}\n\n");
				
				sb.append("\tprivate function close()\n\t{\n\t\t$this->sendArray(array('a'=>'close'));\n\t\tfclose($this->socket);\n\t}\n\n");
				
		*/
				//sb.append("\tprivate function rxtx($arr)\n\t{\n\t\t$this->sendArray($arr);\n\t\treturn $this->receiveArray();\n\t}\n\n");
			//	}
				
				
			//destruktor
			//sb.append("\tpublic function __destruct()\n\t{\n\t\t$this->close();\n\t}\n\n");
				
				//metódusok
				for(RpcFunction<RpcRequest, RpcFunctionParameter> f:functions)
				{
					String comment = JavaRpcExportTools.renderFunctionComment(f);
					if(null != comment)
					{
						sb.append(comment);
					}
					
					sb.append("\tthis.");
					
					if(teavm)
					{
						sb.append("$");
					}
					
					sb.append(f.getMethodName());
					sb.append(" = function(");
					
					/*if(teavm)
					{
						sb.append("$this, ");
					}*/
					
					RpcFunctionParameter[] params = f.getParameterClasses();
					
					if(use_cb)
					{
						sb.append("callback");
					}
					
					for(int p=0;p<params.length;p++)
					{
						if(use_cb || p != 0)
						{
							sb.append(", ");
						}
						sb.append((char)('a'+p));
					}
					
					sb.append(")\n\t{\n\t\t");
					
					if(f.getReturningClass() != null && !void.class.equals(f.getReturningClass()))
					{
						sb.append("return ");
					}
					
					
					if(teavm)
					{
						//sb.append("this.transfer.$publish(null, {'f': '");
						sb.append("this.transfer.$publish({'f': '");
					}
					else
					{
						sb.append("this.transfer.rxtx({'f': '");
					}
					sb.append(f.getMethodName());
					sb.append("', 'p' : [");
					for(int p=0;p<params.length;p++)
					{
						if(0 != p)
						{
							sb.append(", ");
						}
						sb.append((char)('a'+p));
					}
					
					sb.append("]}, ");
					sb.append(use_cb?"callback":"null");
					sb.append(");\n\t};\n\n");
				}
			
			sb.append("};\n");
			
			return sb.toString();
		}
	};
}
