package launch;

import static spark.Spark.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twilio.base.ResourceSet;
import com.twilio.http.TwilioRestClient.Builder;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.api.v2010.account.message.Media;
import json.ParseMessage;
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

		post("/text-room/:room/conversations.json", (req,res) -> {
			
			res.type("application/json");
			String Room = req.params(":room");
			ResourceSet<Message> messages = Message.reader().setFrom("+"+Room).read();
			ArrayList<ParseMessage> cntct = new ArrayList<ParseMessage>();
			
			for(Message m : messages)
			{
				cntct.add(new ParseMessage(m.getTo().toString(),m.getBody(),m.getDateSent().getMillis()));
			}
			
			// Build map of contact to most recent message
			Map<String, ParseMessage> cntctMap = cntct.stream().collect(Collectors.toMap(
			        ParseMessage::getContact,
			        Function.identity(),
			        (a, b) -> a.getDateSent() >= b.getDateSent() ? a : b
			));

			return map.writeValueAsString(cntctMap.values());
		});
		
		post("/text-room/:room/:number" , (req,res) -> {
			
			res.type("application/json");
			
			String Room = req.params(":room");
			String number = req.params(":number");
			
			ResourceSet<Message> messagesTo = Message.reader().setTo(number).setFrom(Room).read().setLimit(10);
			ResourceSet<Message> messagesFrom = Message.reader().setFrom(number).setTo(Room).read().setLimit(10);
			
			
			ArrayList<ParseMessage> messages = new ArrayList<ParseMessage>();
			
			for(Message m : messagesTo) {
				
				if(!m.getNumMedia().equals("0"))
				{
					ArrayList<String> tmp = new ArrayList<String>();
					m.getSubresourceUris().get("media");
					ResourceSet<Media> medias = Media.reader(m.getSid()).read();
					for(Media media : medias)
					{
						tmp.add("https://api.twilio.com"+media.getUri().substring(0, media.getUri().lastIndexOf(".json")));
					}
					messages.add(new ParseMessage("myMessage",m.getBody(),m.getDateSent().getMillis(),tmp));
				}
				else
				{
					messages.add(new ParseMessage("myMessage",m.getBody(),m.getDateSent().getMillis()));
				}

			}
			for(Message m : messagesFrom) {		
				if(!m.getNumMedia().equals("0"))
				{
					ArrayList<String> tmp = new ArrayList<String>();
					m.getSubresourceUris().get("media");
					ResourceSet<Media> medias = Media.reader(m.getSid()).read();
					for(Media media : medias)
					{
						tmp.add("https://api.twilio.com"+media.getUri().substring(0, media.getUri().lastIndexOf(".json")));
					}
					messages.add(new ParseMessage("fromThem",m.getBody(),m.getDateSent().getMillis(),tmp));
				}
				else
				{
					messages.add(new ParseMessage("fromThem",m.getBody(),m.getDateSent().getMillis()));
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
			

			
			return map.writeValueAsString(messages);
		
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
