package servers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.trunking.v1.Trunk;
import com.twilio.type.PhoneNumber;
import json.ConfigReader;
import regex.CommonRegex;
import server.Mapper;
import server.TwilioAuthData;
import spark.utils.IOUtils;
import static spark.Spark.*;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SmsServer implements server.Server{

	public static final Logger LOGGER = Logger.getLogger(SmsServer.class.getName());
	private static SmsServer instance = null;
	private boolean isRunning = false;
	private MailEngine me = null;
	private long startTime = 0l;
	private long lastErrorDate = 0l;
	private Timer st = null;
	private Exception lastError = null;
    public int[][] stats = new int [5][4];
	public String ACCOUNT_SID = "";
	public String AUTH_TOKEN = "";
	public boolean init = false;
	public HashMap<String, String> autoReplyMap;
	private boolean accepting = true;
	// Create Number and response maps
 
	//Create Single Instance
	private SmsServer()
	{
		load();
	}
	
	public static SmsServer getInstance()
	{
		if(instance == null)
		{
			instance = new SmsServer();
			instance.InitSmsServer();
			instance.setRunning(true);
		}
		return instance;
	}
	// Starts Web Server for twilio
	private void InitSmsServer()
	{
		Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
		init = true;	
		// Set Port for web server
		
		post("/text/sendSms.json", (req, res) -> {
			
			res.type("application/json");
			if(req.session(false) != null || req.ip().matches(CommonRegex.LOCAL_IP))
			{
				if((req.session(false) != null && req.session(false).attribute("username").equals("thewonderfullhint")) || req.ip().matches(CommonRegex.LOCAL_IP))
				{
					String to = req.queryParams("toNumber");
					String from = req.queryParams("fromNumber");
					String msg = req.queryParams("textBody");
					
					if(to==null || from==null)
					{
						return "{\"success\":false}";
					}
					
					String messageId = sendText(to, from, msg);
					if(messageId != null)
					{
						return "{\"success\":true, \"mid\":\""+messageId+"\"}";
					}
					return "{\"success\":false}";
				}
			}
			return null;
		});
		
		post("/text/numbers.json", (req, res) -> {
			HashSet<String> numberList = new HashSet<String>();
			ResourceSet<Trunk> trunks = Trunk.reader().read();
			res.type("application/json");
			for(Trunk t : trunks)
			{
				ResourceSet<com.twilio.rest.trunking.v1.trunk.PhoneNumber> phones = 
						com.twilio.rest.trunking.v1.trunk.PhoneNumber.reader(t.getSid()).read();
				for(com.twilio.rest.trunking.v1.trunk.PhoneNumber pn : phones)
					numberList.add(pn.getPhoneNumber().toString());
			}
			
			return ConfigReader.printObject(numberList);
		});
		
		post("/sms", (req, res) -> {
			
			if(!accepting)
			{
				res.status(403);
				return "";
			}
			
			stats[4][0]++; //Texts Recieved
			
			// Location for the email to go to
			String location = "";

			// Phone number that we are recieving the text from
			String fromNumber = req.queryParams("From");

			// Phone number the text is going to
			String toNumber = req.queryParams("To");

			// Get the location to send the email from the numberMap.
			location = Mapper.getEmail(toNumber);		

			// Check MMS
			int numMedia = 0;
			String numMediaStr = req.queryParams("NumMedia");
			
			if(numMediaStr != null)
				numMedia = Integer.parseInt(numMediaStr);
			

			// Create An Arraylist to hold all the attachments
			HashMap<String, byte[]> attachments = new HashMap<String, byte[]>();

			// Check to see If the text was an MMS
			if (numMedia > 0)
			{
				while (numMedia > 0)
				{
					numMedia = numMedia - 1;

					// Get all info
					String mediaUrl = req.queryParams((String.format("MediaUrl%d", numMedia)));
					String contentType = req.queryParams(String.format("MediaContentType%d", numMedia));
					String fileName = mediaUrl.substring(mediaUrl.lastIndexOf("/") + 1);
					String file = fileName + "." + contentType;

					// Download file
					URL url = new URL(mediaUrl);
					CloseableHttpClient httpclient = HttpClients.custom().setRedirectStrategy(new LaxRedirectStrategy())
							.build();
					HttpGet get = new HttpGet(url.toURI());
					HttpResponse response = httpclient.execute(get);
					InputStream source = response.getEntity().getContent();
					// Save Data into Byte Array
					byte b[] = IOUtils.toByteArray(source);
					attachments.put(file, b);
				}
			}
			// Make Sure if nothing is in the hashmap that we pass null
			if (attachments.size() < 1)
				attachments = null;
			boolean sent = false;
			// Send the email
			try {
				sent = me.sendEmail(location, "from"+fromNumber, "Re: SMS", req.queryParams("Body").toString(),
					me.getReplyID(fromNumber), attachments);
			}
			catch(Exception e) {
				
			};
			
			if(sent)
			{
				stats[4][2]++;//texts tunneled
			}
			else
			{
				stats[4][3]++;//tunnels failed
			}
			
			
			//TODO: Following code does not work properly 
			/*
			// Get the message history
			ResourceSet<Message> messages = Message.reader().setFrom(new com.twilio.type.PhoneNumber(fromNumber))
					.read();

			boolean withinTheMonth = false;

			// Iterate through the messages to find if one is within the last 30 days
			for (Message m : messages)
			{
				if (m.getDateSent() == null)
					continue;
				if ((m.getDateSent().compareTo(DateTime.now().minusDays(30)) >= 0))
				{
					withinTheMonth = true;
					break;
				}
			}

			// If no messages within the last 30 days. Then send the auto response
			if (!withinTheMonth)
			{
				res.type("application/xml");
				Body body = new Body.Builder(autoReplyMap.get(location)).build();
				com.twilio.twiml.messaging.Message sms = new com.twilio.twiml.messaging.Message.Builder().body(body)
						.build();
				MessagingResponse twiml = new MessagingResponse.Builder().message(sms).build();
				return twiml.toXml();
			}
			*/
			// END TODO:
			// Otherwise return an empty response //AKA don't send a message back
			return "<Response></Response>";
		});
	}
	// Send a text message
	public String sendText(String to, String from, String text, String tempfile)
	{
		stats[4][1]++; //texts sent
		LOGGER.info("Creating MMS Message");
		if (instance.init)
		{
			try
			{
				
			Message message = Message.creator(new PhoneNumber(to), // to
					new PhoneNumber(from), // from
					"").setMediaUrl("https://smsmail.pittsburgfoundry.com/file?FileName="+tempfile.replaceAll(" ", "%20")).create();
			LOGGER.info("Message Sent");
			return message.getSid();
			}
			catch (Exception e) {
				instance.lastError = e;
				instance.lastErrorDate = System.currentTimeMillis();
				LOGGER.log(Level.WARNING,"MMS Failed",e);
			}
		}
		return null;
	}
	
	public String sendText(String to, String from, String msg)
	{
		
		stats[4][1]++; //texts sent
		LOGGER.info("Creating Text Message");
		if (instance.init)
		{
			if(msg.replaceAll("[ \t\r]", "").length()>0)
			{
				try
				{
					Message message = Message.creator(new PhoneNumber(to), // to
							new PhoneNumber(from), // from
							msg).create();
					LOGGER.info("Message Sent");
					return message.getSid();
				}
				catch (com.twilio.exception.ApiException e) {
					LOGGER.info("CANNOT SEND EMPTY MESSAGE");
					return "";
				}
			}
			return "";
		}
		return null;
	}

	public String sendText(String to, String msg)
	{
		return sendText(to, "+19037086135", msg);
	}

	public String sendText(String msg)
	{
		return sendText("+19039463351", "+19039463351", msg);
	}
	// Lookup Message History chron is how many texts back we want to go
	public String messageHistory(String phoneNumber, String toPhone, int chron)
	{
		
		phoneNumber = phoneNumber.substring(phoneNumber.indexOf('+'), phoneNumber.indexOf('+')+ 12);
		
		ResourceSet<Message> messagesfrom = Message.reader().setFrom(new com.twilio.type.PhoneNumber(phoneNumber)).setTo(new com.twilio.type.PhoneNumber(toPhone))
				.read();

		ResourceSet<Message> messagesto = Message.reader().setTo(new com.twilio.type.PhoneNumber(phoneNumber)).setFrom(new com.twilio.type.PhoneNumber(toPhone)).read();

		ArrayList<Message> combined = new ArrayList<Message>();

		for (Message m : messagesfrom)
		{
			combined.add(m);
		}
		for (Message m : messagesto)
		{
			combined.add(m);
		}

		try
		{
			combined.sort(new Comparator<Message>() {
				@Override
				public int compare(Message o1, Message o2)
				{
					if (o2 == null || o1 == null)
						return -1;
					return o2.getDateSent().compareTo(o1.getDateSent());
				}
			});
		} catch (Exception e)
		{
			LOGGER.log(Level.FINE, e.toString()+" Sorting", e);
		}
		String returnVal = "\r\n";
		String inc = ">";
		for (int x = 1; x < (chron > combined.size() ? combined.size() : chron); x++) // Starts off at 1 to avoid the last message sent
		{
			
			DateTimeFormatter df = DateTimeFormat.forPattern("M/d/yyyy h:mm a,");
			String from = combined.get(x).getFrom().toString();

			returnVal += inc.substring(0, inc.length() - 1);

			returnVal += "On " + df.print(combined.get(x).getDateSent()) + " "
					+ (from.matches(CommonRegex.EMAIL_ADDRESS) ? Mapper.getNumber(from) : from)
					+ " wrote:\n";

			for (int y = 0; y <= x; y++)
				returnVal += ">";

			returnVal += combined.get(x).getBody() + "\n" + inc + "\n";
			inc += ">";
		}
		return returnVal;
	}
	
	public boolean isRunning()
	{
		return instance != null ? instance.isRunning : false;
	}
	
	public void setRunning(boolean isRunning)
	{
		if(instance!=null)
			instance.isRunning = isRunning;
		else if (isRunning)
			SmsServer.getInstance();
	}
	
	@Override
	public Object start()
	{
		accepting = true;
		startTime = System.currentTimeMillis();
		if(instance == null)
		{
			SmsServer.getInstance();
		}
		instance.isRunning = true;
		if(MailEngine.getInstance() != null)
			MailEngine.getInstance().regesterRequiredServers();
		
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
					for(int y = 0; y<stats[x].length;y++)
					{
						stats[x][y] = stats[x+1][y];
					}
				}
				for(int y = 0; y<stats[stats.length-1].length;y++)
				{
					stats[4][y] = 0;
				}
			}
		}, 30000, 120000);// 2 minutes
		
		return instance;
	}
	
	@Override
	public Object stop()
	{
		accepting  = false;
		return true;
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
		return "SMS Server";
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
		me=MailEngine.getInstance();
		
		return me != null;
	}

	/*
	 * (non-Javadoc)
	 * This will reload all of the related configuration files.
	 * TODO: authentication and the ID token as well as the number map and the autoreply map.
	 * TODO: /config/sms.conf with that will read the number map and the auto reply map. optionaly specify the configuration file. 
	 */
	public boolean reload()
	{
		load();
		return true;
	}

	@Override
	public boolean save()
	{
		try
		{
			TwilioAuthData td = new TwilioAuthData();
			td.setAccount_sid(ACCOUNT_SID);
			td.setAuth_token(AUTH_TOKEN);
			
			ConfigReader.WriteConf(td, "credentials/twilio.json");
			return true;
		} catch (Exception e)
		{	
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.WARNING, "Save Failed", e);
			return false;
		}
	}

	@Override
	public boolean load()
	{
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			
			//read json file data to String
			byte[] jsonData = Files.readAllBytes(Paths.get("credentials/twilio.json"));
			
			//convert json string to object
			TwilioAuthData twilio = objectMapper.readValue(jsonData, TwilioAuthData.class);
			ACCOUNT_SID = twilio.getAccount_sid();
			AUTH_TOKEN = twilio.getAuth_token();
		}catch (Exception e)
		{
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.WARNING, "Failed To load SMS configuration", e);
			return false;
		}
		Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
		return true;
	}

	@Override
	public ObjectNode getConfig()
	{
		ObjectMapper m = new ObjectMapper();
		
		ObjectNode o2 = m.createObjectNode();
		ObjectNode o1 = m.createObjectNode();
		o1.put("AuthToken",AUTH_TOKEN);
		o1.put("AccountSID", ACCOUNT_SID);
		o2.set("SmsServer", o1);
		
		return o2;
	}

	
	@Override
	public boolean setConfig(JsonNode config)
	{
		try
		{
			ACCOUNT_SID=config.get("AccountSID").asText();
			AUTH_TOKEN=config.get("AuthToken").asText();	
			return save();
		}catch(Exception e)
		{
			
			return false;
		}
	}

	@Override
	public ObjectNode getStats()
	{
		DateTime startDt = new DateTime();
		
		ObjectMapper mapper = new ObjectMapper();
		HashMap<String, Object> data = new HashMap<String, Object>();
		HashMap<String, Object> chartData;
		ArrayList<HashMap<String, Object>> chartPoints;
		
		HashMap<String,String> names = new HashMap<String,String>();
		HashMap<Integer,String> orders = new HashMap<Integer,String>();
		names.put("Texts Sent", "#619D67");
		names.put("Texts Recieved", "#455B4F");
		names.put("Texts Tunneled", "#88AA88");
		names.put("Tunnel Failed", "#E60000");

		
		orders.put(0,"Texts Sent");
		orders.put(1,"Texts Recieved");
		orders.put(2,"Texts Tunneled");
		orders.put(3,"Tunnel Failed");
		
		int count = 0;
		
		for(int y = 0; y<stats[0].length ; y++)
		{
			chartData = new HashMap<String, Object>();
			chartPoints = new ArrayList<HashMap<String, Object>>();
			data.put(orders.get(y), chartData);
			
			chartData.put("type", "line");
			chartData.put("name",  orders.get(y));
			chartData.put("color", names.get(orders.get(y)));
			chartData.put("showInLegend", true);
			chartData.put("markerType", "square");

			DateTime dt = startDt;
			
			HashMap<String, Object> temp = new HashMap<String, Object>();
			for(int x=stats.length-1;x>=0;x--)
			{
				temp.put("x", dt.getMillis());
				temp.put("y", stats[x][count]);
				chartPoints.add(temp);
				temp = new HashMap<String, Object>();
				dt = dt.minusMinutes(2);
			}				
			chartData.put("dataPoints",chartPoints);
			count++;
		}
		return mapper.valueToTree(data);
	}

	@Override
	public Long getStartTime()
	{
		return startTime;
	}
}
