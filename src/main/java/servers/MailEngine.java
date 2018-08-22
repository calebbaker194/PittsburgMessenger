package servers;

import java.io.UnsupportedEncodingException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.activation.DataHandler;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;
import javax.mail.search.FromTerm;
import javax.mail.search.RecipientTerm;
import javax.mail.search.SearchTerm;
import javax.mail.util.ByteArrayDataSource;
import javax.mail.*;
import javax.mail.Flags.Flag;
import javax.mail.Message.RecipientType;
import org.eclipse.jetty.util.BlockingArrayQueue;
import org.joda.time.format.DateTimeFormat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import email.ImapServer;
import json.ConfigReader;
import json.MailServerConfig;
import regex.CommonRegex;
import server.Mapper;
import server.Server;
import server.mailParseTask;

public class MailEngine implements Server{
	
	private long lastErrorDate = 0l;
	public boolean locked = false;
	public long locktime = System.currentTimeMillis();
	private Exception lastError = null;
	private ExecutorService threadPool;
	private ArrayDeque<Future<String>> threadStatus= new ArrayDeque<Future<String>>(); 
	public static final Logger LOGGER = Logger.getLogger(MailEngine.class.getName());
	private static final boolean DEBUG = true;
	public String mailStrProt = "imap";
	Thread t;
	private Timer timer = new Timer();
	private SmsServer sms = null;
	private boolean isRunning = false;
	public Session emailSessionObj;
	public Store storeObj;
	public Pattern pattern = Pattern
			.compile("On [0-9]?[0-9]/[0-9]?[0-9]/[0-9][0-9][0-9][0-9] [0-9]?[0-9].[0-9][0-9] (AM|PM), .* wrote.");
	public static Long CHECK_INTERVAL = 5000l;
	public ArrayList<ImapServer> defaultServers = new ArrayList<ImapServer>();
	public boolean Continue = true;
	public static final String DEFAULT_REPLY_NUMBER="ALL";
	public static String DEFAULT_FROM_EMAIL = "";
	
	public static MailEngine instance = null;

	private MailEngine()
	{
	    load();
	}
	
	private void initMailServer()
	{
		Properties props = new Properties();
		props.put("mail.imap.host", defaultServers.get(0).getHost());
		props.put("mail.imap.port", defaultServers.get(0).getPort());

		emailSessionObj = Session.getInstance(props);
		
		try
		{
			storeObj = emailSessionObj.getStore("imap");
			
			// Initiate Imap Connection
			storeObj.connect(defaultServers.get(0).getHost(),defaultServers.get(0).getUsername(), defaultServers.get(0).getPassword());

			boolean isCreated = true;
			// Grab The default INBOX folder
			Folder defaultFolder = storeObj.getDefaultFolder();
			// Create Folder Sent
			Folder newFolder = defaultFolder.getFolder("Sent");
			if (!newFolder.exists())
				isCreated = newFolder.create(Folder.HOLDS_MESSAGES);

			// Create Folder SENT mail
			Folder newFolder2 = defaultFolder.getFolder("SentMail");
			if (!newFolder2.exists())
				isCreated = newFolder.create(Folder.HOLDS_MESSAGES);
			
			LOGGER.log(Level.INFO, ("Folder Structure is Good: " + isCreated));
			LOGGER.info("Mail Server Is running");
			
			
		} catch (MessagingException e)
		{
			e.printStackTrace();
			if (MailEngine.DEBUG)
				e.printStackTrace();
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			//LOGGER.log(Level.SEVERE, e.toString(), e);
		}
		connect();
	}
	
	public static MailEngine getInstance()
	{
		if (instance == null)
		{
			instance = new MailEngine();
			instance.setRunning(false);
			instance.initMailServer();
			instance.start();
		}
		
		return instance;
	}

