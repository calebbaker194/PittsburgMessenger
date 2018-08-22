package launch;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import json.LaunchConfig;
import server.ConfigReader;
import server.Mapper;
import server.Server;
import servers.DriveServer;
import servers.MailEngine;
import servers.SmsServer;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

import static spark.Spark.*;


public class Main{
	
	public MailEngine me;
	public SmsServer sms;
	public DriveServer dr;
	
	private String uname="";
	private String passwd="";
	
	public Server[] serverList= {me,sms,dr};
	
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	
	public Main()
	{
		
		LaunchConfig a = new LaunchConfig();
		
		a = ConfigReader.ReadConf(a.getClass(), "config/main.conf");
		
		uname = a.getWebUser();
		passwd = a.getWebPassword();
		
		
		staticFiles.externalLocation("webresources");		
		//Start Mail Server
		//Start sms server
		try
		{
			port(a.getPort());
		} catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, e.toString(), e);
		}

		// Add Certificate for the web server. 
		secure(a.getCertPath(), a.getCertPassword(),
				null, null, false);
		
		//Start Mail Server
		me = MailEngine.getInstance();
		
		//Start sms server
		sms = SmsServer.getInstance();
		
		//Intiate Drive
		dr = DriveServer.getInstance();
		
		serverList = new Server[] {me,sms,dr};
		InitMonitor();
		
		me.regesterRequiredServers();
		sms.regesterRequiredServers();
		dr.regesterRequiredServers();
		
		Mapper.loadMap(Mapper.DEFAUTL_MAP_LOCATION);
		me.save();
	}

	private void InitMonitor()
	{
		get("/" , (req,res) -> {
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					Map<String, Object> model = new HashMap<String, Object>();
					return new VelocityTemplateEngine().render(
							
							new ModelAndView(model, "web/index.html")
					);
				}
			}

				res.redirect("/login");
				return res;
		});
		
		get("/logout", (req,res) -> {
			if(req.session(false) != null )
				req.session(false).removeAttribute("username");
			res.redirect("/login");
			return res;
		});
		
		post("/login", (req,res) -> {
			String username = req.queryParams("username");
			String password = req.queryParams("password");
			
			boolean match = login(username,password);
			if(match)
			{
				req.session();
				req.session().attribute("username", "thewonderfullhint");
				res.redirect("/");

			}
			else
			{
				res.redirect("/login");
			}
			return res;
			
		});
		
		get("/login" , (req,res) -> {
			Map<String, Object> model = new HashMap<String, Object>();
			return new VelocityTemplateEngine().render(
					
					new ModelAndView(model, "web/login.html")
			);
		});
		
		get("/monitor", (req, res) -> {
			Map<String, Object> model = new HashMap<String, Object>();
			
			model.put("serverList", getServers());
			
			return new VelocityTemplateEngine().render(
					new ModelAndView(model, "web/monitor.html")
			); 
		});
		
		post("/restart",(req, res) -> {
			int tindex=Integer.parseInt(req.queryParams("index"));
			LOGGER.info("SERVER "+ tindex +" Is Restarting");
			serverList[tindex].restart();
			serverList = new Server[] {me,sms,dr};
			return true;
		});
		post("/stop",(req, res) -> {
			int tindex=Integer.parseInt(req.queryParams("index"));
			LOGGER.info("SERVER "+ tindex +" Has Stopped");
			serverList[tindex].stop();
			serverList = new Server[] {me,sms,dr};
			return true;
		});
		post("/start",(req, res) -> {
			int tindex=Integer.parseInt(req.queryParams("index"));
			LOGGER.info("SERVER "+ tindex +" Has Started");
			serverList[tindex].start();
			serverList = new Server[] {me,sms,dr};
			return true;
		});
	}
	

	private boolean login(String username, String password)
	{
		return (username.equals(uname) && password.equals(passwd));
	}

	private ArrayList<HashMap<String, String>> getServers()
	{
		ArrayList<HashMap<String, String>> a = new ArrayList<HashMap<String, String>>();
		HashMap<String, String> map;
		for( Server s: serverList)
		{
			if(s!= null)
			{
				map = new HashMap<String, String>(); 
				map.put("name", s.getName());
				map.put("status",s.isRunning()+"");
				map.put("startTime", System.currentTimeMillis()+"");
				map.put("lastErrorDate",s.getLastErrorDate());
				map.put("lastError",s.getLastError());
				a.add(map);
			}
		}
		return a;
	}

	public void reload()
	{
		for(Server s : serverList)
		{
			s.stop();
		}
		me = MailEngine.getInstance();
		
		//Start sms server
		sms = SmsServer.getInstance();
		
		//Intiate Drive
		dr = DriveServer.getInstance();
		
		serverList = new Server[] {me,sms,dr};
		
		me.reload();
		sms.reload();
		dr.reload();
		
		me.regesterRequiredServers();
		sms.regesterRequiredServers();
		dr.regesterRequiredServers();
		
	}
	
	public static void main(String args[])throws IOException, GeneralSecurityException
	{	
		new Main(); 
		
	}

}
