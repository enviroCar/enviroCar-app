/**
 * Copyright (C) 2013 - 2021 the enviroCar community
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
package org.envirocar.app.handler.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.squareup.otto.Bus;

import org.envirocar.app.exception.LoginException;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.TrackDAOHandler;
import org.envirocar.core.UserManager;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;
import org.envirocar.core.events.NewUserSettingsEvent;
import org.envirocar.core.exception.MailNotConfirmedException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.ResourceConflictException;
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
public class UserPreferenceHandler extends AbstractCachable<User> implements UserManager {
    private static final Logger LOG = Logger.getLogger(UserPreferenceHandler.class);

    private static final String KEY_USERNAME = "username";
    private static final String KEY_FIRSTNAME = "firstName";
    private static final String KEY_LASTNAME = "lastName";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_ACCEPTED_TERMS_OF_USE_VERSION = "acceptedTermsOfUseVersion";
    private static final String KEY_ACCEPTED_PRIVACY_STATEMENT = "acceptedPrivacyStatement";
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
    public UserPreferenceHandler(@InjectApplicationScope Context context, Bus bus, DAOProvider daoProvider, TrackDAOHandler trackDAOHandler) {
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
        user.setPrivacyStatementVersion(prefs.getString(KEY_ACCEPTED_PRIVACY_STATEMENT, null));
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
        if(user.getPrivacyStatementVersion() != null)
            e.putString(KEY_ACCEPTED_PRIVACY_STATEMENT, user.getPrivacyStatementVersion());
        e.commit();
    }

    /**
     * Determines whether the getUserStatistic is logged in. A getUserStatistic is logged in when
     * the application has a getUserStatistic as a variable.
     *
     * @return
     */
    public boolean isLoggedIn() {
        return readFromCache() != null;
    }

    /**
     * Get the getUserStatistic
     *
     * @return getUserStatistic
     */
    public User getUser() {
        return this.readFromCache();
    }

    /**
     * Get the getUserStatistic, but the most recent version from the remote DAO
     *
     * @return getUserStatistic
     */
    public User retrieveUpdatedUser(User user) throws NotConnectedException {
        try {
            User result = daoProvider.getUserDAO().getUser(user.getUsername());
            result.setToken(user.getToken());
            writeToCache(result);
            return result;
        } catch (DataRetrievalFailureException | UnauthorizedException | ResourceConflictException e) {
            LOG.warn(e.getMessage(), e);
            throw new NotConnectedException(e);
        }
    }

    /**
     * Sets the getUserStatistic
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
     * @param token getUserStatistic token
     * @return
     */
    public Completable logIn(String user, String token) {
        return this.logIn(user, token, true);
    }

    public Completable logIn(String user, String token, boolean withEvent) {
        return Completable.create(emitter -> {
            LOG.info("Trying to login getUserStatistic %s".format(user));
            User candidateUser = new UserImpl(user, token);

            // hack
            writeToCache(candidateUser);

            boolean success = false;
            try {
                User result = daoProvider.getUserDAO().getUser(user);
                result.setToken(token);
                writeToCache(result);

                // Successfully logged in.
                success = true;
                emitter.onComplete();
                if (withEvent) {
                    bus.post(new NewUserSettingsEvent(result, true));
                }
            } catch (MailNotConfirmedException e) {
                LOG.warn(e.getMessage(), e);
                emitter.onError(new LoginException(e.getMessage(), LoginException.ErrorType.MAIL_NOT_CONFIREMED));
            } catch (UnauthorizedException e) {
                LOG.warn(e.getMessage(), e);
                // UnauthorizedException can be either due to Incorrect password, Incorrect Username or both.
                // Hence, set error to both password and username
                emitter.onError(new LoginException(e.getMessage(), LoginException.ErrorType.USERNAME_OR_PASSWORD_INCORRECT));
            } catch (NotConnectedException e) {
                LOG.warn(e.getMessage(), e);
                // UnauthorizedException can be either due to Incorrect password, Incorrect Username or both.
                // Hence, set error to both password and username
                if (e.getMessage().contains("Legal reasons response")) {
                    emitter.onError(new LoginException(e.getMessage(), LoginException.ErrorType.TERMS_NOT_ACCEPTED));
                } else {
                    emitter.onError(e);    
                }
            } catch (Exception e) {
                LOG.warn(e.getMessage(), e);
                emitter.onError(e);
            } finally {
                if (!success) {
                    logoutInternal(false);
                }
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
            logoutInternal(withEvent);
            emitter.onComplete();
        });
    }

    private void logoutInternal(Boolean withEvent) {
        LOG.info("Logging out current getUserStatistic.");
        // Removes all the preferences from the editor.
        resetCache();

        // Delete all local representations of tracks that are already uploaded.
        trackDAOHandler.deleteAllRemoteTracksLocally();

        // Fire a new event on the event bus holding indicating that no logged in getUserStatistic exist.
        if (withEvent) {
            bus.post(new NewUserSettingsEvent(null, false));
        }
    }
}
