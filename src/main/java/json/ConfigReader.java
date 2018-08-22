package json;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

public class ConfigReader 
{
	public static <T extends Object>T ReadConf(Class<T> classOfObj, String file) 
	{
		ObjectMapper mapper = new ObjectMapper(); 
		File from = new File(file);
		if(from.exists())
		{
    	    try
    		{
    			return mapper.readValue(from, classOfObj);
    		} catch (IOException e)
    		{
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		} 
		}
		return null;
	}
	public static boolean WriteConf(Object o,String file) 
	{
		ObjectMapper mapper = new ObjectMapper(); 
		
		File targetFile = new File(file);
		File parent = targetFile.getParentFile();
		if (!parent.exists() && !parent.mkdirs()) {
		    throw new IllegalStateException("Couldn't create dir: " + parent);
		}
		if(!targetFile.exists())
		{
			try
			{
				targetFile.createNewFile();
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());
		try
		{
			writer.writeValue(new File(file), o);
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
	public static String printObject(Object o) 
	{
		ObjectMapper mapper = new ObjectMapper(); 
		
		
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());

			try
			{
				return writer.writeValueAsString(o);
			} catch (JsonProcessingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	
		return "error";
	}
	public static String printObject(Object[] ol) 
	{
		ObjectMapper mapper = new ObjectMapper(); 
		
		ArrayList<Object> olist = new ArrayList<Object>();
		
		for( Object o: ol)
			olist.add(o);
		
		ObjectWriter writer = mapper.writer();

			try
			{
				return writer.writeValueAsString(olist);
			} catch (JsonProcessingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	
		return "error";
	}
}
