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
package org.envirocar.core.entity;

/**
 * @author dewall
 */
public class TermsOfUseImpl implements TermsOfUse {
    protected String id;
    protected String issuedDate;
    protected String contents;

    public TermsOfUseImpl() {
    }

    public TermsOfUseImpl(String id, String issuedDate, String contents) {
        this.id = id;
        this.issuedDate = issuedDate;
        this.contents = contents;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIssuedDate() {
        return issuedDate;
    }

    public void setIssuedDate(String issuedDate) {
        this.issuedDate = issuedDate;
    }

    public String getContents() {
        return contents;
    }

    public void setContents(String contents) {
        this.contents = contents;
    }

    @Override
    public TermsOfUse carbonCopy() {
        TermsOfUseImpl res = new TermsOfUseImpl();
        res.id = id;
        res.issuedDate = issuedDate;
        res.contents = contents;
        return res;
    }
}
