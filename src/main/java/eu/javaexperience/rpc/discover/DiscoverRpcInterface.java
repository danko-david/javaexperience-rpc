package eu.javaexperience.rpc.discover;

import java.util.Map;

public interface DiscoverRpcInterface
{
	public String[] getNamespaces();
	public String help();
	public String source(String language, String namespace, Map<String, String> params);
	public boolean ping();
}
