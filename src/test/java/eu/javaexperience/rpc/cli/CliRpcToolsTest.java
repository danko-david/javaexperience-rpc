package eu.javaexperience.rpc.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.datareprez.javaImpl.DataObjectJavaImpl;
import eu.javaexperience.rpc.JavaClassRpcUnboundFunctionsInstance;
import eu.javaexperience.rpc.RpcFacility;
import eu.javaexperience.rpc.RpcSession;
import eu.javaexperience.rpc.RpcSessionTools;
import eu.javaexperience.rpc.RpcTools;

public class CliRpcToolsTest
{
	public static class RpcTest
	{
		public String addIs(int a, int b)
		{
			return String.valueOf(a+b);
		}
		
		
		public int addI(int a, int b)
		{
			return a+b;
		}
		
		public String wrap(String start, String content, String end)
		{
			return start+content+end;
		}
		
		public DataObject object(String key, Object value)
		{
			RpcSession sess = RpcSessionTools.getCurrentRpcSession();
			DataObject obj = sess.getDefaultRpcProtocolHandler().getDefaultCommunicationProtocolPrototype().newObjectInstance();
			DataReprezTools.put(obj, key, value);
			return obj;
		}
	}
	
	public static RpcFacility rpc()
	{
		return new JavaClassRpcUnboundFunctionsInstance<>(new RpcTest(), RpcTest.class);
	}
	
	public static DataObject invoke(String... args)
	{
		return RpcCliTools.cliExecute(null, rpc(), args);
	}
	
	@Test
	public void testHelp()
	{
		String help = RpcCliTools.generateCliHelp(rpc());
		assertTrue(help.contains("addI"));
		assertTrue(help.contains("addIs"));
		assertTrue(help.contains("wrap"));
		assertTrue(help.contains("object"));
	}
	
	@Test
	public void testCliAddIs()
	{
		DataObject ret = invoke("addIs", "2", "3"); 
		assertEquals("5", ret.getString("r"));
	}
	
	@Test
	public void testCliAddI()
	{
		DataObject ret = invoke("addI", "2", "3"); 
		assertEquals(5, ret.getInt("r"));
	}
	
	@Test
	public void testCliWrap()
	{
		DataObject ret = invoke("wrap", "/home/", "user", "/git");
		assertEquals("/home/user/git", ret.getString("r"));
	}
	
	@Test
	public void testCliObject()
	{
		DataObject ret = invoke("object", "key", "value");
		DataObject obj = ret.getObject("r");
		assertEquals("value", obj.getString("key"));
	}
}
