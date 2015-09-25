/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.model;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Instance holder of a single terms of use.
 *
 * @author dewall
 */
public class TermsOfUseInstance {

    @Deprecated
    public static TermsOfUseInstance fromJson(JSONObject json) throws JSONException {
        String date = json.getString("issuedDate");
        String contents = json.optString("contents", null);
        String id = json.optString("id", null);
        TermsOfUseInstance result = new TermsOfUseInstance(id, date, contents);
        return result;
    }

    @Deprecated
    public static TermsOfUseInstance fromIssuedDate(String date) {
        TermsOfUseInstance result = new TermsOfUseInstance(null, date, null);
        return result;
    }

    private String issuedDate;
    private String contents;
    private String id;

    /**
     * Constructor.
     *
     * @param id         the id of the terms of use instance.
     * @param issuedDate the issued date of the terms of use.
     */
    public TermsOfUseInstance(String id, String issuedDate) {
        this(id, issuedDate, null);
    }

    /**
     * Constructor.
     *
     * @param id         the id of the terms of use instance.
     * @param issuedDate the issued date of the terms of use.
     * @param contents   the terms of use iteself.
     */
    private TermsOfUseInstance(String id, String issuedDate, String contents) {
        this.id = id;
        this.issuedDate = issuedDate;
        this.contents = contents;
    }

    /**
     * Returns the id of the terms of use.
     *
     * @return the id of the terms of use.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the issued date of the terms of use as string.
     *
     * @return the issued date of the terms of use.
     */
    public String getIssuedDate() {
        return issuedDate;
    }

    /**
     * Returns the content of the terms of use.
     *
     * @return the content of the terms of use.
     */
    public String getContents() {
        return contents;
    }

}
