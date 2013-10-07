package org.envirocar.app.application;

import static android.content.Context.MODE_PRIVATE;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class UserManager {
	
	private static UserManager instance = null;
	
	private static final String USERNAME = "username";
	
	private static final String TOKEN = "token";
	
	private static final String ACCEPTED_TERMS_OF_USE_VERSION = "acceptedTermsOfUseVersion";
	
	private static final String USER_PREFERENCES = "userPrefs";
	
	private Context context;
	
	public UserManager(Context context) {
		this.context = context;
	}

	public static synchronized UserManager instance(){
		if (instance == null) {
			// TODO init first;
		}
		return instance;
	}
	
	public static void init(Context context) {
		if (instance == null) {
			instance = new UserManager(context);
		}
	}
	
	/**
	 * Set the user in the private user preferences
	 * @param user The user you want to set
	 */
	public void setUser(User user) {
		Editor e = getUserPreferences().edit();
		e.putString(USERNAME, user.getUsername());
		e.putString(TOKEN, user.getToken());
		e.putString(ACCEPTED_TERMS_OF_USE_VERSION, user.getAcceptedTermsOfUseVersion());
		e.commit();
	}

	/**
	 * Get the user
	 * @return user
	 */
	public User getUser() {
		SharedPreferences prefs = getUserPreferences();
		String username = prefs.getString(USERNAME, null);
		String token = prefs.getString(TOKEN, null);
		User result = new User(username, token);
		result.setAcceptedTermsOfUseVersion(prefs.getString(ACCEPTED_TERMS_OF_USE_VERSION, null));
		return result;
	}

	/**
	 * Determines whether the user is logged in. A user is logged in when
	 * the application has a user as a variable.
	 * @return
	 */
	public boolean isLoggedIn() {
		SharedPreferences prefs = getUserPreferences();
		if (prefs.contains(USERNAME) && prefs.contains(TOKEN)) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Logs out the user.
	 */
	public void logOut() {
		SharedPreferences prefs = getUserPreferences();
		Editor e = prefs.edit();
		if (prefs.contains(USERNAME))
			e.remove(USERNAME);
		if (prefs.contains(TOKEN))
			e.remove(TOKEN);
		if (prefs.contains(ACCEPTED_TERMS_OF_USE_VERSION))
			e.remove(ACCEPTED_TERMS_OF_USE_VERSION);
		e.commit();
	}

	/**
	 * Get a user object from the shared preferences
	 * @return the user that is stored on the device
	 */
	private SharedPreferences getUserPreferences() {
		SharedPreferences userPrefs = context.getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
		return userPrefs;
	}

}
