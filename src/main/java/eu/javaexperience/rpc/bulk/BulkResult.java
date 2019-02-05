package eu.javaexperience.rpc.bulk;

import java.util.ArrayList;
import java.util.List;

import eu.javaexperience.rpc.bulk.BulkOperation.RequestedFields;

public class BulkResult<K, R>
{
	protected RequestedFields<K, R> fields;
	protected List<R> results = new ArrayList<>();
	
}
