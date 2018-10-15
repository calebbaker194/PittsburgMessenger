package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.BodyPart;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Flags.Flag;
import javax.mail.Message;
import javax.mail.MessagingException;

import regex.CommonRegex;
import servers.MailEngine;
import servers.SmsServer;
import spark.utils.StringUtils;

public class mailParseTask implements Callable<String> {
	public javax.mail.Message indvidualmsg;
	
	public mailParseTask(Message m)
	{
		indvidualmsg = m;
	}

	public String call() throws InvalidParameterException {
		long t = System.currentTimeMillis();
		String returnVal="";
		System.out.println(Thread.currentThread().getName()+"[MAIL_PARGSER]" + (System.currentTimeMillis() - t));
		try
		{
			if (!(indvidualmsg.getFlags().contains(Flag.ANSWERED)))
			{
				//Set this early on so the next thread doesn't pick up the same one
				indvidualmsg.setFlag(Flag.ANSWERED, true);
				
				returnVal += "Message To: " +indvidualmsg.getAllRecipients()[0].toString();
				// Parse Phone number out of email address.
				String toParser = indvidualmsg.getAllRecipients()[0].toString();
				if (toParser.indexOf('+') > -1)
				{
					String phoneNumber = toParser.substring(toParser.indexOf('+'), toParser.indexOf('@'));
					if (phoneNumber.matches(CommonRegex.PHONE_NUMBER))
					{
						String reply;
						// Get reply with previous email text
						if(indvidualmsg.getContent() instanceof Multipart)
						{
							reply = ((Multipart)indvidualmsg.getContent()).getBodyPart(0).getContent().toString();
						}
						else
						{
							reply = indvidualmsg.getContent().toString();
						}
						// Get email address sent from
						String from = indvidualmsg.getFrom()[0].toString();
						// Strip Additional text from previous emails
						Matcher matcher = Pattern.compile(CommonRegex.MESSAGE_SEPERATOR).matcher(reply);
						// getMatch
						String parsedReply;
						if (matcher.find())
						{
							parsedReply = reply.substring(0, matcher.start());
	
						} else
						{
							parsedReply = reply;
						}
	
						// Get number to send reply from.
						String sendReplyFrom = null;
						String parseFrom = null;
						
						if(from.indexOf('<') > -1 && from.indexOf('>') > -1)
							parseFrom=from.substring(from.indexOf('<')+1, from.indexOf('>'));
						else
							parseFrom=from;
						
						
						sendReplyFrom = Mapper.getNumber(parseFrom);
						
						// Send Text
						if(parsedReply.length()>0)
						{
							SmsServer.getInstance().sendText(phoneNumber, sendReplyFrom != null ? sendReplyFrom : "+19037086135",parsedReply);
						}
						
						// Check For Attachments
						ArrayList<File> attachments = new ArrayList<File>();
						try
						{
							Multipart multipart = (Multipart) indvidualmsg.getContent();
							
							for (int z = 0; z < multipart.getCount(); z++) 
						    {
								BodyPart bodyPart = multipart.getBodyPart(z);
								if(!Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) &&
										StringUtils.isBlank(bodyPart.getFileName())) {
						            continue; // dealing with attachments only
						        } 
						        InputStream is = bodyPart.getInputStream();
						        new File("/tmp/").mkdirs();
						        File f = new File("/tmp/" + bodyPart.getFileName());
						        FileOutputStream fos = new FileOutputStream(f);
						        byte[] buf = new byte[4096];
						        int bytesRead;
						        while((bytesRead = is.read(buf))!=-1) {
						            fos.write(buf, 0, bytesRead);
						        }
						        fos.close();
						        attachments.add(f);
						    }
						}catch(ClassCastException | FileNotFoundException e)
						{
							MailEngine.LOGGER.fine(e.getMessage());
						} catch (IOException e)
						{
							MailEngine.LOGGER.log(Level.WARNING, e.toString(), e);
						}
					    
					    // Iterator through attachments and send them
					    if(!(attachments == null || attachments.size() < 1))
					    {
					    	for(File tempfile : attachments)
					    	{
					    		SmsServer.getInstance().sendText(phoneNumber, sendReplyFrom != null ? sendReplyFrom : "+19037086135", null , tempfile.getName());
					    	}
					    }
						
						// Mark As Answered
						indvidualmsg.setFlag(Flag.ANSWERED, true);
					}
				}
				// Mark mail as answered so that it is no longer parsed by server
				indvidualmsg.setFlag(Flag.ANSWERED, true);			
			}
		} catch (MessagingException e)
		{
			MailEngine.LOGGER.log(Level.WARNING, e.toString(), e);
			e.printStackTrace();
		} catch (IOException e)
		{
			MailEngine.LOGGER.log(Level.WARNING, e.toString(), e);
			e.printStackTrace();
		}
		System.out.println(Thread.currentThread().getName()+"[MAIL_PARGSER]" + (System.currentTimeMillis() - t));
		return returnVal;
	}


}
