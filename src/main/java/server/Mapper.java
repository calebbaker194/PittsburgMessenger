package server;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class Mapper{

	private static HashMap<String,String> numToEmail = new HashMap<String,String>();
	private static HashMap<String,String> emailToNum = new HashMap<String,String>();
	public static final String DEFAUTL_MAP_LOCATION = "config/maps.conf";
	private static final String DEFAULT_MAP_LOOKUP="ALL";
	
	@SuppressWarnings("unchecked")
	public static boolean loadMap(String path) 
	{
		HashMap<String,HashMap<String, String>> a = new HashMap<String,HashMap<String, String>>();
		
		a  = ConfigReader.ReadConf(a.getClass(), DEFAUTL_MAP_LOCATION);
		if(a==null)
			return false;
		emailToNum=a.get("number-to");
		numToEmail=a.get("mail-to");
		
		return true;

	}
	public static boolean saveMap(String path) 
	{
		ObjectMapper mapper = new ObjectMapper(); 
		if(!new File("config").exists())
		{
			new File("config").mkdirs();
		}
		try
		{
			if(!new File("config/maps.conf").exists())
			new File("config/maps.conf").createNewFile();
		} catch (IOException e2)
		{
		}
		HashMap<String,HashMap<String, String>> a = new HashMap<String,HashMap<String, String>>();
		a.put("number-to",emailToNum);
		a.put("mail-to",numToEmail);
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
		try
		{
			writer.writeValue(new File("config/maps.conf"), a);
			return true;
		} catch (JsonGenerationException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (JsonMappingException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return false;
	}
	public static String getNumber(String email)
	{
		String reply=emailToNum.get(email);
		if(reply==null)
		{
			reply=emailToNum.get(DEFAULT_MAP_LOOKUP);
		}
		return reply;
	}
	public static String getEmail(String phone)
	{
		String reply=numToEmail.get(phone);
		if(reply==null)
		{
			reply=numToEmail.get(DEFAULT_MAP_LOOKUP);
		}
		return reply;
	}
	public static String getAutoReply(String phone)
	{
		return "";
	}
	public static void addEmailToNumber(String email,String number)
	{
		emailToNum.put(email, number);
	}
	public static void addNumberToEmail(String number,String email)
	{
		numToEmail.put(number , email);
	}
}
