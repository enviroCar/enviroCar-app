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
package org.envirocar.remote.requests;

import org.envirocar.core.entity.UserImpl;

/**
 * @author dewall
 */
public class CreateUserRequest extends UserImpl {
    private String name;
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
        this.name = username;
        this.acceptedTerms = acceptedTerms;
        this.acceptedPrivacy = acceptedPrivacy;
    }

    public String getName() {
        return name;
    }

    public boolean isAcceptedTerms() {
        return acceptedTerms;
    }

    public boolean getAcceptedTerms() {
        return acceptedTerms;
    }

    public void setAcceptedTerms(boolean acceptedTerms) {
        this.acceptedTerms = acceptedTerms;
    }

    public boolean isAcceptedPrivacy() {
        return acceptedPrivacy;
    }

    public boolean getAcceptedPrivacy() {
        return acceptedPrivacy;
    }

    public void setAcceptedPrivacy(boolean acceptedPrivacy) {
        this.acceptedPrivacy = acceptedPrivacy;
    }
}
