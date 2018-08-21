package regex;

public class CommonRegex {
	public static final String PHONE_NUMBER="\\+\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d";
	public static final String EMAIL_ADDRESS = "[^\\.].*[^\\.]@[^\\.].*[^\\.]";
	public static final String MESSAGE_SEPERATOR = "On [0-9]?[0-9]/[0-9]?[0-9]/[0-9][0-9][0-9][0-9] [0-9]?[0-9].[0-9][0-9] (AM|PM), .* wrote.";
}
