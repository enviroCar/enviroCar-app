/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.handler;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.squareup.otto.Bus;

import org.envirocar.core.UserManager;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Completable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class UserHandler extends AbstractCachable<User> implements UserManager {
    private static final Logger LOG = Logger.getLogger(UserHandler.class);

    private static final String KEY_USERNAME = "username";
    private static final String KEY_FIRSTNAME = "firstName";
    private static final String KEY_LASTNAME = "lastName";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ACCEPTED_TERMS_OF_USE_VERSION = "acceptedTermsOfUseVersion";
    private static final String KEY_USER_PREFERENCES = "userPrefs";


    private final Bus bus;
    private final DAOProvider daoProvider;
    private final TrackDAOHandler trackDAOHandler;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    @Inject
    public UserHandler(@InjectApplicationScope Context context, Bus bus, DAOProvider daoProvider, TrackDAOHandler trackDAOHandler) {
        super(context, KEY_USER_PREFERENCES);
        this.bus = bus;
        this.daoProvider = daoProvider;
        this.trackDAOHandler = trackDAOHandler;

        this.bus.register(this);
    }

    @Override
    protected User readFromCache(SharedPreferences prefs) {
        if (!prefs.contains(KEY_USERNAME))
            return null;

        String username = prefs.getString(KEY_USERNAME, null);
        String firstName = prefs.getString(KEY_FIRSTNAME, null);
        String lastName = prefs.getString(KEY_LASTNAME, null);
        String token = prefs.getString(KEY_TOKEN, null);
        String mail = prefs.getString(KEY_EMAIL, null);

        User user = new UserImpl(username, token, mail, firstName, lastName);
        user.setTermsOfUseVersion(prefs.getString(KEY_ACCEPTED_TERMS_OF_USE_VERSION, null));
        return user;
    }

    @Override
    protected void writeToCache(User user, SharedPreferences prefs) {
        Editor e = prefs.edit();
        e.putString(KEY_USERNAME, user.getUsername());
        e.putString(KEY_FIRSTNAME, user.getFirstName());
        e.putString(KEY_LASTNAME, user.getLastName());
        e.putString(KEY_TOKEN, user.getToken());
        e.putString(KEY_EMAIL, user.getMail());
        e.putString(KEY_ACCEPTED_TERMS_OF_USE_VERSION, user.getTermsOfUseVersion());
        e.commit();
    }

    /**
     * Determines whether the user is logged in. A user is logged in when
     * the application has a user as a variable.
     *
     * @return
     */
    public boolean isLoggedIn() {
        return readFromCache() != null;
    }

    /**
     * Get the user
     *
     * @return user
     */
    public User getUser() {
        return this.readFromCache();
    }

    /**
     * Sets the user
     *
     * @param user
     */
    public void setUser(User user) {
        this.writeToCache(user);
    }

    /**
     * Handles the login as a completable
     *
     * @param user  username
     * @param token user token
     * @return
     */
    public Completable logIn(String user, String token) {
        return this.logIn(user, token, true);
    }

    public Completable logIn(String user, String token, boolean withEvent) {
        return Completable.create(emitter -> {
            LOG.info("Trying to login user %s".format(user));
            User candidateUser = new UserImpl(user, token);

            // hack
            writeToCache(candidateUser);

            try {
                User result = daoProvider.getUserDAO().getUser(user);
                result.setToken(token);
                writeToCache(result);

                // Successfully logged in.
                emitter.onComplete();
                if (withEvent)
                    bus.post(new NewUserSettingsEvent(result, true));
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
                logOut(true);
                emitter.onError(e);
            }
        });
    }

    /**
     * Handles the logout procedure
     *
     * @return a completable handling the logout
     */
    public Completable logOut() {
        return logOut(true);
    }

    public Completable logOut(Boolean withEvent) {
        return Completable.create(emitter -> {
            LOG.info("Logging out current user.");
            // Removes all the preferences from the editor.
            resetCache();

            // Delete all local representations of tracks that are already uploaded.
            trackDAOHandler.deleteAllRemoteTracksLocally();

            //set complete
            emitter.onComplete();

            // Fire a new event on the event bus holding indicating that no logged in user exist.
            if (withEvent) {
                bus.post(new NewUserSettingsEvent(null, false));
            }
        });
    }
}
