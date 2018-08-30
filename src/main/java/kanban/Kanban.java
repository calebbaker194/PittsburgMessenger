package kanban;

import static spark.Spark.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.joda.time.LocalDateTime;
import org.slf4j.event.Level;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import json.ConfigReader;
import launch.Main;
import spark.ModelAndView;
import spark.Response;
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
					
					new ModelAndView(model, "web/kanban/index.html")
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
			if(scanBundleIn(bundleId)) // This will Scan The Bundle In. And Return True on success
			{
				String itemId = getItemIdFromBundle(bundleId);
				if(itemId != null)
				{
					res.removeCookie("ps-kanban-bundle-id");
					res.redirect("/kanban/item?bundle_id="+bundleId+"&item-id="+itemId+"&scan=success");
				}
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
	
	get("/kanban/item" , (req,res) -> {
		String id = req.cookie("ps-kanban-item-id");
		String bundleId = req.queryParams("bundle_id");
		if(id == null) {
			id = req.queryParams("item-id");
		}
		
		res.removeCookie("ps-kanban-item-id");
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
		if(user == null || user.getFname().equals("Caleb"))
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
		if(user == null || user.getFname().equals("Caleb"))
		{
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("registerd", true);
			return new VelocityTemplateEngine().render(
					
					new ModelAndView(model, "web/kanban/register.html")
			);
		}
		res.redirect("/kanban");
		return res;
	});
	
	get("/kanban/login/submit" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		if(user == null || user.getFname().equals("Caleb"))
		{
			User u = new User();
			u.setFname(req.queryParams("firstName"));
			u.setLname(req.queryParams("lastName"));
			u.setPassword(req.queryParams("password"));
			
			String response = login(u,req.queryParams("company"));
			boolean success = response != null;
			if(success)
			{	
				res.cookie("ps-kanban-auth", response);
				
			}
			else
			{
				res.redirect("/kanban/login?failed=true");;
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
		String companyPassword = getCompanyPassword(getCompanyId(cust_id));
		if(companyPassword.equals(cpassword))
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
		if(user == null || true)
		{
			String query = "SELECT cust_id AS id, "
					+ "cust_name AS name "
					+ "FROM charass "
					+ "JOIN cust ON(charass_target_id = cust_id) "
					+ "WHERE charass_target_type = 'C' "
					+ "AND charass_char_id = 31";
			
			ResultList r = SQLEngine.executeDBQuery(query);
			
			return ConfigReader.printObject(r);
		}
		return null;
	});
	get("/kanban/bundles.json" , (req,res) -> {
		String auth = req.cookie("ps-kanban-auth");
		User user = getUserFromCookie(auth);
		res.type("application/json");
		if(user == null || true)
		{
			String itemid = req.queryParams("item_id");
			String query="";
			if(itemid == null)
			{
				String bundleid = req.queryParams("bundle_id");
				if(bundleid == null)
				{
					return null;
				}
				itemid = getItemFromBundle(bundleid);
			}
			
			query = "SELECT kanbanlineitem_id as id, "
					+ "kanbanlineitem_timecreated as created, "
					+ "kanbanlineitem_timecleared as cleared "
					+ "FROM kanbanlineitem "
					+ "LEFT JOIN kanbanitem ON (kanbanlineitem_kanbanitem_id = kanbanitem_id) "
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
		if(user == null || true)
		{
			//TODO: QUERY NOT COMPLETE
			String query = "SELECT kanbanitem_id, "
					+ "kanbanitem_number as number, "
					+ "item_descrip1 as description, "
					+ "";
			
			ResultList r = SQLEngine.executeDBQuery(query);
			
			return ConfigReader.printObject(r);
		}
		return null;
	});
	///////////////////////////////// CONTROLER //////////////////////////////////////////////////
	}
	private static String getItemFromBundle(String bundleid)
	{
		// TODO Auto-generated method stub
		return null;
	}
/*
	 * Gets the item id that a specific bundle is related to
	 * @param Bundle: The bundle_id to link to the item_id
	 */
	private static String getItemIdFromBundle(String bundleId)
	{
		ResultList r = SQLEngine.executeDBQuery("SELECT kanbanitem_item_id AS item_id"
				+ "FROM kanbanlineitem "
				+ "LEFT JOIN kanbanitem ON (kanbanlineitem_kanbanitem_id = kanbanitem_id) "
				+ "WHERE kanbanlineitem_id = "+bundleId);
		return (String) r.get(0).get("item_id");
	}
	private static boolean scanBundleIn(String bundleId)
	{
		if (getItemIdFromBundle(bundleId) == null)
		{
			return false;
		}
		else 
		{
			SQLEngine.executeDBQuery("UPDATE kanbanlineitem"
					+ "SET kanbanlineitem_timecleared = NOW() "
					+ "WHERE kanbanlineitem_id = "+bundleId);
			return true;
		}
	}
	private static String registerUserToCompany(User u, String cust_id)
	{
		String cookieToReturn = null;
		
		//TODO: Find a way to create users from the web interface. Probably add some porperties to set up customers
		
		return cookieToReturn;
	}
	
	/*
	 * gets the kanban program password for a specific customer 
	 */
	private static String getCompanyPassword(String cust_id)
	{
		ResultList r = SQLEngine.executeDBQuery("SELECT charass_value AS password "
				+ "FROM charass "
				+ "WHERE charass_target_type = 'C' "
				+ "AND charas_target_id = "+cust_id);
		
		return (String) r.get("password");
	}
	private static String getCompanyId(String name)
	{
		// TODO Actually Grab Live Company Name
		return "95";
	}
	private static User getUserFromCookie(String cookie)
	{
		ResultList r = SQLEngine.executeDBQuery("SELECT \r\n" + 
				"cntct_id AS id,\r\n" + 
				"cntct_first_name AS fname, \r\n" + 
				"cntct_last_name AS lname\r\n" + 
				"FROM charass\r\n" + 
				"JOIN cntct ON (charass_target_id = cntct_id)\r\n" + 
				"WHERE charass_target_type = 'CNTCT'\r\n" + 
				"AND charass_char_id = 32\r\n" + 
				"AND charass_value = '"+cookie+"'");
		
		User temp = new User();
		if(r.first())
		{
			temp.setFname((String) r.get("fname"));
			temp.setLname((String) r.get("lname"));
			temp.setId((long) r.get("id"));
			
			return temp;
		}
		return null;
	}
	
	private static String login(User u, String queryParams)
	{
		String cookieToSet = null;
		// TODO Login Stuff
		return cookieToSet;
	}
}

