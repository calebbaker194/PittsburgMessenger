package kanban;

public class User {
	private long id;
	private String fname;
	private String lname;
	private String password;
	public long getId()
	{
		return id;
	}
	public void setId(long id)
	{
		this.id = id;
	}
	public String getFname()
	{
		return fname;
	}
	public void setFname(String fname)
	{
		this.fname = fname;
	}
	public String getPassword()
	{
		return password;
	}
	public void setPassword(String password)
	{
		this.password = password;
	}
	public String getLname()
	{
		return lname;
	}
	public void setLname(String lname)
	{
		this.lname = lname;
	}
}
