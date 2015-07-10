package org.envirocar.app.events;

import com.google.common.base.MoreObjects;

import org.envirocar.app.model.User;

/**
 * @author dewall
 */
public class NewUserSettingsEvent {

    public final boolean mIsLoggedIn;
    public final User mUser;

    /**
     * Constructor.
     *
     * @param user
     * @param isLoggedIn
     */
    public NewUserSettingsEvent(final User user, final boolean isLoggedIn){
        this.mUser = user;
        this.mIsLoggedIn = isLoggedIn;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("User", mUser)
                .add("IsLoggedIn", mIsLoggedIn)
                .toString();
    }
}
