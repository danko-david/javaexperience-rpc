package eu.javaexperience.rpc.api.doc;

import java.util.HashMap;
import java.util.Map;

import eu.javaexperience.reflect.Mirror;
import eu.javaexperience.reflect.property.PropertyAccessTools;
import eu.javaexperience.reflect.property.WellKnownDataAccessors;

public class DocumentationRegistry implements DocumentationApi
{
	protected Map<String, Object> nodeTree = new HashMap<String, Object>();
	protected Map<String, Object> accessor = wrap(nodeTree);
	
	protected Map<String, Object> wrap(Object o)
	{
		return PropertyAccessTools.wrap
		(
			o,
			WellKnownDataAccessors.MAP,
			WellKnownDataAccessors.OBJECT_LIKE,
			WellKnownDataAccessors.ARRAY_LIKE,
			WellKnownDataAccessors.ARRAY_SIMPLEST,
			WellKnownDataAccessors.LIST_SIMPLEST,
			PropertyAccessTools.EXTRACT_NONPRIMITIVE_OBJECTS
		);
	}
	
	public synchronized void addRootNode(String node, Object obj)
	{
		nodeTree.put(node, obj);
	}
	
	public synchronized String[] getKeysOf(String key)
	{
		Object ret = accessor.get(key);
		if(null == ret)
		{
			return null;
		}
		
		return wrap(ret).keySet().toArray(Mirror.emptyStringArray);
	}
	
	public synchronized DocApiNode get(String path)
	{
		Object ret = accessor.get(path);
		if(ret instanceof Map)
		{
			return new DocApiNode(path, false, ((Map) ret).keySet());
		}
		return new DocApiNode(path, true, ret);
	}
}
