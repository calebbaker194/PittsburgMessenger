package servers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twilio.Twilio;
import com.twilio.base.ResourceSet;
import com.twilio.twiml.MessagingResponse;
import com.twilio.twiml.messaging.Body;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import server.Mapper;
import server.TwilioAuthData;
import spark.utils.IOUtils;
import static spark.Spark.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
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
	private long lastErrorDate = 0l;
	private Exception lastError = null;
	
	private String ACCOUNT_SID = "";
	private String AUTH_TOKEN = "";
	public boolean init = false;
	public HashMap<String, String> autoReplyMap;
	// Create Number and response maps
 
	//Create Single Instance
	private SmsServer()
	{
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			
			//read json file data to String
			byte[] jsonData = Files.readAllBytes(Paths.get("credentials/twilio.json"));
			
			//convert json string to object
			TwilioAuthData twilio = objectMapper.readValue(jsonData, TwilioAuthData.class);
			ACCOUNT_SID = twilio.getAccount_sid();
			AUTH_TOKEN = twilio.getAuth_token();
			
		} catch (FileNotFoundException e) {
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.SEVERE, e.toString(), e);
		} catch (IOException e) {
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.SEVERE, e.toString(), e);
		}
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
		Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
		init = true;
		// Set Port for web server
		
		post("/sms", (req, res) -> {

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
			try
			{
				numMedia = Integer.parseInt(numMediaStr);
			} catch (Exception e)
			{
				System.out.println("Either No attachment or server fail");
			}

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

			// Send the email
			me.sendEmail(location, "from"+fromNumber, "Re: SMS", req.queryParams("Body").toString(),
					me.getReplyID(fromNumber), attachments);

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
			// Otherwise return an empty response //AKA don't send a message back
			return "<Response></Response>";
		});
	}
	// Send a text message
	public String sendText(String to, String from, String text, String tempfile)
	{
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
		for (int x = 0; x < (chron > combined.size() ? combined.size() : chron); x++)
		{
			DateTimeFormatter df = DateTimeFormat.forPattern("M/d/yyyy h:mm a,");
			String from = combined.get(x).getFrom().toString();

			returnVal += inc.substring(0, inc.length() - 1);

			returnVal += "On " + df.print(combined.get(x).getDateSent()) + " "
					+ (Mapper.getNumber(from))
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
		if(instance == null)
		{
			SmsServer.getInstance();
		}
		instance.isRunning = sendText("+19039463351", "+19037086135", "") != null;
		if(MailEngine.getInstance() != null)
			MailEngine.getInstance().regesterRequiredServers();
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
		return "SMS Server";
	}

	@Override
	public String getLastError()
	{
		return lastError != null ? lastError.getMessage():"";
	}

	@Override
	public String getLastErrorDate()
	{
		org.joda.time.format.DateTimeFormatter f = DateTimeFormat.forPattern("hh:mm:ss dd/MMM/YYYY");
		org.joda.time.DateTime dateTime = new org.joda.time.DateTime(lastErrorDate);
		return dateTime.toString(f);
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
	public void reload()
	{
		
	}

	@Override
	public void save()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void load()
	{
		// TODO Auto-generated method stub
		
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
}
