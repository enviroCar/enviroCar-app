package org.envirocar.remote.serializer;

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
