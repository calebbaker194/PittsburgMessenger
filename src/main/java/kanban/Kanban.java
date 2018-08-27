package kanban;

import static spark.Spark.*;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.joda.time.LocalDateTime;
import org.slf4j.event.Level;

import launch.Main;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

public class Kanban 
{
	public static boolean routed;
	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(Kanban.class.getName());
	
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
		} catch (ClassNotFoundException e)
		{
			LOGGER.log(java.util.logging.Level.SEVERE,"CANNOT LOAD POSTGRESQL DRIVERS"+e.toString(),e);
		}
	}
	public static void mapRoutes(){

	get("/kanban" , (req,res) -> {
		String user = getUserFromID(req.cookie("ps-kanban-auth"));
		if(user != null)
		{
			Map<String, Object> model = new HashMap<String, Object>();
			return new VelocityTemplateEngine().render(
					
					new ModelAndView(model, "web/kanban/index.html")
			);
		}
		
		res.redirect("/kanban/regester");
		return res;
	});
	
	get("/kanban/scan" , (req,res) -> {

		String auth = req.cookie("ps-kanban-auth");
		String name = getUserFromID(auth);
		String bundleId = req.queryParams("ps-kanban-bundle-id");
		if(bundleId == null)
		{
			bundleId = req.cookie("ps-kanban-bundle-id");
		}
		
		if(name != null)
		{
			if(scanBundleIn(bundleId)) // This will Scan The Bundle In. And Return True on success
			{
				String itemId = getItemIdFromBundle(bundleId);
				if(itemId != null)
				{
					res.removeCookie("ps-kanban-bundle-id");
					res.redirect("/kanban/item?bundle-id="+bundleId+"&item-id="+itemId+"&scan=success");
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
			res.cookie("ps-kanban-bundle-id", bundleId);
		}
		res.redirect("/kanban/regester");
		
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
		String bundleId = req.cookie("bundle-id");
		if(id == null) {
			id = req.queryParams("item-id");
		}
		
		res.removeCookie("ps-kanban-item-id");
		String auth = req.cookie("ps-kanban-auth");
		String name = getUserFromID(auth);
		if(name != null)
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
		return false;
	});
	
	get("/kanban/regester" , (req,res) -> {

		String auth = req.cookie("ps-kanban-auth");
		String name = getUserFromID(auth);
		if(name == null || name.equals("Caleb"))
		{
			Map<String, Object> model = new HashMap<String, Object>();
			return new VelocityTemplateEngine().render(
					
					new ModelAndView(model, "web/kanban/regester.html")
			);
		}
		res.redirect("/kanban");
		return res;
	});
	
	post("/kanban/regester/submit" , (req,res) -> {
		String pcompany =req.queryParams("company");
		String ppassword = req.queryParams("company-password");
		String puname = req.queryParams("company-user-name");
		String BundleId = req.cookie("ps-kanban-bundle-id");
		String companyPassword = getCompanyPasswordByName(ppassword);
		if(companyPassword.equals(ppassword))
		{
			String redir="";
			regesterUserToCompany(puname,pcompany);
			if(BundleId != null)
			{
				redir="/kanban/scan";
			}
			else
			{
				redir="/kanban";
			}
			
			return "{\"success\":\"true\",\"redir\":\""+redir+"\"}";
		}
		
		return "{\"success\":\"false\" , \"message\":\"invalid\"}";
	});
	}
	
	///////////////////////////////// CONTROLER //////////////////////////////////////////////////
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
			LocalDateTime no = new LocalDateTime();
			SQLEngine.executeDBQuery("UPDATE kanbanlineitem"
					+ "SET kanbanlineitem_timecleared = NOW() "
					+ "WHERE kanbanlineitem_id = "+bundleId);
			return true;
		}
	}
	private static void regesterUserToCompany(String puname, String pcompany)
	{
		//TODO change return as well as database structure
	}
	private static String getCompanyPasswordByName(String ppassword)
	{
		// TODO Auto-generated method stub
		return "password";
	}
	private static String getUserFromID(String auth)
	{
		//TODO DataBase
		return "Caleb";
	}
}

