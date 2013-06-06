package car.io.application;

public class User {

	private String username;
	private String token;

	public User(String username, String token) {
		this.username = username;
		this.token = token;
		// TODO write to sharedpreferences
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return the token
	 */
	public String getToken() {
		return token;
	}

	public void logout() {
		// TODO delete in sharedpreferences
	}

}
