package kanban;

import static spark.Spark.*;
import java.util.HashMap;
import java.util.Map;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

public class Kanban 
{
	public static boolean routed;
	
	public Kanban() {
		if(!routed)
		{
			routed=true;
			mapRoutes();
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
		String id = req.queryParams("id");
		if(id == null)
		{
			id = req.cookie("ps-kanban-item-id");
		}
		if(name != null)
		{
			if(id != null)
			{
				System.out.println("Scan ID: "+id);
				res.redirect("/kanban/scan/success");
			}
			else
			{
				res.redirect("/kanban");
			}
			return res;
		}
		if(id != null)
		{
			res.cookie("ps-kanban-item-id", id);
		}
		res.redirect("/kanban/regester");
		
		return res;
	});
	
	get("/kanban/scan/success" , (req,res) -> {
		String id = req.cookie("ps-kanban-item-id");
		res.removeCookie("ps-kanban-item-id");
		String auth = req.cookie("ps-kanban-auth");
		String name = getUserFromID(auth);
		if(name != null)
		{
			Map<String, Object> model = new HashMap<String, Object>();
			return new VelocityTemplateEngine().render(
					
			new ModelAndView(model, "web/kanban/scan.html")
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
		String itemId = req.cookie("ps-kanban-item-id");
		String companyPassword = getCompanyPasswordByName(ppassword);
		if(companyPassword.equals(ppassword))
		{
			String redir="";
			regesterUserToCompany(puname,pcompany);
			if(itemId != null)
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
	private static void regesterUserToCompany(String puname, String pcompany)
	{
		// TODO Auto-generated method stub
		
	}
	private static String getCompanyPasswordByName(String ppassword)
	{
		// TODO Auto-generated method stub
		return null;
	}
	private static String getUserFromID(String auth)
	{
		//TODO DataBase
		return "Caleb";
	}
}

