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
		System.out.println("/kanban AUTH="+req.cookie("ps-kanban-auth")+" USER="+(user==null) + " " +(user != null));
		if(user != null)
		{
			System.out.println("true");
			Map<String, Object> model = new HashMap<String, Object>();
			return new VelocityTemplateEngine().render(
					
					new ModelAndView(model, "web/kanban/kanban.html")
			);
		}
		System.out.println("not right");
		res.redirect("/kanban/register");
		return res;
	});
	
	get("/kanban/scan" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		String bundleId = req.queryParams("ps-kanban-bundle-id");
		/*<*/System.out.println("kanban/scan");/*>*/
		if(user != null)
		{
			/*<*/System.out.println("kanban/scan has user");/*>*/
			if(scanBundleIn(bundleId)) // This will Scan The Bundle In. And Return True on success
			{
				try
				{
					/*<*/System.out.println("kanban/scan scanned bundle");/*>*/
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
	
	get("/kanban/items/:item" , (req,res) -> {
		/*<*/System.out.println("kanban/item");/*>*/
		String id = null;
		String bundleId = req.queryParams("bundle_id");
		id = req.params(":item");
		System.out.println(id);
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		if(user != null)
		{
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("item_id", id);
			if(bundleId != null)
				model.put("bundle_id", bundleId);
			else
				model.put("bundle_id","5");
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
		System.out.println("/kanban/login AUTH="+auth);
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
				System.out.println(req.host());
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
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		res.type("application/json");
		if(user == null)
		{
			String query = "SELECT kanbancust_id AS id, " + 
					"shipto_name AS name " + 
					"FROM charass " + 
					"JOIN pittsteelcustom.kanbancust ON(charass_target_id = kanbancust_id) " + 
					"JOIN shiptoinfo ON(shipto_id = kanbancust_shipto_id) " + 
					"WHERE charass_target_type = 'KBN' " + 
					"AND charass_char_id = 31";
			
			ResultList r = SQLEngine.executeDBQuery(query);
			System.out.println(r);
			return ConfigReader.printObject(r);
		}
		return null;
	});
	
	get("/kanban/bundles.json" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		System.out.println("/kanban/bundles AUTH="+auth);
		User user = getUserFromCookie(auth);
		res.type("application/json");

		System.out.println("fetching bundles");
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
				+ "kanbanlineitem_timecleared as cleared "
				+ "FROM pittsteelcustom.kanbanlineitem "
				+ "LEFT JOIN pittsteelcustom.kanbanitem ON (kanbanlineitem_kanbanitem_id = kanbanitem_id) "
				+ "WHERE kanbanitem_item_id = "+itemid;
		
		ResultList r = SQLEngine.executeDBQuery(query);
		System.out.println(r);
		return ConfigReader.printObject(r);

	});
	
	get("/kanban/items.json" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		res.type("application/json");
		if(user == null)
		{
			//TODO: QUERY NOT COMPLETE
			String query = "SELECT kanbanitem_id, "
					+ "kanbanitem_number as number, "
					+ "item_descrip1 as description, "
					+ "";
			
			ResultList r = SQLEngine.executeDBQuery(query);
			System.out.println(r);
			return ConfigReader.printObject(r);
		}
		return null;
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
	private static boolean scanBundleIn(String bundleId)
	{
		if (getItemIdFromBundle(bundleId) == null)
		{
			return false;
		}
		else 
		{
			SQLEngine.executeDBQuery("PREFORM pittsteelcustom.scanKanbanItem("+bundleId+")");
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
		System.out.println(cust_id);
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
			System.out.println("SELECT pittsteelcustom.getCookieFromUser('"+u.getFname()+"','"+u.getLname()+"','"+u.getPassword()+"',"+kbc_id+") AS cookie");
			return  (r.get("cookie")+"").equals("-1") ? null: r.get("cookie")+"";
		}
		else
			return null;
	}
}

