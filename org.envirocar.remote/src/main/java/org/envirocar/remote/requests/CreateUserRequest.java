package org.envirocar.remote.requests;

import org.envirocar.core.entity.UserImpl;

/**
 * @author dewall
 */
public class CreateUserRequest extends UserImpl {
    private boolean acceptedTerms;
    private boolean acceptedPrivacy;

    /**
     * @param username
     * @param email
     * @param token
     * @param acceptedTerms
     * @param acceptedPrivacy
     */
    public CreateUserRequest(String username, String email, String token, boolean acceptedTerms, boolean acceptedPrivacy) {
        super(username, token, email);
        this.acceptedTerms = acceptedTerms;
        this.acceptedPrivacy = acceptedPrivacy;
    }

    public boolean isAcceptedTerms() {
        return acceptedTerms;
    }

    public void setAcceptedTerms(boolean acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
    }

    public boolean isAcceptedPrivacy() {
        return acceptedPrivacy;
    }

    public void setAcceptedPrivacy(boolean acceptedPrivacy) {
        this.acceptedPrivacy = acceptedPrivacy;
    }
}
