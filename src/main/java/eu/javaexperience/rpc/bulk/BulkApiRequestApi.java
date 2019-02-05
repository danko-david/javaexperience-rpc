package eu.javaexperience.rpc.bulk;

import eu.javaexperience.datareprez.DataArray;
import eu.javaexperience.datareprez.DataObject;

public abstract class BulkApiRequestApi
{
	public abstract DataObject handleSingleRequest(DataObject obj);
	
	public DataObject doMulticall(DataObject req)
	{
		DataObject ret = req.newObjectInstance();
		
		Long t = req.optLong("t");
		if(null != t)
		{
			req.putLong("t", t);
		}
		
		DataArray reqs =  req.getArray("p");
		DataArray r = req.newArrayInstance();
		for(int i=0;i<reqs.size();++i)
		{
			DataObject res = handleSingleRequest(reqs.getObject(i));
			r.putObject(res);
		}
		
		ret.putArray("r", r);
		
		return ret;
	}
}
