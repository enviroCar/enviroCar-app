package org.envirocar.core;

import org.envirocar.core.entity.User;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface UserManager {
    /**
     * Callback interface for the login process.
     */
    interface LoginCallback {
        /**
         * Called when the specific user has been successfully logged in.
         *
         * @param user the valid {@link User} instance that has been logged in.
         */
        void onSuccess(User user);

        /**
         * Called when the password is incorrect.
         *
         * @param password the incorrect password string.
         */
        void onPasswordIncorrect(String password);

        /**
         * Called when no connection could be established to the server.
         */
        void onUnableToCommunicateServer();
    }

    boolean isLoggedIn();

    void logOut();

    void logIn(String user, String token, LoginCallback callback);

    User getUser();

    void setUser(User user);
}
