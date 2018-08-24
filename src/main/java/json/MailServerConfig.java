package json;

import java.util.ArrayList;

import email.ImapServer;

public class MailServerConfig {
 
	private ArrayList<ImapServer> defaultServer = new ArrayList<ImapServer>();

	public ArrayList<ImapServer> getDefaultServer()
	{
		return defaultServer;
	}

	public void setDefaultServer(ArrayList<ImapServer> defaultserver)
	{
		this.defaultServer = defaultserver;
	}
	
}
