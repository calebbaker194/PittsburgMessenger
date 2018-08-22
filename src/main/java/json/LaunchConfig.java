package json;

public class LaunchConfig {
	private String certPassword;
	private int port;
	private String certPath;
	private String webUser;
	private String webPassword;
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
	public String getWebUser()
	{
		return webUser;
	}
	public void setWebUser(String webUser)
	{
		this.webUser = webUser;
	}
	public String getWebPassword()
	{
		return webPassword;
	}
	public void setWebPassword(String webPassword)
	{
		this.webPassword = webPassword;
	}
	public String getCertPassword()
	{
		return certPassword;
	}
	public void setCertPassword(String certPassword)
	{
		this.certPassword = certPassword;
	}
}
