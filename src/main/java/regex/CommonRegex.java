package regex;

public class CommonRegex {
	public static final String PHONE_NUMBER="\\+\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d";
	public static final String EMAIL_ADDRESS = "[^\\.].*[^\\.]@[^\\.].*[^\\.]";
	public static final String MESSAGE_SEPERATOR = "On [0-9]?[0-9]/[0-9]?[0-9]/[0-9][0-9][0-9][0-9] [0-9]?[0-9].[0-9][0-9] (AM|PM), .* wrote.";
	public static final String LOCAL_IP = "(192\\.168\\.\\d{1,3}\\.\\d{1,3})|(172\\.16\\.\\d{1,3}\\.\\d{1,3})|(10\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})|(127\\.0\\.0\\.1)";

}
