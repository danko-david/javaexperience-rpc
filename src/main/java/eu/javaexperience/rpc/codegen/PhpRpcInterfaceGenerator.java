package eu.javaexperience.rpc.codegen;

import java.util.Collection;
import java.util.Map;

import eu.javaexperience.rpc.RpcFunction;
import eu.javaexperience.rpc.RpcRequest;
import eu.javaexperience.rpc.function.RpcFunctionParameter;

/**
 * Emlegy:
 * 		a - action //metódus sorszáma
 * 		0, 1, 2, 3 ... //paraméterek 
 * 
 * Visszajön
 * 
 * 		r - viszatérési érték
 * 		e - kivétel.toString()

class SocketCommunicator
{
	private $socket;
	
	public function __construct($host, $port)
	{
		$this->socket = fsockopen($host, $port);
	}
	
	public function __descruct()
	{
		socket_close($this->socket);
	}
	
	public function txrx($arr)
	{
		fwrite($this->socket, json_encode($arr).chr(10));
		
		$str = fgets($this->socket);
		if($str === false)
		{
			throw new Exception("Rpc connection closed");
		}
		$ret = json_decode($str, true);
		
		if(isset($ret['e']))
		{
			throw new Exception(var_export($ret['e'],true));
		}
		
		return $ret['r'];
	}
}


 * */
public class PhpRpcInterfaceGenerator
{
	public static RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>> BASIC_PHP_SOURCE_BUILDER = new RpcSourceBuilder<RpcFunctionParameter, RpcFunction<RpcRequest, RpcFunctionParameter>>()
	{
		@Override
		public String buildRpcClientSource(String compilationUnitName, Collection<RpcFunction<RpcRequest, RpcFunctionParameter>> functions, Map<String, Object> options)
		{
			StringBuilder sb = new StringBuilder();

			sb.append("class ");
			sb.append(compilationUnitName);
			
				sb.append("\n{\n\tprivate $conn;\n\n\tpublic function __construct($conn");
				
				sb.append(")\n\t{\n\t\t$this->conn = $conn;\n");
				
				sb.append("\t}\n\n");
				
				//metódusok
				for(RpcFunction<RpcRequest, RpcFunctionParameter> f:functions)
				{
					String comment = JavaRpcExportTools.renderFunctionComment(f);
					if(null != comment)
					{
						sb.append(JavaRpcExportTools.withTabIndent(comment, 1));
						sb.append("\n");
					}
					
					sb.append("\tpublic function ");
					sb.append(f.getMethodName());
					sb.append("(");
					
					RpcFunctionParameter[] params = f.getParameterClasses();
					for(int p=0;p<params.length;p++)
					{
						if(p != 0)
						{
							sb.append(", $");
						}
						else
						{
							sb.append("$");
						}
						sb.append((char)('a'+p));
					}
					
					sb.append(")\n\t{\n\t\t");
					
					if(f.getReturningClass() != null && !void.class.equals(f.getReturningClass()))
					{
						sb.append("return ");
					}
					
					
					sb.append("$this->conn->txrx(array(");
					
					sb.append("'f' => '");
					sb.append(f.getMethodName());
					sb.append("', 'p' => array(");
					for(int p=0;p<params.length;p++)
					{
						if(0 != p)
						{
							sb.append(", ");
						}
						sb.append(" $");
						sb.append((char)('a'+p));
					}
					
					sb.append(")));\n\t}\n\n");
				}
				
			sb.append("}");
			
			return sb.toString();
		}
	};
	
	private static void _()
	{
		StringBuilder sb = new StringBuilder();
		
		String comm = null;//methods[0].getComment();
		if(comm != null)
		{
			sb.append("\t/*");
			sb.append(comm.replace("\n", "\n\t *"));
			sb.append("\t*/\n");
		}
	}
}