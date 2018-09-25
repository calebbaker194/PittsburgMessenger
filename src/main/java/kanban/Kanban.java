package kanban;

import static spark.Spark.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.logging.Level;

import org.mindrot.jbcrypt.BCrypt;

import json.ConfigReader;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

public class Kanban 
{
	public static boolean routed;
	public static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Kanban.class.getName());	
	public HashMap<String, Integer> attempts = new HashMap<String, Integer>();
	Timer attemptCleaner = new Timer(true);
	
	
	public Kanban() {
		if(!routed)
		{
			routed=true;
			intiatePostgres();
			mapRoutes();
			intiateMemoryScrubber();
		}
	}

	private void intiateMemoryScrubber()
	{
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long tillMidnight = (c.getTimeInMillis()-System.currentTimeMillis());
		
		attemptCleaner.schedule(new TimerTask() {
			@Override
			public void run()
			{
				attempts.clear();			}
			}, tillMidnight, 864000000); //Runs every day at midnight
	}

	private void intiatePostgres()
	{
		try
		{
			InetAddress address=null;
			try {
				address = InetAddress.getByName("psh1");
			} catch (UnknownHostException e) {
				LOGGER.warning("Failed to parse host name correctly");
				try {
					address = InetAddress.getByName("192.168.2.3");
				} catch (UnknownHostException e1) {
					LOGGER.severe("Postgresql Host does not exists");
				}
			}
			String addr = address != null ? address.getHostAddress() : "192.168.2.3";
			Class.forName("org.postgresql.Driver");
			SQLEngine.SQLConnection("PittSteel2", addr, 5432, "caleb", "tori");
			LOGGER.info("Succesfully logged into the database server");
		} catch (ClassNotFoundException e)
		{
			LOGGER.log(java.util.logging.Level.SEVERE,"CANNOT LOAD POSTGRESQL DRIVERS "+e.toString(),e);
		}
	}
	public void mapRoutes(){

	get("/kanban" , (req,res) -> {
		User user = getUserFromCookie(req.cookie("ps-kanban-auth"));
		if(user != null)
		{
			Map<String, Object> model = new HashMap<String, Object>();
			return new VelocityTemplateEngine().render(
					
					new ModelAndView(model, "web/kanban/kanban.html")
			);
		}
		res.redirect("/kanban/login");
		return res;
	});
	
	get("/kanban/scan" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		String bundleId = req.queryParams("bundle_id");
		// LEGACY DEMO CODE
		if(bundleId == null)
			bundleId = req.queryParams("ps-kanban-bundle-id");
		//
		if(user != null)
		{
			if(scanBundleIn(bundleId,user.getId())) // This will Scan The Bundle In. And Return True on success
			{
				try
				{
					String itemId = getItemIdFromBundle(bundleId);
					if(itemId != null)
					{
						res.header("status", "success");
						res.redirect("/kanban/items/"+itemId+"?bundle_id="+bundleId);
					}
				}
				catch(Exception e) {LOGGER.log(Level.WARNING, "Unknown Scan Error", e);}
			}
			else
			{
				res.redirect("/kanban/items?scan=failed");
			}
			return res;
		}
		else if(bundleId != null)
		{
			res.redirect("/kanban/register?bundle_id="+bundleId);
		}
		else
		{
			res.redirect("/kanban/register");
		}
		return res;
	});
	get("/test", (req,res) -> {
		Map<String, Object> model = new HashMap<String, Object>();
		return new VelocityTemplateEngine().render(
				
		new ModelAndView(model, "web/kanban/layout.html")
		);
	});
	
	get("/kanban/items", (req,res) -> {
		
		Map<String, Object> model = new HashMap<String, Object>();
		return new VelocityTemplateEngine().render(
				
		new ModelAndView(model, "web/kanban/items.html")
		);
	});
	
	get("/kanban/items/:item" , (req,res) -> {
		String id = null;
		String bundleId = req.queryParams("bundle_id");
		id = req.params(":item");
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		if(user != null)
		{
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("item_id", id);
			model.put("item_number", SQLEngine.SFQ("SELECT item_number FROM item WHERE item_id="+id));
			if(bundleId != null)
				model.put("bundle_id", bundleId);
			else
				model.put("bundle_id","-2");
			return new VelocityTemplateEngine().render(
					
			new ModelAndView(model, "web/kanban/item.html")
			);
		}
		res.redirect("/kanban/regester");
		return res;
	});
	
	get("/kanban/register" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		if(user == null)
		{
			Map<String, Object> model = new HashMap<String, Object>();
			String bundleId = req.queryParamOrDefault("bundle_id","-1");
			model.put("registerd", false);
			model.put("bundle_id", bundleId);

			return new VelocityTemplateEngine().render(
					new ModelAndView(model, "web/kanban/register.html")
			);
		}
		res.redirect("/kanban");
		return res;
	});
	
	get("/kanban/login" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		String bundleId = req.queryParamOrDefault("bundle_id","-1");
		if(user == null )
		{
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("bundle_id",bundleId);
			model.put("registerd", true);
			return new VelocityTemplateEngine().render(
					
					new ModelAndView(model, "web/kanban/register.html")
			);
		}
		res.redirect("/kanban");
		return res;
	});
	
	post("/kanban/login/submit" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		res.type("application/json");
		
		if(attempts.get(req.ip())==null)
			attempts.put(req.ip(), 0);
		if(attempts.get(req.ip())>100)
			return "{\"excede\":true}";
		
		if(user == null)
		{
			User u = new User();
			u.setFname(req.queryParams("fname"));
			u.setLname(req.queryParams("lname"));
			u.setPassword(req.queryParams("password"));
			
			String response = login(u,req.queryParamOrDefault("kanbancust_id", "-1"));
			boolean success = response != null;
			String redir="";
			if(success)
			{	
				res.cookie(req.host(), "/", "ps-kanban-auth", response, 2592000, true,true);
				String bundle = req.queryParamOrDefault("bundle_id", "-1");
				if(bundle.equals("-1"))
				{
					redir = "/kanban";
				}
				else
				{
					redir = "/kanban/scan?bundle_id="+bundle;
				}
				return "{\"success\":true,\"\",\"redir\":"+redir+"}";
			}
			else
			{			
				attempts.put(req.ip(), attempts.get(req.ip())+1);
				
				return "{\"success\":false}";
			}
		}
		return "{\"user\":true}";
	});
	
	post("/kanban/register/submit" , (req,res) -> {
		String cust_id =req.queryParams("kanbancust_id");
		String cpassword = req.queryParams("kanbancust_password");
		String fname = req.queryParams("fname");
		String lname = req.queryParams("lname");
		String password = req.queryParams("password");
		String BundleId = req.queryParams("bundle_id");
		String companyPassword = getCompanyPassword(cust_id);
		System.out.println("Submitting Form. Password:"+cpassword);
		if(attempts.get(req.ip())==null)
			attempts.put(req.ip(), 0);
		if(attempts.get(req.ip())>100)
			return "{\"excede\":true}";
			
		if(companyPassword != null && companyPassword.equals(cpassword))
		{
			String redir="";
			User u = new User();
			u.setFname(fname);
			u.setLname(lname);
			u.setPassword(password);
			System.out.println(u);
			String cookie = registerUserToCompany(u,cust_id);
			
			if(cookie == null)
			{
				return "{\"success\":false , \"message\":\"There Is already a user with that name \"}";
			}
			if(BundleId != null)
			{
				redir="/kanban/scan?bundle_id="+BundleId;
			}
			else
			{
				redir="/kanban";
			}
			res.cookie(req.host(), "/", "ps-kanban-auth", cookie, 2592000, true,true);
			return "{\"success\":true,\"redir\":\""+redir+"\",\"cookie\":\""+cookie+"\"}";
		}
		attempts.put(req.ip(), attempts.get(req.ip())+1);
		return "{\"success\":false , \"message\":\"invalid\"}";
	});
	
	//////////////////////////////// JSON DATA CALLS /////////////////////////////////////////////
	get("/kanban/companies.json" , (req,res) -> {
		
		res.type("application/json");
		String query = "SELECT kanbancust_id AS id, " + 
				"shipto_name AS name " + 
				"FROM charass " + 
				"JOIN pittsteelcustom.kanbancust ON(charass_target_id = kanbancust_id) " + 
				"JOIN shiptoinfo ON(shipto_id = kanbancust_shipto_id) " + 
				"WHERE charass_target_type = 'KBN' " + 
				"AND charass_char_id = 31";
		
		ResultList r = SQLEngine.executeDBQuery(query);
		return ConfigReader.printObject(r);
	});
	
	get("/kanban/bundles.json" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		
		if(user != null)
		{
			res.type("application/json");
			String itemid = req.queryParams("item_id");
			String query="";
			if(itemid == null)
			{
				String bundleid = req.queryParams("bundle_id");
				if(bundleid == null)
				{
					return 1;
				}
				itemid = getItemIdFromBundle(bundleid);
			}
			
			query = "SELECT kanbanlineitem_id as id, "
					+ "kanbanlineitem_timeissued as created, "
					+ "kanbanlineitem_timecleared as cleared, "
					+ "kanbanlineitem_clearedby as clearedby "
					+ "FROM pittsteelcustom.kanbanlineitem "
					+ "LEFT JOIN pittsteelcustom.kanbanitem ON (kanbanlineitem_kanbanitem_id = kanbanitem_id) "
					+ "WHERE kanbanitem_item_id = "+itemid;
			
			ResultList r = SQLEngine.executeDBQuery(query);
			return ConfigReader.printObject(r);
		}
		return null;
	});
	
	get("/kanban/items.json" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		res.type("application/json");
		if(user != null)
		{
			String query = "SELECT kanbanitem_id, "
					+ "item_id, "
					+ "item_number, "
					+ "kanbanitem_number as cust_number, "
					+ "item_descrip1 AS description, "
					+ "(SELECT COUNT(kanbanlineitem_id) "
					+ "FROM pittsteelcustom.kanbanlineitem WHERE (kanbanlineitem_timecleared IS NULL) AND kanbanlineitem_kanbanitem_id = kanbanitem_id) "
					+ " AS instock, "
					+ "kanbanitem_stocklevel AS basestock, "
					+ "pittsteelcustom.countItemOnOrder(kanbanitem_id) AS onorder, "
					+ "pittsteelcustom.countItemOnConf(kanbanitem_id) AS onconf "
					+ "FROM pittsteelcustom.kanbanitem "
					+ "LEFT JOIN item ON(item_id = kanbanitem_item_id) "
					+ "LEFT JOIN pittsteelcustom.kanbancust ON (kanbanitem_kanbancust_id = kanbancust_id)"
					+ "WHERE kanbancust_id = pittsteelcustom.getKanbanCustFromUserId("+user.getId()+");";
			
			ResultList r = SQLEngine.executeDBQuery(query);
			return ConfigReader.printObject(r);
		}
		return null;
	});
	get("/kanban/quotes.json" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		res.type("application/json");
		if(user != null)
		{
			String query = "SELECT quhead_id, " + 
					"quhead_number, " + 
					"COALESCE( (SELECT sum(quitem_price) FROM quitem WHERE quitem_quhead_id = quhead_id),0 ) AS estematedCost " + 
					"FROM quhead " + 
					"WHERE quhead_shipto_id =  " + 
					"(SELECT kanbancust_shipto_id FROM pittsteelcustom.kanbancust  " + 
					"WHERE kanbancust_id=pittsteelcustom.getkanbancustfromuserid("+user.getId()+"));";
			
			ResultList r = SQLEngine.executeDBQuery(query);
			
			query = "SELECT quhead_id, \r\n" + 
					"quitem_id,\r\n" + 
					"kanbanitem_number AS cust_item_number,\r\n" + 
					"quitem_linenumber,\r\n" + 
					"item_number, \r\n" + 
					"item_descrip1 , \r\n" + 
					"quitem_price \r\n" + 
					"FROM quitem LEFT JOIN quhead ON (quitem_quhead_id = quhead_id) \r\n" + 
					"LEFT JOIN item ON (quitem_item_id = item_id)\r\n" + 
					"LEFT JOIN pittsteelcustom.kanbancust ON (kanbancust_shipto_id = quhead_shipto_id)\r\n" + 
					"LEFT JOIN pittsteelcustom.kanbanitem ON (kanbanitem_item_id = item_id AND kanbanitem_kanbancust_id = kanbancust_id)\r\n" + 
					"WHERE quhead_shipto_id = \r\n" + 
					"(SELECT kanbancust_shipto_id FROM pittsteelcustom.kanbancust \r\n" + 
					"WHERE kanbancust_id=pittsteelcustom.getkanbancustfromuserid("+user.getId()+")) \r\n" + 
					"ORDER BY quhead_id, quitem_linenumber;";
			
			r.addLevel(query,"quhead_id");
			return ConfigReader.printObject(r);
		}
		return null;
	});
	get("/kanban/employees.json" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		res.type("application/json");
		if(user != null)
		{
			String query = "SELECT cntct_first_name AS fname, cntct_last_name AS lname, "
					+ "(SELECT charass_value FROM charass AS c WHERE c.charass_target_id = "+user.getId()+" "
					+ "AND c.charass_char_id=29 AND c.charass_target_type='CNTCT') AS passwd FROM cntct  " 
					+ "LEFT JOIN charass ON (charass_target_id = cntct_id) "
					+ "LEFT JOIN pittsteelcustom.kanbancust ON (charass_value = kanbancust_id||'') " 
					+ "WHERE  " 
					+ "charass_char_id = 33 AND " 
					+ "kanbancust_id=pittsteelcustom.getkanbancustfromuserid("+user.getId()+");";
			
			ResultList r = SQLEngine.executeDBQuery(query);
			
			return ConfigReader.printObject(r);
		}
		return null;
	});
	//////////////////////////////////////////////////////////////////////////////////////////////
	
	post("/kanban/convert.json", (req,res) -> {
		String quhead_id = req.queryParams("quhead_id");
		String custpo = req.queryParams("custpo");
		res.type("application/json");
		if(quhead_id != null && custpo != null)
		{
			SQLEngine.executeDBQuery("UPDATE quhead SET quhead_custponumber="+custpo+" WHERE quhead_id="+quhead_id);
			ResultList r = SQLEngine.executeDBQuery("SELECT convertquote("+quhead_id+") AS cohead_id");
			//TODO: Possibly allow them to schedule there reports using xtconnect
			if(r.get("error") == null)
				return "{\"success\":true}";
			
		}
		return "{\"success\":false}";
	});
	
	post("/kanban/move-quote" , (req,res) -> {
		res.type("application/json");
		String quitem_id = req.queryParamOrDefault("quitem_id", "-1");
		String quhead_id = req.queryParams("quhead_id");
		Boolean success = false;
		if(quhead_id == null || quhead_id == "-1")
		{
			success = (Boolean) SQLEngine.SFQ("SELECT pittsteelcustom.movequitem("+quitem_id+") AS id");
		}
		else
		{
			success =  (Boolean) SQLEngine.SFQ("SELECT pittsteelcustom.movequitem("+quitem_id+","+quhead_id+") AS id");
		}
		return "{\"success\":"+success+"}";
	});
	///////////////////////////////// CONTROLER //////////////////////////////////////////////////
	}
	/**
	 * Gets the item id that a specific bundle is related to
	 * @param Bundle: The bundle_id to link to the item_id
	 */
	private static String getItemIdFromBundle(String bundleId)
	{
		ResultList r = SQLEngine.executeDBQuery("SELECT kanbanitem_item_id AS item_id "
				+ "FROM pittsteelcustom.kanbanlineitem "
				+ "LEFT JOIN pittsteelcustom.kanbanitem ON (kanbanlineitem_kanbanitem_id = kanbanitem_id) "
				+ "WHERE kanbanlineitem_id = "+bundleId);
		return ""+r.get("item_id");
	}
	/**
	 * This scans the bundle in and opens a quote, or adds the bundle to a quote.
	 * @param bundleId : the bundle that is being scanned
	 * @param cntct_id : the person that is scanning the bundle
	 * @return True: if the bundle scans False: if the bundle does not acan
	 */
	private static boolean scanBundleIn(String bundleId, long cntct_id)
	{
		if (getItemIdFromBundle(bundleId) == null)
		{
			return false;
		}
		else 
		{
			SQLEngine.executeDBQuery("SELECT pittsteelcustom.scanBundleIn("+bundleId+","+cntct_id+")");
			return true;
		}
	}
	
	/**
	 * Register a User to a company. 
	 * @param user : A user that is not registered to a company
	 * @param cust_id : The kanbancust_id
	 * @return The UUID generated by the method for he customer
	 */
	private static String registerUserToCompany(User user, String cust_id)
	{
		String cookieToReturn = null;
		String uniqueID = UUID.randomUUID().toString();
		cookieToReturn = (String) SQLEngine.SFQ("SELECT pittsteelcustom.regesterKanbanUser('"+user.getFname()+"','"+user.getLname()+"','"+user.getPassword()+"',"+cust_id+",'"+uniqueID+"');");
		
		return cookieToReturn;
	}
	
	/**
	 * gets the kanban program password for a specific customer
	 * @param cust_id The customer's ID in the database; 
	 */
	private static String getCompanyPassword(String cust_id)
	{
		
		ResultList r = SQLEngine.executeDBQuery("SELECT charass_value AS passwd "
				+ "FROM charass "
				+ "WHERE charass_target_type = 'KBN' "
				+ "AND charass_target_id = "+cust_id);
		System.out.println(cust_id+" Password Is: "+r.get("passwd"));
		return (String) r.get("passwd");
	}
	
	
	/**
	 * Gets the user associated with the cookie
	 * @param cookie : the cookie to associate the user
	 * @return the User associated with the cookie
	 */
	public static User getUserFromCookie(String cookie)
	{
		ResultList r = SQLEngine.executeDBQuery("SELECT " + 
				"cntct_id AS id, " + 
				"cntct_first_name AS fname, " + 
				"cntct_last_name AS lname " + 
				"FROM charass " + 
				"JOIN cntct ON (charass_target_id = cntct_id) " + 
				"WHERE charass_target_type = 'CNTCT' " + 
				"AND charass_char_id = 32 " + 
				"AND charass_value = '"+cookie+"'");
		
		User temp = new User();
		if(r.first())
		{
			temp.setFname((String) r.get("fname"));
			temp.setLname((String) r.get("lname"));
			temp.setId(0l+(Integer)r.get("id"));
			
			return temp;
		}
		return null;
	}
	
	/**
	 * Check the password and returns the cookie for the user if it matches.
	 * @param u : The "User" including the first name, last name, and password 
	 * @param kbc_id : the Kanbancust_id so that users with the same first and last name at different companies. don't cause problems
	 * @return The cookie for the user or null, if the password doesnt match or the user doesnt exists
	 */
	private static String login(User u, String kbc_id)
	{
		BCrypt.checkpw(u.getPassword(),BCrypt.hashpw(u.getPassword(),"$2a$12$z88w6I4kRI42Xa0wsHDVHO")); // Does nothing but slow down the verification. 
		
		ResultList r = SQLEngine.executeDBQuery("SELECT pittsteelcustom.getCookieFromUser('"+u.getFname()+"','"+u.getLname()+"','"+u.getPassword()+"',"+kbc_id+") AS cookie");
		if(r!=null && r.first()) {
			return  (r.get("cookie")+"").equals("-1") ? null: r.get("cookie")+"";
		}
		else
			return null;
	}
}

