package kanban;

import static spark.Spark.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import json.ConfigReader;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

public class Kanban 
{
	public static boolean routed;
	public static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Kanban.class.getName());
	
	public Kanban() {
		if(!routed)
		{
			routed=true;
			intiatePostgres();
			mapRoutes();
		}
	}
	private void intiatePostgres()
	{
		try
		{
			Class.forName("org.postgresql.Driver");
			SQLEngine.SQLConnection("PittSteel2", "psh1", 5432, "caleb", "tori");
		} catch (ClassNotFoundException e)
		{
			LOGGER.log(java.util.logging.Level.SEVERE,"CANNOT LOAD POSTGRESQL DRIVERS "+e.toString(),e);
		}
	}
	public static void mapRoutes(){

	get("/kanban" , (req,res) -> {
		User user = getUserFromCookie(req.cookie("ps-kanban-auth"));
		if(user != null)
		{
			Map<String, Object> model = new HashMap<String, Object>();
			return new VelocityTemplateEngine().render(
					
					new ModelAndView(model, "web/kanban/kanban.html")
			);
		}
		res.redirect("/kanban/register");
		return res;
	});
	
	get("/kanban/scan" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		String bundleId = req.queryParams("ps-kanban-bundle-id");
		if(user != null)
		{
			if(scanBundleIn(bundleId,user.getId())) // This will Scan The Bundle In. And Return True on success
			{
				try
				{
					String itemId = getItemIdFromBundle(bundleId);
					if(itemId != null)
					{
						res.removeCookie("ps-kanban-bundle-id");
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
			res.redirect("/kanban/login?bundle_id="+bundleId);
		}
		else
		{
			res.redirect("/kanban/login");
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
		res.redirect("/kanban/login");
		return res;
	});
	
	get("/kanban/register" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		if(user == null)
		{
			Map<String, Object> model = new HashMap<String, Object>();
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
		String bundleId = req.queryParams("ps-kanban-bundle-id");
		if(user == null )
		{
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("bundleId",bundleId);
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
		if(user == null)
		{
			User u = new User();
			u.setFname(req.queryParams("fname"));
			u.setLname(req.queryParams("lname"));
			u.setPassword(req.queryParams("password"));
			
			String response = login(u,req.queryParamOrDefault("kanbancust_id", "-1"));
			boolean success = response != null;
			if(success)
			{	
				res.cookie(req.host(), "/", "ps-kanban-auth", response, -1, true,true);
				String bundle = req.queryParamOrDefault("ps-kanban-bundle-id", "-1");
				if(bundle.equals("-1"))
				{
					res.redirect("/kanban/scan?ps-kanban-bundle-id="+bundle);
				}
				else
				{
					res.redirect("/kanban");
				}
			}
			else
			{
				res.redirect("/kanban/login?failed=failed");;
			}
			return res;
		}
		res.redirect("/kanban");
		return res;
	});
	
	post("/kanban/register/submit" , (req,res) -> {
		String cust_id =req.queryParams("company");
		String cpassword = req.queryParams("company-password");
		String fname = req.queryParams("firstName");
		String lname = req.queryParams("lastName");
		String password = req.queryParams("password");
		String BundleId = req.queryParams("bundle_id");
		String companyPassword = getCompanyPassword(cust_id);
		if(companyPassword != null && companyPassword.equals(cpassword))
		{
			String redir="";
			User u = new User();
			u.setFname(fname);
			u.setLname(lname);
			u.setPassword(password);
			String cookie = registerUserToCompany(u,cust_id);
			if(cookie == null)
			{
				return "{\"success\":\"false\" , \"message\":\"There Is already a user with that name \"}";
			}
			if(BundleId != null)
			{
				redir="/kanban/scan";
			}
			else
			{
				redir="/kanban";
			}
			
			return "{\"success\":\"true\",\"redir\":\""+redir+"\",\"cookie\":\""+cookie+"\"}";
		}
		
		return "{\"success\":\"false\" , \"message\":\"invalid\"}";
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
					"(SELECT sum(quitem_price) FROM quitem WHERE quitem_quhead_id = quhead_id) AS estematedCost " + 
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
			int cohead_id = (int) r.get("cohead_id");
			//TODO: Possibly allow them to schedule there reports using xtconnect
			if(r.get("error") == null)
				return "{\"success\":true}";
			
		}
		return "{\"success\":false}";
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
	private static String registerUserToCompany(User u, String cust_id)
	{
		String cookieToReturn = null;
		
		//TODO: Find a way to create users from the web interface. Probably add some porperties to set up customers
		
		return cookieToReturn;
	}
	
	/**
	 * gets the kanban program password for a specific customer
	 * @param cust_id The customer's ID in the database; 
	 */
	private static String getCompanyPassword(String cust_id)
	{
		ResultList r = SQLEngine.executeDBQuery("SELECT charass_value AS password "
				+ "FROM charass "
				+ "WHERE charass_target_type = 'KBN' "
				+ "AND charass_target_id = "+cust_id);
		return (String) r.get("password");
	}

	private static User getUserFromCookie(String cookie)
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
	
	private static String login(User u, String kbc_id)
	{
		ResultList r = SQLEngine.executeDBQuery("SELECT pittsteelcustom.getCookieFromUser('"+u.getFname()+"','"+u.getLname()+"','"+u.getPassword()+"',"+kbc_id+") AS cookie");
		if(r!=null && r.first()) {
			return  (r.get("cookie")+"").equals("-1") ? null: r.get("cookie")+"";
		}
		else
			return null;
	}
}

