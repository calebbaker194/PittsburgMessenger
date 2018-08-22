package json;

import java.util.ArrayList;

import email.ImapServer;

public class MailServerConfig {
	
	private ArrayList<ImapServer> mailservers = new ArrayList<ImapServer>();

	public ArrayList<ImapServer> getMailservers()
	{
		return mailservers;
	}

	public void setMailservers(ArrayList<ImapServer> mailservers)
	{
		this.mailservers = mailservers;
	}
	
}
