package eu.javaexperience.rpc.api.doc;

public interface DocumentationApi
{
	public String[] getKeysOf(String key);
	public DocApiNode get(String path);
}
