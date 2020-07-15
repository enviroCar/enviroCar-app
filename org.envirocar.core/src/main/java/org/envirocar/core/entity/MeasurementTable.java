package org.envirocar.core.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import org.envirocar.core.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Blob;

import io.reactivex.functions.Function;

@Entity(tableName = "measurements")
public class MeasurementTable {

    public static final String KEY_TIME = "time";
    public static final String KEY_LONGITUDE = "longitude";
    public static final String KEY_LATITUDE = "latitude";
    public static final String KEY_ROWID = "_id";
    public static final String KEY_PROPERTIES = "properties";
    public static final String KEY_TRACK = "track";

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = KEY_ROWID)
    int rowId;

    @ColumnInfo(name = KEY_LATITUDE)
    String keyLatitude;

    @ColumnInfo(name = KEY_LONGITUDE)
    String keyLongitude;

    @ColumnInfo(name = KEY_PROPERTIES)
    String keyProperties;

    @ColumnInfo(name = KEY_TIME)
    String keyTime;

    @ColumnInfo(name = KEY_TRACK)
    String keyTrack;

    public int getRowId() {
        return rowId;
    }

    public void setRowId(int rowId) {
        this.rowId = rowId;
    }

    public String getKeyLatitude() {
        return keyLatitude;
    }

    public void setKeyLatitude(String keyLatitude) {
        this.keyLatitude = keyLatitude;
    }

    public String getKeyLongitude() {
        return keyLongitude;
    }

    public void setKeyLongitude(String keyLongitude) {
        this.keyLongitude = keyLongitude;
    }

    public String getKeyProperties() {
        return keyProperties;
    }

    public void setKeyProperties(String keyProperties) {
        this.keyProperties = keyProperties;
    }

    public String getKeyTime() {
        return keyTime;
    }

    public void setKeyTime(String keyTime) {
        this.keyTime = keyTime;
    }

    public String getKeyTrack() {
        return keyTrack;
    }

    public void setKeyTrack(String keyTrack) {
        this.keyTrack = keyTrack;
    }

    private static final Logger LOG = Logger.getLogger(MeasurementTable.class);

    public static final Function<? super MeasurementTable, ? extends Measurement> MAPPER = measurementTable ->
            measurementTableToMeasurement(measurementTable);

    private static Measurement measurementTableToMeasurement(MeasurementTable measurementTable) {
        Measurement measurement = new MeasurementImpl();
        measurement.setLatitude(Double.parseDouble(measurementTable.getKeyLatitude()));
        measurement.setLongitude(Double.parseDouble(measurementTable.getKeyLongitude()));
        measurement.setTime(Integer.parseInt(measurementTable.getKeyTime()));
        measurement.setTrackId(new Track.TrackId(Long.parseLong(measurementTable.getKeyTrack())));

        String rawData = measurementTable.getKeyProperties();
        if (rawData != null) {
            try {
                JSONObject json = new JSONObject(rawData);
                JSONArray names = json.names();
                if (names != null) {
                    for (int j = 0; j < names.length(); j++) {
                        String key = names.getString(j);
                        measurement.setProperty(Measurement.PropertyKey.valueOf(key), json
                                .getDouble(key));
                    }
                }
            } catch (JSONException e) {
                LOG.severe("could not load properties", e);
            }
        }
        return measurement;
    }

}
