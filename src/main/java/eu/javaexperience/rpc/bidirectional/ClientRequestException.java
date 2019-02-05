package eu.javaexperience.rpc.bidirectional;

import eu.javaexperience.text.StringTools;

public class ClientRequestException extends RuntimeException
{
	private static final long serialVersionUID = 1L;
	
	protected final Object o;
	
	public ClientRequestException(Object o)
	{
		super(StringTools.toString(o));
		this.o = o;
	}
	
	public Object getObject()
	{
		return o;
	}
}
