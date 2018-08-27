package kanban;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.security.auth.login.FailedLoginException;

public class SQLEngine {
	
	public static HashMap<String,Integer> flagMap = new HashMap<String,Integer>();
	
	static {
		flagMap.put("SELECT", 0);
		flagMap.put("INSERT", 1);
		flagMap.put("UPDATE", 2);
		flagMap.put("DELETE", 3);
		flagMap.put("CREATE", 4);
		flagMap.put("ALTER", 5);
		flagMap.put("DROP", 6);
	}
	
	private static String connectionString = null;
	private static String username;
	private static String password;

	public static String SQLConnection(String db,String host,int port,String username,String password)
	{
		connectionString="jdbc:postgresql://"+host+":"+port+"/"+db;
		Connection dbConnection;
		try 
		{
			//Should Only Call This once. Will Probably Move it to Main
			Class.forName("org.postgresql.Driver");
			
			dbConnection = DriverManager.getConnection(connectionString,username,password);
			dbConnection.close();
			setPassword(password);
			setUsername(username);
			return "0";
		} 
		catch (ClassNotFoundException e1) 
		{
			e1.printStackTrace();
			return "Drivers: "+e1.getMessage();
		} catch (SQLException e) {
			e.printStackTrace();
			return "SQL: "+e.getMessage();
		}
	}
	
	public static ResultList executeDBQuery(String query)
	{
		int flag = 0;
		
		String function = query.contains(" ") ? query.split(" ")[0].toUpperCase() : query.toUpperCase();
		
		flag = SQLEngine.flagMap.get(function) != null ? SQLEngine.flagMap.get(function) : 0;
		
		
		return executeDBQuery(query,flag);
	}
	
	/*
	 *Execute a query. the flag currently states if you want to return generated keys 
	 */
	public static ResultList executeDBQuery(String query,int flag)
	{
		Connection dbConnection=null;
		
		try 
		{
			dbConnection = DriverManager.getConnection(connectionString,username,password);
		}
		catch (SQLException e) 
		{
			System.out.println("Failed To Connect");
			e.printStackTrace();
		}
		
		try {
			if(dbConnection!=null)
			{
				try {
					Statement st = dbConnection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
					List<HashMap<String,Object>> resultArray = new ArrayList<HashMap<String,Object>>();
					HashMap<String,Object> row;
					ResultSet results;
					
					if(flag==0)
					{
						results = st.executeQuery(query);
					}	
					else
					{
						st.executeUpdate(query,Statement.RETURN_GENERATED_KEYS);
						results = st.getGeneratedKeys();
					}
					
					ResultSetMetaData meta= results.getMetaData();
						
						
					int columns = meta.getColumnCount();
					while(results.next())
					{
						row = new HashMap<String,Object>(columns);
						for(int i=1;i<=columns;i++)
						{
							row.put(meta.getColumnName(i), results.getObject(i));
						}
						resultArray.add(row);
					}
				
					results.close();
					
					st.close();
					dbConnection.close();
					return new ResultList(resultArray);
				} catch (SQLException e) {
					System.err.println("Query Failed" + e.getMessage());
				}
			}
			else
			{
				throw new FailedLoginException("Failed to connect to the database");
			}
		} catch (FailedLoginException e) {
			e.printStackTrace();
			return null;
		}
		return null;
	}
	public static String getPassword() {
		return password;
	}
	public static void setPassword(String password) {
		SQLEngine.password = password;
	}
	public static String getUsername() {
		return username;
	}
	public static void setUsername(String username) {
		SQLEngine.username = username;
	}

	
	/*
	 * Querys a specific table to see if the database has been set up
	 */
	public static boolean isInit()
	{
		try
		{
			ResultList r1 = new ResultList(SQLEngine.executeDBQuery("SELECT * FROM verify LIMIT 1"));
			return r1.first();
		}
		catch(Exception e)
		{
			return false;
		}
	}

}