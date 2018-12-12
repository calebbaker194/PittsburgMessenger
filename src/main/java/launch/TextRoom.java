package launch;

import static spark.Spark.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.base.Page;
import com.google.common.collect.Range;
import com.twilio.base.ResourceSet;
import com.twilio.http.TwilioRestClient.Builder;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.MessageReader;
import com.twilio.rest.api.v2010.account.message.Media;
import json.ParseMessage;
import org.joda.time.DateTime;
import spark.ModelAndView;
import spark.template.velocity.VelocityTemplateEngine;

public class TextRoom {
	
	private static TextRoom instance = null;
	ObjectMapper map = new ObjectMapper();
	Builder b = new Builder("it@pittsburgsteel.com", "67tonya6767tonya67");
	
	private TextRoom() {
		
		get("/text-room/:room" , (req,res) -> {
			String room = req.params(":room");
			Map<String, Object> model = new HashMap<String, Object>();
			model.put("room",room);
			return new VelocityTemplateEngine().render(
					new ModelAndView(model, "web/text-room.html")
			);
		});
		
		
		get("/text-room" , (req,res) -> {
			res.redirect("/text-room/19037086135");
			return res;
		});

		//Get a list of conversations.
		post("/text-room/:room/conversations.json", (req,res) -> {
			
			res.type("application/json");
			String Room = req.params(":room");
			ResourceSet<Message> messages = Message.reader().setFrom("+"+Room).read();
			ArrayList<ParseMessage> cntct = new ArrayList<ParseMessage>();
			
			for(Message m : messages)
			{
				cntct.add(new ParseMessage(m.getTo().toString(),
										   m.getBody(),
										   m.getDateSent().getMillis(),
										   m.getStatus().toString()));
			}
			
			// Build map of contact to most recent message
			Map<String, ParseMessage> cntctMap = cntct.stream().collect(Collectors.toMap(
			        ParseMessage::getContact,
			        Function.identity(),
			        (a, b) -> a.getDateSent() >= b.getDateSent() ? a : b
			));

			return map.writeValueAsString(cntctMap.values());
		});
		
		//Get The page of messages from a specific number
		post("/text-room/:room/:number" , (req,res) -> {
			
			res.type("application/json");
			
			String Room = req.params(":room");
			String number = req.params(":number");
			String lastRefreshS = req.queryParams("lastUpdate");
			String page = req.queryParams("page");
			
			MessageReader toReader = Message.reader().setTo(number).setFrom(Room);
			MessageReader fromReader = Message.reader().setFrom(number).setTo(Room);
			
			ArrayList<ParseMessage> messages = new ArrayList<ParseMessage>();
			if(lastRefreshS != null)
			{
				long lastRefresh = Long.parseLong(lastRefreshS);
		
				toReader.setDateSent(Range.greaterThan(new DateTime(lastRefresh)));
				fromReader.setDateSent(Range.greaterThan(new DateTime(lastRefresh)));
				ResourceSet<Message> messagesTo = toReader.read();
				ResourceSet<Message> messagesFrom = fromReader.read();
				ArrayList<ResourceSet<Message>> comb = new ArrayList<ResourceSet<Message>>();
				comb.add(messagesTo);
				comb.add(messagesFrom);
				
				for( ResourceSet<Message> ind : comb)
				{
					for(Message m : ind) 
					{
						
						if(!m.getNumMedia().equals("0"))
						{
							ArrayList<String> tmp = new ArrayList<String>();
							m.getSubresourceUris().get("media");
							ResourceSet<Media> medias = Media.reader(m.getSid()).read();
							for(Media media : medias)
							{
								tmp.add("https://api.twilio.com"+media.getUri().substring(0, media.getUri().lastIndexOf(".json")));
							}
							messages.add(new ParseMessage(m.getFrom().toString().equals("+*"+Room)  ? "myMessage" : "fromThem",m.getBody(),m.getDateSent().getMillis(),tmp,m.getStatus().toString()));
						}
						else
						{
							messages.add(new ParseMessage(m.getFrom().toString().equals("+"+Room)  ? "myMessage" : "fromThem",m.getBody(),m.getDateSent().getMillis(),m.getStatus().toString()));
						}
	
					}
				}
			}
			else 
			{
				if(page != null)
				{
					
				}
				Page<Message> messagesTo = toReader.firstPage();
				Page<Message> messagesFrom = fromReader.firstPage();
				
				List<Message> msg = messagesTo.getRecords();
				msg.addAll(messagesFrom.getRecords());
							
				for(Message m : msg) {		
					if(!m.getNumMedia().equals("0"))
					{
						ArrayList<String> tmp = new ArrayList<String>();
						m.getSubresourceUris().get("media");
						ResourceSet<Media> medias = Media.reader(m.getSid()).read();
						for(Media media : medias)
						{
							tmp.add("https://api.twilio.com"+media.getUri().substring(0, media.getUri().lastIndexOf(".json")));
						}
						messages.add(new ParseMessage(m.getFrom().toString().equals("+"+Room)  ? "myMessage" : "fromThem",m.getBody(),m.getDateSent().getMillis(),tmp,m.getStatus().toString()));
					}
					else
					{
						messages.add(new ParseMessage(m.getFrom().toString().equals("+"+Room)  ? "myMessage" : "fromThem",m.getBody(),m.getDateSent().getMillis(),m.getStatus().toString()));
					}
				}
			}
			messages.sort(new Comparator<ParseMessage>() {
				@Override
				public int compare(ParseMessage o1, ParseMessage o2)
				{
					if(o1 == null || o2 == null)
						return 1;
					if(o1.getDateSent() > o2.getDateSent())
						return -1;
					else if(o1.getDateSent() < o2.getDateSent())
						return 1;
					return 0;
				}
			});
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
			long time = cal.getTimeInMillis();
			ArrayList<ParseMessage> remove = new ArrayList<ParseMessage>();
			if(lastRefreshS != null) 
			{
				
				for(ParseMessage p : messages)
				{
					if(p.getDateSent() < time)
						remove.add(p);
				}
				for(ParseMessage p : remove)
				{
					messages.remove(p);
				}
			}
			
			return "{\"lastRefresh\":"+time+" ,\"messages\" : "+map.writeValueAsString(messages)+"}";
		});
		
		// Apply A Filter To The Messages
		post("/text-room/:room/filter/:filter" , (req,res) -> {
			
			res.type("application/json");
			
			String Room = req.params(":room");
			String filter = req.params(":filter");
			
			ResourceSet<Message> messagesTo = Message.reader().setFrom(Room).read();
			ResourceSet<Message> messagesFrom = Message.reader().setTo(Room).read();
			
			Set<String> cntcts = new HashSet<String>();
			
			for(Message m : messagesTo) {
				if(m.getBody().contains(filter))
					cntcts.add(m.getTo().toString());
			}
			for(Message m : messagesFrom) {		
				if(m.getBody().contains(filter))
					cntcts.add(m.getFrom().toString());
			}
			return map.writeValueAsString(cntcts);
		});
	}
	public static TextRoom getInstance()
	{
		if(instance == null)
		{
			instance = new TextRoom();
		}
		return instance;
	}
}
