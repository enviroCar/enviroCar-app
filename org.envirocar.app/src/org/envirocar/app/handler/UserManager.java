package org.envirocar.app.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;

import com.squareup.otto.Bus;

import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;
import org.envirocar.app.injection.DAOProvider;
import org.envirocar.remote.gravatar.GravatarUtils;

import java.io.IOException;

import javax.inject.Inject;

import rx.Observable;

import static android.content.Context.MODE_PRIVATE;

public class UserManager implements org.envirocar.core.UserManager {
    private static final Logger LOG = Logger.getLogger(UserManager.class);

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

    @Inject
    protected DAOProvider mDAOProvider;

    private User mUser;
    private Bitmap mGravatarBitmap;

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
    @Override
    public User getUser() {
        if (mUser == null) {
            SharedPreferences prefs = getUserPreferences();
            String username = prefs.getString(USERNAME, null);
            String token = prefs.getString(TOKEN, null);
            String mail = prefs.getString(EMAIL, null);
            mUser = new UserImpl(username, token, mail);
            mUser.setTermsOfUseVersion(prefs.getString(ACCEPTED_TERMS_OF_USE_VERSION, null));
        }
        return mUser;
    }

    /**
     * Set the user in the private user preferences
     *
     * @param user The user you want to set
     */
    @Override
    public void setUser(User user) {
        // First set the user in the preferences
        Editor e = getUserPreferences().edit();
        e.putString(USERNAME, user.getUsername());
        e.putString(TOKEN, user.getToken());
        e.putString(EMAIL, user.getMail());
        e.putString(ACCEPTED_TERMS_OF_USE_VERSION, user.getTermsOfUseVersion());
        e.commit();

        // Set the local user reference to the current user.
        mUser = user;

        mBus.post(new NewUserSettingsEvent(user, true));
    }

    /**
     * Determines whether the user is logged in. A user is logged in when
     * the application has a user as a variable.
     *
     * @return
     */
    @Override
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
    @Override
    public void logOut() {
        // Removes all the preferences from the editor.
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

        // Remove the user instance.
        mUser = null;
        mGravatarBitmap = null;

        // Delete all local representations of tracks that are already uploaded.
//        mTrackHandler.deleteAllRemoteTracksLocally();

        // Fire a new event on the event bus holding indicating that no logged in user exist.
        mBus.post(new NewUserSettingsEvent(null, false));
    }

    /**
     * Method used for authentication (e.g. at loginscreen to verify user
     * credentials
     */
    public void logIn(String user, String token, LoginCallback callback) {
        User currentUser = getUser();

        if (currentUser == null || currentUser.getToken() == null) {
            User candidateUser = new UserImpl(user, token);
            setUser(candidateUser);
        }

        try {
            User result = mDAOProvider.getUserDAO().getUser(user);
            result.setToken(token);
            setUser(result);

            // Successfully logged in. Inform the callback about this.
            callback.onSuccess(result);
            return;
        } catch (UnauthorizedException e) {
            LOG.warn(e.getMessage(), e);
            // Password is incorrect. Inform the callback about this.
            callback.onPasswordIncorrect(token);
        } catch (DataRetrievalFailureException e) {
            LOG.warn(e.getMessage(), e);
            // Unable to communicate with the server. Inform the callback about this.
            callback.onUnableToCommunicateServer();
        }

        // If any exception has been thrown, then set the state to logged out.
        logOut();
    }


    public Observable<Bitmap> getGravatarBitmapObservable() {
        return Observable.just(true)
                .map(aBoolean -> {
                    if (isLoggedIn()) {
                        // If the gravatar bitmap already exist, then return it.
                        if(mGravatarBitmap != null)
                            return mGravatarBitmap;

                        // Else try to download the bitmap.
                        // But first check whether all required credentials are valid.
                        User user = getUser();
                        String mail = user.getMail();
                        if (mail == null || mail.equals("") || mail.isEmpty())
                            return null;

                        // Try to download the bitmap.
                        try {
                            mGravatarBitmap = GravatarUtils.downloadBitmap(user.getMail());
                            return mGravatarBitmap;
                        } catch (IOException e) {
                            LOG.warn("Error while downloading Gravatar bitmap.", e);
                            e.printStackTrace();
                        }
                    }

                    return null;
                });
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
