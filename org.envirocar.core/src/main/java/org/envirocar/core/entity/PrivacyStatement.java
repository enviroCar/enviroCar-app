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

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface PrivacyStatement extends BaseEntity<PrivacyStatement> {
    String KEY_PRIVACY_STATEMENT = "privacyStatements";
    String KEY_PRIVACY_STATEMENT_ID = "id";
    String KEY_PRIVACY_STATEMENT_ISSUEDATE = "issuedDate";
    String KEY_PRIVACY_STATEMENT_CONTENTS = "contents";

    /**
     * Returns the id of the privacy statement.
     *
     * @return the id of the privacy statement.
     */
    String getId();

    /**
     * Sets the id
     *
     * @param id the id
     */
    void setId(String id);

    /**
     * Returns the issued date of the privacy statement as string.
     *
     * @return the issued date of the privacy statement.
     */
    String getIssuedDate();

    /**
     * Sets the issued date of the privacy statement as string.
     *
     * @param issuedDate the issued date of the privacy statement.
     */
    void setIssuedDate(String issuedDate);

    /**
     * Returns the content of the privacy statement.
     *
     * @return the content of the privacy statement.
     */
    String getContents();

    /**
     * Sets the content of the  privacy statement as string.
     *
     * @param content the content of the privacy statement.
     */
    void setContents(String content);
}
