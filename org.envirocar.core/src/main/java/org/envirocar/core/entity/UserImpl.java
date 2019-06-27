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
package org.envirocar.core.entity;

import org.envirocar.core.util.VersionRange;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class UserImpl implements User {

    protected String username;
    protected String firstName;
    protected String lastName;
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
    public String getName() {
        String name = this.getFirstName() + " " + this.getLastName();
        return name;
    }

    @Override
    public String getFirstName() {
        return firstName;
    }

    @Override
    public String getLastName() {
        return lastName;
    }

    @Override
    public void setFirstName(String FirstName) {
        this.firstName = FirstName;
    }

    @Override
    public void setLastName(String LastName) {
        this.lastName = LastName;
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
