/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface TermsOfUse extends BaseEntity<TermsOfUse> {
    String KEY_TERMSOFUSE = "termsOfUse";
    String KEY_TERMSOFUSE_ID = "id";
    String KEY_TERMSOFUSE_ISSUEDDATE = "issuedDate";
    String KEY_TERMSOFUSE_CONTENTS = "contents";
    /**
     * Returns the id of the terms of use.
     *
     * @return the id of the terms of use.
     */
    String getId();

    void setId(String id);

    /**
     * Returns the issued date of the terms of use as string.
     *
     * @return the issued date of the terms of use.
     */
    String getIssuedDate();

    void setIssuedDate(String issuedDate);

    /**
     * Returns the content of the terms of use.
     *
     * @return the content of the terms of use.
     */
    String getContents();

    void setContents(String content);
}
