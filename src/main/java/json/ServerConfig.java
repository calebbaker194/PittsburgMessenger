package json;

import java.util.ArrayList;

import email.ImapServer;

public class ServerConfig {
	
	private ArrayList<ImapServer> mail_servers = new ArrayList<ImapServer>();
	
	public ArrayList<ImapServer> getServers()
	{
		return mail_servers;
	}

	public void setServers(ArrayList<ImapServer> servers)
	{
		this.mail_servers = servers;
	}
}
