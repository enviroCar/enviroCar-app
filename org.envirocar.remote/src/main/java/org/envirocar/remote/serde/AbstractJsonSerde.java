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
package org.envirocar.remote.serde;

import com.google.gson.JsonObject;

import org.envirocar.core.logging.Logger;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author dewall
 */
public abstract class AbstractJsonSerde {
    private static final Logger LOG = Logger.getLogger(AbstractJsonSerde.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    protected Long parseStringAsTime(String key, JsonObject o) {
        try {
            if (o.has(key)) {
                String time = o.get(key).getAsString();
                return DATE_FORMAT.parse(time).getTime();
            }
        } catch (Exception e) {
            LOG.error("Error while parsing date.", e);
        }
        return null;
    }

    protected Double parseAsDouble(String key, JsonObject o){
        try {
            if(o.has(key)){
                return o.get(key).getAsDouble();
            }
        } catch (Exception e){
            LOG.error("Error while parsing double value.", e);
        }
        return null;
    }
}
