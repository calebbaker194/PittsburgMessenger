package email;

import java.io.Serializable;

public class ImapServer implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = -8812497077186759604L;
	private boolean starttls = false;
	private String host;
	private String username;
	private String address;
	private String password;
	private int port;
	private boolean auth;
	private String commonName;
	public ImapServer(boolean starttls,String host,String username,String address,String commonName,String password,int port,boolean auth)
	{
		setStarttls(starttls);
		setHost(host);
		setUsername(username);
		setAddress(address);
		setPassword(password);
		setPort(port);
		setAuth(auth);
		setCommonName(commonName);
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
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
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
	public int getPort() {
		return (port);
	}
	public void setPort(int port) {
		this.port = port;
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
}
