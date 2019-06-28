/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;

import com.squareup.otto.Bus;

import org.envirocar.app.R;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.core.UserManager;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.util.InjectApplicationScope;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.gravatar.GravatarUtils;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

import static android.content.Context.MODE_PRIVATE;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class UserHandler implements UserManager {
    private static final Logger LOG = Logger.getLogger(UserHandler.class);

    private static final String USERNAME = "username";
    private static final String FIRSTNAME = "firstName";
    private static final String LASTNAME = "lastName";
    private static final String TOKEN = "token";
    private static final String EMAIL = "email";
    private static final String ACCEPTED_TERMS_OF_USE_VERSION = "acceptedTermsOfUseVersion";
    private static final String USER_PREFERENCES = "userPrefs";


    private final Context context;
    private final Bus bus;
    private final DAOProvider daoProvider;

    private User mUser;
    private Bitmap mGravatarBitmap;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    @Inject
    public UserHandler(@InjectApplicationScope Context context, Bus bus, DAOProvider daoProvider) {
        this.context = context;
        this.bus = bus;
        this.daoProvider = daoProvider;
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
            String firstName = prefs.getString(FIRSTNAME, null);
            String lastName = prefs.getString(LASTNAME, null);
            String token = prefs.getString(TOKEN, null);
            String mail = prefs.getString(EMAIL, null);
            mUser = new UserImpl(username, token, mail);
            mUser.setTermsOfUseVersion(prefs.getString(ACCEPTED_TERMS_OF_USE_VERSION, null));
            mUser.setFirstName(firstName);
            mUser.setLastName(lastName);
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
        e.putString(FIRSTNAME, user.getFirstName());
        e.putString(LASTNAME, user.getLastName());
        e.putString(TOKEN, user.getToken());
        e.putString(EMAIL, user.getMail());
        e.putString(ACCEPTED_TERMS_OF_USE_VERSION, user.getTermsOfUseVersion());
        e.commit();

        // Set the local user reference to the current user.
        mUser = user;

        bus.post(new NewUserSettingsEvent(user, true));
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
        return prefs.contains(USERNAME) && prefs.contains(TOKEN);
    }

    public <T> Func1<T, T> getIsLoggedIn() {
        return new Func1<T, T>() {
            @Override
            public T call(T t) {
                if (isLoggedIn())
                    return t;
                else
                    throw OnErrorThrowable.from(new NotLoggedInException(context.getString(R
                            .string.trackviews_not_logged_in)));
            }
        };
    }

    /**
     * Logs out the user.
     */
    @Override
    public void logOut() {
        logOut(false);
    }

    private void logOut(boolean withoutEvent) {
        // Removes all the preferences from the editor.
        SharedPreferences prefs = getUserPreferences();
        Editor e = prefs.edit();
        if (prefs.contains(USERNAME))
            e.remove(USERNAME);
        if (prefs.contains(TOKEN))
            e.remove(TOKEN);
        if (prefs.contains(FIRSTNAME))
            e.remove(FIRSTNAME);
        if (prefs.contains(LASTNAME))
            e.remove(LASTNAME);
        if (prefs.contains(EMAIL))
            e.remove(EMAIL);
        if (prefs.contains(ACCEPTED_TERMS_OF_USE_VERSION))
            e.remove(ACCEPTED_TERMS_OF_USE_VERSION);
        e.commit();

        // Remove the user instance.
        mUser = null;
        mGravatarBitmap = null;

        // Delete all local representations of tracks that are already uploaded.
        //        mTrackRecordingHandler.deleteAllRemoteTracksLocally();

        // Fire a new event on the event bus holding indicating that no logged in user exist.
        if (!withoutEvent) {
            bus.post(new NewUserSettingsEvent(null, false));
        }
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
            User result = daoProvider.getUserDAO().getUser(user);
            result.setToken(token);
            setUser(result);

            // Successfully logged in. Inform the callback about this.
            callback.onSuccess(result);
            return;
        } catch (UnauthorizedException e) {
            LOG.warn(e.getMessage(), e);

            logOut(true);
            // Password is incorrect. Inform the callback about this.
            callback.onPasswordIncorrect(token);
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);

            logOut(true);
            // Unable to communicate with the server. Inform the callback about this.
            callback.onUnableToCommunicateServer();
        }


    }


    public Observable<Bitmap> getGravatarBitmapObservable() {
        return Observable.just(true)
                .map(aBoolean -> {
                    if (isLoggedIn()) {
                        // If the gravatar bitmap already exist, then return it.
                        if (mGravatarBitmap != null)
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