	public Boolean connect()
	{
		Properties props = new Properties();
		
		props.put("mail.imap.host", defaultServers.get(0).getHost());
		props.put("mail.imap.port", defaultServers.get(0).getPort());

		emailSessionObj = Session.getDefaultInstance(props);

		try {
			storeObj = emailSessionObj.getStore("imap");
			storeObj.connect(defaultServers.get(0).getHost(), defaultServers.get(0).getUsername(), defaultServers.get(0).getPassword());
		} catch (NoSuchProviderException e) {
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			return false;
		}
		catch (MessagingException e) {
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.SEVERE, e.toString(), e);
			return false;
		}
		return true;
	}
	
	public Boolean start()
	{
		if(instance!= null && isRunning)
		{
			return true;
		}
		else
		{
			timer.schedule(new TimerTask() {	
				@Override
				public void run()
				{
					checkMail();
				}
			}, 1000,CHECK_INTERVAL); 
			isRunning = false;
		}
		if(SmsServer.getInstance() != null)
			SmsServer.getInstance().regesterRequiredServers();
		return true;
	}

	public Boolean stop()
	{
		Continue = false;
		timer.cancel();
		instance = null;
		return true;
	}

	public Boolean restart()
	{
		stop();
		return start();
	}

	public void sendEmail(String toEmailAddress, String subject, String emailBody)
	{
		sendEmail(toEmailAddress, DEFAULT_FROM_EMAIL, subject, emailBody, 0, null, null);
	}

	public void sendEmail(String toEmailAddress, String fromEmailAddress, String subject, String emailBody,
			String id, HashMap<String, byte[]> attachments)
	{
		sendEmail(toEmailAddress, fromEmailAddress, subject, emailBody, 0, id, attachments);
	}

	public void sendEmail(String toEmailAddress, String fromEmailAddress, String subject, String emailBody,
			int AddressIndex, String id, HashMap<String, byte[]> attachments)
	{

		// Index to decide what email address will send the mail
		int index = AddressIndex;

		// Create the to address based on a string
		String[] to = { toEmailAddress };

		// Declare String for phone history so that If I grab Phone History I can Store
		// It
		String messageHistory = "\n\n";
		// Check to see if the from address is a phone number
		// Add an email address to it if it is
		boolean isFromPhone = isNumber(fromEmailAddress);
		if (isFromPhone)
		{
			messageHistory = sms.messageHistory(fromEmailAddress,Mapper.getNumber(toEmailAddress), 10);
			fromEmailAddress += "@smsmail.pittsburgfoundry.com";
		}

		Properties prop;
		// Set send address properties
		prop = System.getProperties();
		prop.put("mail.smtp.starttls.enable", defaultServers.get(index).getStarttls());
		prop.put("mail.smtp.host", defaultServers.get(index).getHost());
		prop.put("mail.smtp.user", defaultServers.get(index).getUsername());
		prop.put("mail.smtp.password", defaultServers.get(index).getPassword());
		prop.put("mail.smtp.port", "25");
		prop.put("mail.smtp.auth", defaultServers.get(index).getAuth());

		// Open Session
		Session session = Session.getDefaultInstance(prop);

		// Create New Message
		MimeMessage message = new MimeMessage(session);

		LOGGER.info("Building Email");

		try
		{

			// SET FROM//Set the email address to the specified from email address unless
			// none specified then default
			if (fromEmailAddress.matches(CommonRegex.EMAIL_ADDRESS))
			{
				message.setFrom(new InternetAddress(fromEmailAddress));
			} else
			{
				message.setFrom(
						new InternetAddress(defaultServers.get(index).getAddress(), defaultServers.get(index).getCommonName()));
			}

			// SET HEADER //Check for messages in this thread.
			if (id != null)
				message.setHeader("In-Reply-To", id);

			InternetAddress[] toAddress = new InternetAddress[to.length];

			// CREATE RICIPIENTS// To get the array of addresses // Currently will always be
			// one
			for (int i = 0; i < to.length; i++)
			{
				LOGGER.info(isFromPhone ? "Passing Message To: " + to[i] : "Passing Email To: " + to[i]);
				toAddress[i] = new InternetAddress(to[i]);
			}

			// SET RECIPIENTS//Add all recepients to the message // Still only one
			for (int i = 0; i < toAddress.length; i++)
			{
				message.addRecipient(Message.RecipientType.TO, toAddress[i]);
			}

			// SET SUBJECT//Sets the subject unless not specified then use previous message
			// subject
			message.setSubject(subject);

			// Create Multipart for message body
			Multipart multipart = new MimeMultipart();

			// SET BODY TEXT//Set the content of the email
			BodyPart messageBody = new MimeBodyPart();
			messageBody.setText(emailBody + (isFromPhone ? messageHistory : ""));
			multipart.addBodyPart(messageBody);

			LOGGER.info("Adding Attachments");

			// ADD ATTACHMENTS// if there are any
			if (attachments != null)
			{
				for (String name : attachments.keySet())
				{
					if (name != null && attachments.get(name).length > 10)
						addAttachment(multipart, name, attachments.get(name));
					LOGGER.info("name:" + name);
				}
			}
			message.setContent(multipart, "text/plain");

			LOGGER.info("Sending Message from email");
			// Connect the transport and send the email. // May move this to its own thread.
			// It can take some time.
			
			Transport transport = session.getTransport("smtp");
			transport.connect(defaultServers.get(index).getHost(), defaultServers.get(index).getUsername(),
					defaultServers.get(index).getPassword());
			transport.sendMessage(message, message.getAllRecipients());
			transport.close();

			LOGGER.info("Copying Mail to sent Folder: " + (isFromPhone ? "Sent" : "SentMail"));
			// Copy mail to correct folder
			Folder folder = storeObj.getFolder(isFromPhone ? "Sent" : "SentMail");
			folder.open(Folder.READ_WRITE);
			message.setFlag(Flag.SEEN, true);
			folder.appendMessages(new Message[] { message });

		} catch (AddressException e)
		{
			if (MailEngine.DEBUG)
				e.printStackTrace();
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.WARNING, e.toString(), e);
		} catch (MessagingException e)
		{
			if (MailEngine.DEBUG)
				e.printStackTrace();
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.WARNING, e.toString(), e);
		} catch (UnsupportedEncodingException e)
		{
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.SEVERE, e.toString(), e);
		} catch (Exception e)
		{
			if (MailEngine.DEBUG)
			{
				LOGGER.severe("CRITICAL FAILURE");
				e.printStackTrace();
			}
			LOGGER.log(Level.WARNING, e.toString(), e);
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
		}
	}

	private boolean isNumber(String fromEmailAddress)
	{
		if(fromEmailAddress.matches(CommonRegex.PHONE_NUMBER))
			return true;
		else if(fromEmailAddress.matches("from"+CommonRegex.PHONE_NUMBER))
			return true;
		return false;
	}

	public void checkMail()
	{
		if(!locked)
		{
			try
			{
				locked = true;
				locktime = System.currentTimeMillis();
				
				
				// Create folder object and open it mode
				Folder emailFolderObj = storeObj.getFolder("INBOX");
				Folder sentFolder = storeObj.getFolder("Sent");
				// Open Inbox
				emailFolderObj.open(Folder.READ_WRITE);
				sentFolder.open(Folder.READ_WRITE);
				try
				{
	
					// Fetch UNANSWERED messages
					Flags answered = new Flags(Flags.Flag.ANSWERED);
					FlagTerm ansterm = new FlagTerm(answered, false);
					
					javax.mail.Message[] messageobjs = emailFolderObj.search(ansterm);
					ThreadPoolExecutor threadExecutor = new ThreadPoolExecutor(5, 10, 60l, TimeUnit.SECONDS, new BlockingArrayQueue<Runnable>());
					threadExecutor.allowCoreThreadTimeOut(true);
					threadPool = threadExecutor;
					
					for (int i = 0, n = messageobjs.length; i < n; i++)
					{
						javax.mail.Message indvidualmsg = messageobjs[i];
									
						mailParseTask m = new mailParseTask(indvidualmsg);
						
						threadStatus.add(threadPool.submit(m));
						
						if(threadStatus.size()>=10)
						{
							try
							{
								threadStatus.poll().get(10, TimeUnit.SECONDS);
							}
							catch(TimeoutException e) { LOGGER.log(Level.WARNING, e.toString(), e);}
							catch(NullPointerException e) {}
						}
							
					}
	
					//Clear The Exicutor
					if(!threadPool.awaitTermination(2, TimeUnit.SECONDS));
					
					// Now close all the objects
					threadPool.shutdown();
					emailFolderObj.close();
					sentFolder.close();
					//storeObj.close();
					setRunning(true);
	
				} catch (NoSuchProviderException e)
				{
					lastError = e;
					lastErrorDate = System.currentTimeMillis();
					LOGGER.log(Level.WARNING, e.toString(), e);
					isRunning = false;
				} catch (MessagingException e)
				{
					lastError = e;
					lastErrorDate = System.currentTimeMillis();
					LOGGER.log(Level.WARNING, e.toString(), e);
					isRunning = false;
				} catch (Exception e)
				{
					lastError = e;
					lastErrorDate = System.currentTimeMillis();
					LOGGER.log(Level.WARNING, e.toString(), e);
					isRunning = false;
				}
				locked = false;
			} catch (MessagingException e)
			{
				lastError = e;
				lastErrorDate = System.currentTimeMillis();
				isRunning = false;
				LOGGER.log(Level.WARNING, e.toString(), e);
				e.printStackTrace();
			}
		}
		else if(System.currentTimeMillis()-locktime > 10000)
		{
			locked = false;
		}
	}

	private void addAttachment(Multipart multipart, String filename, byte[] data)
	{

		ByteArrayDataSource source = new ByteArrayDataSource(data, filename);
		MimeBodyPart messageBodyPart = new MimeBodyPart();
		try
		{
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(filename);
			multipart.addBodyPart(messageBodyPart);
		} catch (MessagingException e)
		{
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.severe("Attachement Failed To Retrieve");
			e.printStackTrace();
		}

	}

	public synchronized String getReplyID(String fromPhoneNumber)
	{
		
		String fromAddress = fromPhoneNumber;
		// Add address to phone number
		if (fromPhoneNumber.matches(CommonRegex.PHONE_NUMBER))
			fromAddress = fromPhoneNumber + "@smsmail.pittsburgfoundry.com";

		Properties props = new Properties();

		props.put("mail.imap.host", defaultServers.get(0).getHost());
		props.put("mail.imap.port", defaultServers.get(0).getPort());
		try
		{
			// Initiate Imap Connection
			// Create folder object and open it mode
			Folder emailFolderObj = storeObj.getFolder("INBOX");
			Folder sentFolder = storeObj.getFolder("Sent");
			// What we send internal to external
			emailFolderObj.open(Folder.READ_WRITE);
			//
			sentFolder.open(Folder.READ_WRITE);

			// Create filters for email addresses
			SearchTerm mailside = new FromTerm(new InternetAddress(fromAddress));
			SearchTerm recieveSide = new RecipientTerm(RecipientType.TO, new InternetAddress(fromAddress));
			Message[] sent = null;
			if (sentFolder != null)
			{
				sent = sentFolder.search(mailside);
			}
			Message[] inbox = emailFolderObj.search(recieveSide);

			Message[] propId = new Message[inbox.length];
			int count = 0;
			boolean remove = false;
			if (inbox.length == 0 || sent.length == 0) // We have never sent them a message before
				return null;
			for (Message m : inbox)// Check to see what messages they have not responded to
			{
				for (Message sntm : sent)
				{
					if (m == null || sntm == null)
						break;
					if (m.getHeader("Message-Id")[0].equals(sntm.getHeader("In-Reply-To")[0]))
					{
						remove = true;
						break;
					}
				}
				if (!remove)
				{
					propId[count++] = m;
				}
				remove = false;
			}
			if (count == 0)// No emails without replys so reply to the last email we replied to.
			{
				Message t = sent[0];
				if (sent.length > 0)
					for (Message m : sent)
					{
						if (t.getReceivedDate().compareTo(m.getReceivedDate()) > 0)
							t = m;
					}
				return t.getHeader("In-Reply-To")[0];
			}
			if (count == 1)// This is perfect. Exactly what we want
			{
				return propId[0].getHeader("Message-Id")[0];
			}
			if (count > 1)// Not very Ideal so we are going to go with the most recent conversation if we
							// can
			{
				Message t = propId[0];
				for (Message m : propId)
				{
					if (m == null)
						break;

					if (t.getReceivedDate().compareTo(m.getReceivedDate()) < 0)
						t = m;
				}
				return t.getHeader("In-Reply-To")[0];
			}

			emailFolderObj.close(false);
			sentFolder.close(false);
		} catch (NoSuchProviderException e)
		{
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.WARNING, e.toString(), e);
			e.printStackTrace();
		} catch (MessagingException e)
		{
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.WARNING, e.toString(), e);
			e.printStackTrace();
		} catch (Exception e)
		{
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			return null;
		}

		try
		{
			storeObj.close();
		} catch (MessagingException e)
		{
			lastError = e;
			lastErrorDate = System.currentTimeMillis();
			LOGGER.log(Level.WARNING, e.toString(), e);
			e.printStackTrace();
		}

		return null;
	}
	
	public boolean isRunning()
	{
		return instance != null ? isRunning : false;
	}
	
	public void setRunning(boolean run)
	{
		if(instance != null)
			isRunning = run;
	}

	@Override
	public String getName()
	{
		return "Mail Server";
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
		sms=SmsServer.getInstance();
		
		return sms != null;
	}
	
	/*
	 * (non-Javadoc)
	 * This will reload all of the related configuration files.
	 */
	public void reload()
	{
		try
		{
			storeObj.close();
		} catch (MessagingException e)
		{
			LOGGER.log(Level.WARNING, "Closing Mail Connection Failed", e);
		}
		load();
		initMailServer();		
	}

	@Override
	public void save()
	{
		MailServerConfig s = new MailServerConfig();
		
		s.setMailservers(defaultServers);
		
		ConfigReader.WriteConf(s, "config/mail.conf");
	}

	@Override
	public void load()
	{
		
		MailServerConfig s = new MailServerConfig();
		
		s = ConfigReader.ReadConf(s.getClass(), "config/mail.conf");
		
		defaultServers = s.getMailservers();
	}

	@Override
	public ObjectNode getConfig()
	{
		ObjectMapper m = new ObjectMapper();
		
		ObjectNode o1 = m.createObjectNode();
		ObjectNode o2 = m.createObjectNode();
		JsonNode node = m.convertValue(defaultServers, JsonNode.class);
		
		o2.set("MailServer", o1);
		o1.set("defaultServer", node);
		
		return o2;
	}
}