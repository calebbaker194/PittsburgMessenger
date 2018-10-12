package email;

import java.io.Serializable;

public class ImapServer implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8812497077186759604L;
	private boolean starttls;
	private String imapHost;
	private String smtpHost;
	private String username;
	private String address;
	private String password;
	private int imapPort;
	private int smtpPort;
	private boolean auth;
	private String commonName;
	private String smtpProtocol;
	private String imapProtocol;
	
	public ImapServer(boolean starttls, String imapHost, int imapPort, String smtpHost, int smtpPort, String imapProto, String smtpProto ,String username,String address,String commonName,String password ,boolean auth)
	{
		setStarttls(starttls);
		setImapHost(imapHost);
		setSmtpHost(smtpHost);
		setImapPort(imapPort);
		setSmtpPort(smtpPort);;
		setUsername(username);
		setAddress(address);
		setPassword(password);
		setAuth(auth);
		setCommonName(commonName);
		setImapProtocol(imapProto);
		setSmtpProtocol(smtpProto);
	}
	public ImapServer()
	{
		
	}
	public boolean getStarttls() {
		return starttls;
	}
	public void setStarttls(boolean starttls) {
			this.starttls=starttls;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public boolean getAuth() {
		return auth;
	}
	public void setAuth(boolean auth) {
		this.auth=auth;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public String getCommonName() {
		return commonName;
	}
	public void setCommonName(String commonName) {
		this.commonName = commonName;
	}
	public String toString()
	{
		String s = "";
		s+="host: "+imapHost+"\n";
		s+="address: "+address+"\n";
		s+="username: "+username+"\n";
		s+="password: "+password+"\n";
		s+="auth: "+auth+"\n";
		s+="starttls: "+(starttls?"true":"false")+"\n";
		return s;
	}
	public String getImapHost()
	{
		return imapHost;
	}
	public void setImapHost(String imapHost)
	{
		this.imapHost = imapHost;
	}
	public String getSmtpHost()
	{
		return smtpHost;
	}
	public void setSmtpHost(String smtpHost)
	{
		this.smtpHost = smtpHost;
	}
	public int getImapPort()
	{
		return imapPort;
	}
	public void setImapPort(int imapPort)
	{
		this.imapPort = imapPort;
	}
	public int getSmtpPort()
	{
		return smtpPort;
	}
	public void setSmtpPort(int smtpPort)
	{
		this.smtpPort = smtpPort;
	}
	public String getSmtpProtocol()
	{
		return smtpProtocol;
	}
	public void setSmtpProtocol(String smtpProtocol)
	{
		this.smtpProtocol = smtpProtocol;
	}
	public String getImapProtocol()
	{
		return imapProtocol;
	}
	public void setImapProtocol(String imapProtocol)
	{
		this.imapProtocol = imapProtocol;
	}
}
