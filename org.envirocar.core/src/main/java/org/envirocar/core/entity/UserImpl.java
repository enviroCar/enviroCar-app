package org.envirocar.core.entity;

import org.envirocar.core.util.VersionRange;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class UserImpl implements User {

    protected String username;
    protected String token;
    protected String mail;
    protected String touVersion;

    /**
     * Default Constructor.
     */
    public UserImpl() {
        this(null, null, null);
    }

    /**
     * Constructor.
     *
     * @param username the name of the user
     * @param token    the password
     */
    public UserImpl(String username, String token) {
        this(username, token, null);
    }

    /**
     * Constructor.
     *
     * @param username the name of the user
     * @param token    the password
     * @param mail     the mail
     */
    public UserImpl(String username, String token, String mail) {
        this.username = username;
        this.token = token;
        this.mail = mail;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getMail() {
        return mail;
    }

    @Override
    public void setMail(String mail) {
        this.mail = mail;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String getTermsOfUseVersion() {
        return touVersion;
    }

    @Override
    public void setTermsOfUseVersion(String termsOfUseVersion) {
        this.touVersion = termsOfUseVersion;
    }

    @Override
    public VersionRange getVersionRange() {
        return null;
    }

    @Override
    public void setVersionRange() {

    }

    @Override
    public User carbonCopy() {
        UserImpl user = new UserImpl();
        user.username = username;
        user.token = token;
        user.touVersion = touVersion;
        user.mail = mail;
        return user;
    }
}
