package servers;

import static spark.Spark.get;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.crypto.URIReferenceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.Permission;

public class DriveServer implements server.Server{

	public static DriveServer instance = null;
	private long startTime = 0l;
	private long lastErrorDate = 0l;
	private Exception lastError = null;
	private Timer st = null;
	private int stats[] = new int[5];
	private static final Logger LOGGER = Logger.getLogger(DriveServer.class.getName());
	
	private boolean isRunning = true;

	private static final String APPLICATION_NAME = "Drive File Share";
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String CREDENTIALS_FOLDER = "credentials"; // Directory to store user credentials.

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved credentials/ folder.
	 */
	private static final List<String> SCOPES = Arrays.asList(DriveScopes.DRIVE,DriveScopes.DRIVE_APPDATA,DriveScopes.DRIVE_METADATA);	
	private static final String CLIENT_SECRET_DIR = "credentials/client_secrete.json";

	/**
	 * Creates an authorized Credential object.
	 * 
	 * @param HTTP_TRANSPORT
	 *            The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException
	 *             If there is no client_secret.
	 */
	private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException
	{
		// Load client secrets.
		java.io.File initialFile = new java.io.File(CLIENT_SECRET_DIR);
		InputStream targetStream = new FileInputStream(initialFile);

		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(targetStream));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(CREDENTIALS_FOLDER)))
						.setAccessType("offline").build();
		return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
	}

	public static File getFile(String fileName)
	{
		NetHttpTransport HTTP_TRANSPORT;
		try
		{
			LOGGER.info("Getting Image URL for:"+fileName);
			HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
			Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
					.setApplicationName(APPLICATION_NAME).build();

			// Grab 2 files that match just to make sure I dont get the wrong file
			FileList result = service.files().list().setPageSize(2)
					.setFields("nextPageToken, files(id, name, webContentLink , mimeType)").setQ("name='" + fileName + "'")
					.execute();
			List<File> files = result.getFiles();
			if (files == null || files.isEmpty())
			{
				instance.setRunning(true);
				throw new URIReferenceException(" URL NOT found. message cannot be sent");
			}
			else
			{
				if (files.size() == 1)
				{
					instance.setRunning(true);
					LOGGER.info(files.get(0).toString());
					
					
					// Set file permission
					Permission permission = new Permission();
					permission.setRole("reader");
					permission.setType("anyone");
					permission.setAllowFileDiscovery(false);
					permission.setExpirationTime(new DateTime(System.currentTimeMillis()+86400000l));
					service.permissions().create(files.get(0).getId(), permission).execute();
					
					return files.get(0);
				} 
				else
				{
					instance.setRunning(true);
					throw new URIReferenceException(" Multiple Files Found with the Same URL Cannot Send message");
				}
			}
		} catch (GeneralSecurityException e)
		{
			instance.setRunning(false);
			instance.lastError = e;
			instance.lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.WARNING, e.toString(), e.getMessage());
			return null;
		} catch (IOException e)
		{
			instance.setRunning(false);
			instance.lastError = e;
			instance.lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.WARNING, e.toString(), e.getMessage());
			return null;
		} catch (URIReferenceException e)
		{
			LOGGER.info("File not found in google drive");
			instance.lastError = e;
			instance.lastErrorDate = System.currentTimeMillis();
			return null;
		}
	}
	

	
	private DriveServer()
	{
		LOGGER.info("Logging into the drive");
		get("/file",(req,res) -> {
			stats[4]++;
			String filename = req.queryParams("FileName").replaceAll("%20", " ");
			
			com.google.api.services.drive.model.File f = DriveServer.getFile(filename);
			if(f == null)
			{
				return "File NOT Found";
			}
			InputStream in = new URL(f.getWebContentLink()).openStream();
			OutputStream out = res.raw().getOutputStream();
			
			byte[] buffer = new byte[4096];
	         int length;
	         while ((length = in.read(buffer)) > 0){
	            out.write(buffer, 0, length);
	         }
	         in.close();
	        res.header("Content-Type", f.getMimeType());
			return res;
		});
		get("/file/:fileName",(req,res) -> {
			stats[4]++;
			String filename = req.params(":fileName");
			
			com.google.api.services.drive.model.File f = DriveServer.getFile(filename);
			if(f == null)
			{
				return "File NOT Found";
			}
			InputStream in = new URL(f.getWebContentLink()).openStream();
			OutputStream out = res.raw().getOutputStream();
			
			byte[] buffer = new byte[4096];
	         int length;
	         while ((length = in.read(buffer)) > 0){
	            out.write(buffer, 0, length);
	         }
	         in.close();
	        res.header("Content-Type", f.getMimeType());
	        res.type(f.getMimeType());
			return res;
		});
		setRunning(true);
	}
	
	
	public static DriveServer getInstance()
	{
		if (instance == null)
		{
			instance = new DriveServer();
		}
		return instance;
	}

	public boolean isRunning()
	{
		return instance != null ? instance.isRunning : false;
	}

	public void setRunning(boolean isRunning)
	{
		if(instance != null)
			instance.isRunning = isRunning;
	}

	@Override
	public Object start()
	{
		
		if(st != null)
		{
			st.cancel();
		}
		st = new Timer();
		st.schedule( new TimerTask() {
			
			@Override
			public void run()
			{
				for(int x = 0;x<4;x++)
				{
					stats[x] = stats[x+1];
					stats[x] = stats[x+1];
				}
				stats[4]=0;
				stats[4]=0;
			}
		}, 30000, 120000);// 2 minutes
		
		startTime = System.currentTimeMillis();
		if (instance == null)
		{
			instance = new DriveServer();
		}
		URL url;
		try
		{
			url = new URL("http://www.google.com");
			URLConnection conn = url.openConnection();
			conn.getContent();
			instance.isRunning = true;
		} catch (MalformedURLException e)
		{
			instance.isRunning = false;
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
		} catch (IOException e)
		{
			instance.isRunning = false;
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
		}
		
		return instance;
	}

	@Override
	public Object stop()
	{
		instance = null;
		return null;
	}

	@Override
	public Object restart()
	{
		stop();
		return start();
	}

	@Override
	public String getName()
	{
		return "Drive Server";
	}

	@Override
	public String getLastError()
	{
		return lastError != null ? lastError.getMessage():"";
	}

	@Override
	public long getLastErrorDate()
	{
		return lastErrorDate;
	}
	
	@Override
	public boolean regesterRequiredServers()
	{
		return true;
	}

	public boolean reload()
	{
		//Keep Blank. The drive loads its credentials every time it calls for a link
		return true;
	}

	@Override
	public boolean save()
	{
		// not sure how its going to play in
		return true;
	}

	@Override
	public boolean load()
	{
		//Keep Blank. The drive loads its credentials every time it calls for a link
		return true;
	}

	@Override
	public ObjectNode getConfig()
	{
		ObjectMapper m = new ObjectMapper();
		ObjectNode o1 = m.createObjectNode();
		o1.put("nothing", "nothing");
		return o1;
	}

	@Override
	public boolean setConfig(JsonNode config)
	{
		return true;
	}

	@Override
	public ObjectNode getStats()
	{
		org.joda.time.DateTime dt = new org.joda.time.DateTime();
		
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> data = new HashMap<String, Object>();
		HashMap<String, Object> chartData1 = new HashMap<String, Object>();
		
		ArrayList<HashMap<String, Object>> chartPoints = new ArrayList<HashMap<String, Object>>();
		data.put("0", chartData1);
		
		chartData1.put("type", "line");
		chartData1.put("name",  "Images Uploaded");
		chartData1.put("color", "#619D67");
		chartData1.put("showInLegend", true);
		chartData1.put("markerType", "square");
		
		HashMap<String, Object> temp = new HashMap<String, Object>();
		temp.put("x", dt.getMillis());
		temp.put("y", stats[4]);
		chartPoints.add(temp);
		temp = new HashMap<String, Object>();
		dt = dt.minusMinutes(2);
		temp.put("x", dt.getMillis());
		temp.put("y", stats[3]);
		chartPoints.add(temp);
		temp = new HashMap<String, Object>();
		dt = dt.minusMinutes(2);
		temp.put("x", dt.getMillis());
		temp.put("y", stats[2]);
		chartPoints.add(temp);
		temp = new HashMap<String, Object>();
		dt = dt.minusMinutes(2);
		temp.put("x", dt.getMillis());
		temp.put("y", stats[1]);
		chartPoints.add(temp);
		temp = new HashMap<String, Object>();
		dt = dt.minusMinutes(2);
		temp.put("x", dt.getMillis());
		temp.put("y", stats[0]);
		chartPoints.add(temp);
		
		chartData1.put("dataPoints",chartPoints);
		
		return mapper.valueToTree(data);
	}

	@Override
	public Long getStartTime()
	{
		return startTime;
	}
}
