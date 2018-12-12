package json;

import java.util.ArrayList;

public class ParseMessage implements Comparable<ParseMessage>{
	private long dateSent;
	private String contact;
	private String id;
	private String body;
	private ArrayList<String> mediaLinks;
	private String status;
	
	public ParseMessage() {
		
	}
	public ParseMessage (String cntc, String body, Long sent, String status) {
		setContact(cntc);
		setBody(body);
		setDateSent(sent);
		setStatus(status);
	}
	public ParseMessage (String cntc, String body, Long sent, String status,String id) {
		setContact(cntc);
		setBody(body);
		setDateSent(sent);
		setStatus(status);
		setId(id);
	}
	public ParseMessage(String cntc, String body, Long sent, ArrayList<String> tmp, String status)
	{
		setContact(cntc);
		setBody(body);
		setDateSent(sent);
		setMediaLinks(tmp);
		setStatus(status);
	}
	public ParseMessage(String cntc, String body, Long sent, ArrayList<String> tmp, String status,String id)
	{
		setContact(cntc);
		setBody(body);
		setDateSent(sent);
		setMediaLinks(tmp);
		setStatus(status);
		setId(id);
	}
	public String getBody()
	{
		return body;
	}
	public void setBody(String body)
	{
		this.body = body;
	}
	public String getContact()
	{
		return contact;
	}
	public void setContact(String contact)
	{
		this.contact = contact;
	}
	public long getDateSent()
	{
		return dateSent;
	}
	public void setDateSent(long l)
	{
		this.dateSent = l;
	}
	
	public String toString()
	{
		return getContact();
	}
	
	
	@Override
	public int hashCode() {
		return getContact().hashCode();
	}
	
	@Override
	public boolean equals(Object e) {
		if(!(e instanceof ParseMessage))
		{
			return false;
		}
		
		return ((ParseMessage) e).getContact().equals(getContact());
	}
	
	public int compareTo(ParseMessage t)
	{
		if(getDateSent() > t.getDateSent())
			return 1;
		if(getDateSent() < t.getDateSent())
			return -1;	
		return 0;
	}
	public ArrayList<String> getMediaLinks()
	{
		return mediaLinks;
	}
	public void setMediaLinks(ArrayList<String> mediaLink)
	{
		this.mediaLinks = mediaLink;
	}
	public String getStatus()
	{
		return status;
	}
	public void setStatus(String status)
	{
		this.status = status;
	}
	public String getId()
	{
		return id;
	}
	public void setId(String id)
	{
		this.id = id;
	}
}
