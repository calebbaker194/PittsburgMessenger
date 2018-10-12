package launch;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ObjectNode;
import json.ConfigReader;
import json.LaunchConfig;
import kanban.Kanban;
import kanban.User;
import server.Mapper;
import server.Server;
import servers.DriveServer;
import servers.MailEngine;
import servers.SmsServer;
import spark.Filter;
import spark.ModelAndView;
import spark.Request;
import spark.Response;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;

import static spark.Spark.*;


public class Main{
	
	public MailEngine me;
	public SmsServer sms;
	public DriveServer dr;
	
	private String uname="";
	private String passwd="";
 
	public Server[] serverList= {me,sms,dr};
	public HashMap<String, String> tokens = new HashMap<String, String>();
	private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
	
    private static final HashMap<String, String> corsHeaders = new HashMap<String, String>();
	
    static {
        corsHeaders.put("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
        corsHeaders.put("Access-Control-Allow-Origin", "*");
        corsHeaders.put("Access-Control-Allow-Headers", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin,");
        corsHeaders.put("Access-Control-Allow-Credentials", "true");
    }
	
    public final static void apply() {
        Filter filter = new Filter() {
            @Override
            public void handle(Request request, Response response) throws Exception {
                corsHeaders.forEach((key, value) -> {
                    response.header(key, value);
                });
            }
        };
        Spark.after(filter);
    }
    
    
	public Main()
	{
		LaunchConfig a = new LaunchConfig();
//		
		a = ConfigReader.ReadConf(a.getClass(), "config/main.conf");
//		
		uname = a.getWebUser();
		passwd = a.getWebPassword();
//		
		staticFiles.externalLocation("webresources");	
//		
//		//Start Mail Server
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
		
		apply();
		
		//Start Mail Server
		me = MailEngine.getInstance();
		
		//Start sms server
		sms = SmsServer.getInstance();
		
		
		
		//Intiate Drive
		dr = DriveServer.getInstance();
		
		serverList = new Server[] {me,sms,dr};
		InitMonitor();
		Kanban k = new Kanban();
		
		me.regesterRequiredServers();
		sms.regesterRequiredServers();
		dr.regesterRequiredServers();
		
		Mapper.loadMap(Mapper.DEFAUTL_MAP_LOCATION);
		
		
	}

	private void InitMonitor()
	{
		get("/" , (req,res) -> {
			
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
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
			User user = Kanban.getUserFromCookie(req.cookie("ps-kanban-auth"));
			if(user == null)
			{
				res.redirect("/login");
				return res;
			}
			else
			{
				res.redirect("/kanban");
				return res;
			}
		});
		
		get("/config.json" , (req,res) -> {
		
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					HashMap<String,ObjectNode> nodes = new HashMap<String,ObjectNode>();
					
					for(Server s:serverList)
					{
						if(s != null) 
						{
							ObjectNode t = s.getConfig();
							nodes.put(t.fieldNames().next(),t);
						}
					}
					ObjectNode t = Mapper.getConfig();
					nodes.put("mapper", t);
					
					ObjectMapper m = new ObjectMapper();
					ObjectNode all=m.createObjectNode();
					
					all.setAll(nodes);
					
					ObjectWriter writer = m.writer();
					
					String ret="";
					try
					{
						ret=writer.writeValueAsString(all);
					} catch (JsonProcessingException e)
					{
						LOGGER.log(Level.WARNING,"Failure Reading Log Files",e);
					}
					return ret;
				}
			}
			res.redirect("/login");
			return res;
		});
		
		post("/config.json" , (req,res) -> {		
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}	
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
				    ObjectMapper mapper = new ObjectMapper();
				    JsonNode jsonConfig = mapper.readTree(req.queryParams("data"));
					
				    JsonNode mailServer = jsonConfig.get("MailServer");
				    JsonNode smsServer = jsonConfig.get("SmsServer");
				    JsonNode map = jsonConfig.get("mapper");
				    
				    if(me != null) {
				    me.setConfig(mailServer);
				    }
				    if(sms != null) {
				    sms.setConfig(smsServer);
				    }
				    Mapper.setConfig(map);
					
					res.type("application/json");
					return "{\"success\":true}";
				}
			}
			res.redirect("/login");
			return res;
		});
		
		get("/configure", (req,res) -> {
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
			
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					Map<String, Object> model = new HashMap<String, Object>();
					return new VelocityTemplateEngine().render(
							
							new ModelAndView(model, "web/configure.html")
					);
				}
			}
			res.redirect("/login");
			return res;
		});
		
		get("/server-test", (req,res) -> {
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					Map<String, Object> model = new HashMap<String, Object>();					
					return new VelocityTemplateEngine().render(
							new ModelAndView(model, "web/server-test.html")
					); 
				}
			}
					
			res.redirect("/login");
			return res;
		});
		
		get("/logout", (req,res) -> {
			
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
			
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					if(req.session(false) != null )
						req.session(false).removeAttribute("username");
					res.redirect("/login");
					return res;
				}
			}
			res.redirect("/login");
			return res;
		});
		
		post("/login", (req,res) -> {
			
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
			
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
			
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
			
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
				res.redirect("/");
				return res;
				}
			}
			
			Map<String, Object> model = new HashMap<String, Object>();
			return new VelocityTemplateEngine().render(
					
					new ModelAndView(model, "web/login.html")
			);
		});
		
		get("/admin/config", (req, res) -> {
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					Map<String, Object> model = new HashMap<String, Object>();					
					return new VelocityTemplateEngine().render(
							new ModelAndView(model, "web/admin.html")
					); 
				}
			}
			res.redirect("/login");
			return res;
		});
		
		get("/monitor", (req, res) -> {
			
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
			
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					Map<String, Object> model = new HashMap<String, Object>();					
					return new VelocityTemplateEngine().render(
							new ModelAndView(model, "web/monitor.html")
					); 
				}
			}
			res.redirect("/login");
			return res;
		});
		
		post("/monitor/servers.json", (req, res) -> {
			res.type("application/json");
			ObjectMapper mapper = new ObjectMapper();
			return mapper.writeValueAsString(getServers());
		});
		
		get("/monitor/chartdata.json", (req, res) -> {
			res.type("application/json");
			
			ObjectMapper o = new ObjectMapper();
			ObjectNode data = o.createObjectNode();
			
			data.set(me.getName(), me.getStats());
			data.set(sms.getName(), sms.getStats());
			data.set(dr.getName(), dr.getStats());
			
			return o.writer().writeValueAsString(data);
		});
		
		post("/restart",(req, res) -> {
			
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					String SName = req.queryParams("index");
					LOGGER.info("SERVER "+ SName +" Is Restarting");
					for(Server s : serverList)
					{
						if(s != null && s.getName().equals(SName))
							s.restart();
					}
					serverList = new Server[] {me,sms,dr};
					return true;
				}
			}
			res.redirect("/login");
			return false;
		});
		post("/stop",(req, res) -> {
			
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					String SName = req.queryParams("index");
					LOGGER.info("SERVER "+ SName +" Is Stoping");
					for(Server s : serverList)
					{
						if(s != null && s.getName().equals(SName))
							s.stop();
					}
					serverList = new Server[] {me,sms,dr};
					return true;
				}
			}
			res.redirect("/login");
			return false;
		});
		post("/start",(req, res) -> {
			if(req.host() == "kanban.pittsburgsteel.com") {
				res.redirect("/kanban");
				return res;
			}
			if(req.session(false) != null)
			{
				if(req.session(false).attribute("username") != null && req.session(false).attribute("username").equals("thewonderfullhint"))
				{
					String SName = req.queryParams("index");
					LOGGER.info("SERVER "+ SName +" Is Starting");
					for(Server s : serverList)
					{
						if(s != null && s.getName().equals(SName))
							s.start();
					}
					serverList = new Server[] {me,sms,dr};
					return true;
				}
			}
			res.redirect("/login");
			return false;
		});
		
		post("/getstats", (req,res) -> {
			res.type("application/json");
			String serverName = req.queryParams("server");
			if(serverName != null) 
			{
				
			}
			
			return "";
		});
	}
	

	private boolean login(String username, String password)
	{
		return (username.equals(uname) && password.equals(passwd));
	}

	private ArrayList<HashMap<String, Object>> getServers()
	{
		ArrayList<HashMap<String, Object>> a = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> map;
		for( Server s: serverList)
		{
			if(s!= null)
			{
				map = new HashMap<String, Object>(); 
				map.put("name", s.getName());
				map.put("status",s.isRunning()+"");
				map.put("startTime", s.getStartTime());
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
	@Test
	public static void main(String args[])throws IOException, GeneralSecurityException
	{	
		Main a = new Main(); 
		
	}

}
