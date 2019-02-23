package eu.javaexperience.rpc.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import eu.javaexperience.asserts.AssertArgument;
import eu.javaexperience.datareprez.DataArray;
import eu.javaexperience.datareprez.DataObject;
import eu.javaexperience.datareprez.DataReprezTools;
import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.rpc.JavaClassRpcUnboundFunctionsInstance;
import eu.javaexperience.rpc.RpcFacility;
import eu.javaexperience.rpc.RpcSession;
import eu.javaexperience.rpc.RpcSessionTools;

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
		
		public String[] iVarArgs(int num, String... args)
		{
			return args;
		}
		
		public String[] iSVarArgs(int num, String a, String... args)
		{
			return args;
		}
		
		public String[] varArgs(String... args)
		{
			return args;
		}
		
		public String[] vaCC(String a, String b, String... args)
		{
			ArrayList<String> ret = new ArrayList<>();
			ret.add(a);
			ret.add(b);
			for(String s:args)
			{
				ret.add(s);
			}
			return ret.toArray(Mirror.emptyStringArray);
		}
		
		public int[] vaInt(int... args)
		{
			return args;
		}
		
		public void call()
		{
			System.out.println("call");
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
	
	@Test
	public void testCliVarArgs()
	{
		DataObject ret = invoke("varArgs");
		DataArray arr = ret.getArray("r");
		assertEquals(0, arr.size());
	}
	
	@Test
	public void testCliVarArgs1()
	{
		DataObject ret = invoke("varArgs", "a");
		DataArray arr = ret.getArray("r");
		assertEquals(1, arr.size());
		assertEquals("a", arr.getString(0));
	}
	
	@Test
	public void testCliVarArgs2()
	{
		DataObject ret = invoke("varArgs", "a", "b");
		DataArray arr = ret.getArray("r");
		assertEquals(2, arr.size());
		assertEquals("a", arr.getString(0));
		assertEquals("b", arr.getString(1));
	}
	
	@Test
	public void testCliIVarArgs()
	{
		DataObject ret = invoke("iVarArgs", "1");
		DataArray arr = ret.getArray("r");
		assertEquals(0, arr.size());
	}
	
	@Test
	public void testCliIVarArgs1()
	{
		DataObject ret = invoke("iVarArgs", "1", "a");
		DataArray arr = ret.getArray("r");
		assertEquals(1, arr.size());
		assertEquals("a", arr.getString(0));
	}
	
	@Test
	public void testCliIVarArgs2()
	{
		DataObject ret = invoke("iVarArgs", "1", "a", "b");
		DataArray arr = ret.getArray("r");
		assertEquals(2, arr.size());
		assertEquals("a", arr.getString(0));
		assertEquals("b", arr.getString(1));
	}
	
	@Test
	public void testCliISVarArgs()
	{
		DataObject ret = invoke("iSVarArgs", "1", "x");
		DataArray arr = ret.getArray("r");
		assertEquals(0, arr.size());
	}
	
	@Test
	public void testCliISVarArgs1()
	{
		DataObject ret = invoke("iSVarArgs", "1", "x", "a");
		DataArray arr = ret.getArray("r");
		assertEquals(1, arr.size());
		assertEquals("a", arr.getString(0));
	}
	
	@Test
	public void testCliISVarArgs2()
	{
		DataObject ret = invoke("iSVarArgs", "1", "x", "a", "b");
		DataArray arr = ret.getArray("r");
		assertEquals(2, arr.size());
		assertEquals("a", arr.getString(0));
		assertEquals("b", arr.getString(1));
	}
	
	@Test
	public void testCliVacc()
	{
		DataObject ret = invoke("vaCC", "1", "x");
		DataArray arr = ret.getArray("r");
		assertEquals(2, arr.size());
		assertEquals("1", arr.getString(0));
		assertEquals("x", arr.getString(1));
	}
	
	@Test
	public void testCliVacc1()
	{
		DataObject ret = invoke("vaCC", "1", "x", "b");
		DataArray arr = ret.getArray("r");
		assertEquals(3, arr.size());
		assertEquals("1", arr.getString(0));
		assertEquals("x", arr.getString(1));
		assertEquals("b", arr.getString(2));
	}

	@Test
	public void testCliVacc2()
	{
		DataObject ret = invoke("vaCC", "1", "x", "b", "c");
		DataArray arr = ret.getArray("r");
		assertEquals(4, arr.size());
		assertEquals("1", arr.getString(0));
		assertEquals("x", arr.getString(1));
		assertEquals("b", arr.getString(2));
		assertEquals("c", arr.getString(3));
	}
	
	@Test
	public void testVaInt()
	{
		DataObject ret = invoke("vaInt", "1", "2", "3", "4");
		DataArray arr = ret.getArray("r");
		assertEquals(4, arr.size());
		assertEquals(1, arr.getInt(0));
		assertEquals(2, arr.getInt(1));
		assertEquals(3, arr.getInt(2));
		assertEquals(4, arr.getInt(3));
	}
	
	@Test
	public void testCall()
	{
		DataObject ret = invoke("call");
		AssertArgument.assertTrue(ret.isNull("r"), "return value must be \"null\"");
	}
	
}
