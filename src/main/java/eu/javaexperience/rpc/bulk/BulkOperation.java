package eu.javaexperience.rpc.bulk;

import java.util.Collection;
import java.util.HashSet;

import eu.javaexperience.collection.CollectionTools;

public abstract class BulkOperation<K, M>
{
	protected final HashSet<K> supported;
	
	public BulkOperation(K... supported)
	{
		this.supported = new HashSet<K>();
	}
	
	public static class RequestedFields<K, M>
	{
		protected BulkOperation<K, M> owner;
		
		public RequestedFields(BulkOperation<K, M> owner)
		{
			this.owner = owner;
		}
		
		protected HashSet<K> req = new HashSet<K>();
		
		public BulkOperation<K, M> getOwner()
		{
			return owner;
		}
		
		public boolean needReport(K supported)
		{
			return this.req.contains(supported);
		}
	}
	
	public RequestedFields<K, M> readRequestedFields(K... req)
	{
		RequestedFields<K, M> ret = new RequestedFields<>(this);
		
		for(K r:req)
		{
			if(isFieldSupported(r))
			{
				ret.req.add(r);
			}
		}
		
		return ret;
	}
	
	public boolean isFieldSupported(K r)
	{
		return supported.contains(r);
	}
	
	public void fillSupportedResults(Collection<K> dst)
	{
		CollectionTools.copyInto(supported, dst);
	}
	
	public abstract M createResult();
	
	public abstract BulkResult<K, M> invoke
	(
		K[] results,
		Object[] params
	);
}
