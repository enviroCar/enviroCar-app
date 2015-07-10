package org.envirocar.app.application;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.squareup.otto.Bus;

import org.envirocar.app.events.NewUserSettingsEvent;
import org.envirocar.app.injection.InjectApplicationScope;
import org.envirocar.app.injection.Injector;
import org.envirocar.app.model.User;

import javax.inject.Inject;

import dagger.Provides;

import static android.content.Context.MODE_PRIVATE;

public class UserManager {
    private static final String USERNAME = "username";
    private static final String TOKEN = "token";
    private static final String EMAIL = "email";
    private static final String ACCEPTED_TERMS_OF_USE_VERSION = "acceptedTermsOfUseVersion";
    private static final String USER_PREFERENCES = "userPrefs";

    @Inject
    protected Bus mBus;

    @Inject
    @InjectApplicationScope
    protected Context context;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    public UserManager(Context context) {
        // Inject ourselves.
        ((Injector) context).injectObjects(this);
    }

    /**
     * Get the user
     *
     * @return user
     */
    public User getUser() {
        SharedPreferences prefs = getUserPreferences();
        String username = prefs.getString(USERNAME, null);
        String token = prefs.getString(TOKEN, null);
        String mail = prefs.getString(EMAIL, null);
        User result = new User(username, token, mail);
        result.setTouVersion(prefs.getString(ACCEPTED_TERMS_OF_USE_VERSION, null));
        return result;
    }

    /**
     * Set the user in the private user preferences
     *
     * @param user The user you want to set
     */
    public void setUser(User user) {
        Editor e = getUserPreferences().edit();
        e.putString(USERNAME, user.getUsername());
        e.putString(TOKEN, user.getToken());
        e.putString(EMAIL, user.getMail());
        e.putString(ACCEPTED_TERMS_OF_USE_VERSION, user.getTouVersion());
        e.commit();

        mBus.post(new NewUserSettingsEvent(user, true));
    }

    /**
     * Determines whether the user is logged in. A user is logged in when
     * the application has a user as a variable.
     *
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
        if (prefs.contains(EMAIL))
            e.remove(EMAIL);
        if (prefs.contains(ACCEPTED_TERMS_OF_USE_VERSION))
            e.remove(ACCEPTED_TERMS_OF_USE_VERSION);
        e.commit();

        mBus.post(new NewUserSettingsEvent(null, false));
    }

    /**
     * Get a user object from the shared preferences
     *
     * @return the user that is stored on the device
     */
    private SharedPreferences getUserPreferences() {
        SharedPreferences userPrefs = context.getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        return userPrefs;
    }

}
