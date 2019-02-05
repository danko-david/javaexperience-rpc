package eu.javaexperience.rpc.api.doc;

public class DocApiNode
{
	public final String key;
	public final boolean isLeaf;
	public final Object content;
	
	public DocApiNode(String key, boolean isLeaf, Object content)
	{
		this.key = key;
		this.isLeaf = isLeaf;
		this.content = content;
	}
	
	@Override
	public String toString()
	{
		return "DocApiNode["+key+"]"+(isLeaf?"$":"...")+": "+content;
	}
}
