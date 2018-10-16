package launch;
import java.util.HashMap;
import java.util.Map;

import spark.ModelAndView;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

public class Admin extends HashMap<String, String>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 316189395497593979L;
	private static Admin instance = null;
	private int ipLimit = 30;
	private boolean allowRemote = false;
	
	private Admin() {
		super();
		
		Spark.get("/admin", (req, res) -> {
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					Map<String, Object> model = new HashMap<String, Object>();
					Object[] vals = keySet().toArray(); 
					model.put("username", vals[0]);
					model.put("password", get(vals[0]));
					model.put("remote", allowRemote?"checked":"");
					model.put("iplimit", ipLimit);
					return new VelocityTemplateEngine().render(
							new ModelAndView(model, "web/admin.html")
					); 
				}
			}
			res.redirect("/login");
			return res;
		});
		
		Spark.post("/admin", (req,res) -> {
			
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{

					String uname = req.queryParams("username");
					String passwd = req.queryParams("password");
					String ipLimit = req.queryParams("iplimit");
					String remote = req.queryParams("remote");

					if(!(uname == null || uname.equals("") || passwd == null || passwd.equals("")))
					{
						if(containsKey(uname))
						{
							put(uname,passwd);
						}
					}
					if(!(ipLimit == null || ipLimit.equals("")))
					{
						setIpLimit(Integer.parseInt(ipLimit));
					}
					if(!(remote == null || remote.equals("")))
					{
						setAllowRemote(remote.equals("on"));
					}
					else
					{
						setAllowRemote(false);
					}
					// Write Config
					Main.writeConfig();
					
					res.redirect("/monitor");
					return res;
				}
			}
			return null;
		});
	}
	public static void setInstance(Admin a)
	{
		instance = a;
	}
	public static Admin getInstance()
	{
		if(instance == null)
		{
			instance = new Admin();
		}
		return instance;
	}
	
	public boolean login(String uname, String passwd)
	{
		return get(uname) != null && get(uname).equals(passwd);
	}
	public void setMap(HashMap<String, String> admin)
	{
		for(String s : admin.keySet())
		{
			put(s, admin.get(s));
		}
	}
	public void setAllowRemote(boolean a)
	{
		allowRemote = a;
	}
	public boolean getAllowRemote()
	{
		return allowRemote;
	}
	public void setIpLimit(int i)
	{
		ipLimit = i;
	}
	public int getIpLimit()
	{
		return ipLimit;
	}
}
