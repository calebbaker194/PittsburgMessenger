package server;

public class TwilioAuthData {
	private String account_sid;
	private String auth_token;
	
	public String getAuth_token() {
		return auth_token;
	}
	public void setAuth_token(String auth_token) {
		this.auth_token = auth_token;
	}
	public String getAccount_sid() {
		return account_sid;
	}
	public void setAccount_sid(String account_sid) {
		this.account_sid = account_sid;
	}
}
