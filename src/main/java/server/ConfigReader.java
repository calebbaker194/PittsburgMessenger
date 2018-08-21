package server;

import java.io.File;
import java.io.IOException;
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
	public static boolean printObject(Object o) 
	{
		ObjectMapper mapper = new ObjectMapper(); 
		
		
		ObjectWriter writer = mapper.writer(new DefaultPrettyPrinter());

			try
			{
				System.out.println(writer.writeValueAsString(o));
				return true;
			} catch (JsonProcessingException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
	
		return false;
	}
}
