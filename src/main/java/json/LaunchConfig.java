package json;

import java.util.HashMap;

public class LaunchConfig {
	private String certPassword;
	private int port;
	private String certPath;
	private HashMap<String, String> admin;
	private int ipLimit = 30;
	private boolean allowRemote = false;
	
	public int getPort()
	{
		return port;
	}
	public void setPort(int port)
	{
		this.port = port;
	}
	public String getCertPath()
	{
		return certPath;
	}
	public void setCertPath(String certPath)
	{
		this.certPath = certPath;
	}
	public String getCertPassword()
	{
		return certPassword;
	}
	public void setCertPassword(String certPassword)
	{
		this.certPassword = certPassword;
	}
	public HashMap<String, String> getAdmin()
	{
		return admin;
	}
	public void setAdmin(HashMap<String, String> admin)
	{
		this.admin = admin;
	}
	public int getIpLimit()
	{
		return ipLimit;
	}
	public void setIpLimit(int ipLimit)
	{
		this.ipLimit = ipLimit;
	}
	public boolean isAllowRemote()
	{
		return allowRemote;
	}
	public void setAllowRemote(boolean allowRemote)
	{
		this.allowRemote = allowRemote;
	}
}
