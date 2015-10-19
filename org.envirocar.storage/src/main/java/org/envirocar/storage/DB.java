//package org.envirocar.storage;
//
//import android.content.ContentValues;
//import android.database.Cursor;
//import android.util.Log;
//
//import org.envirocar.core.entity.Car;
//import org.envirocar.core.entity.CarImpl;
//import org.envirocar.core.entity.Measurement;
//import org.envirocar.core.entity.MeasurementImpl;
//import org.envirocar.core.entity.Track;
//import org.envirocar.core.entity.TrackImpl;
//import org.envirocar.core.exception.MeasurementSerializationException;
//import org.envirocar.core.logging.Logger;
//import org.envirocar.core.util.TrackMetadata;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//
//import java.util.Map;
//
///**
// * TODO JavaDoc
// *
// * @author dewall
// */
//public class DB {
//    private static final Logger LOG = Logger.getLogger(DB.class);
//
//    public abstract static class TrackTable {
//        public static final String TABLE_TRACK = "tracks";
//        public static final String KEY_TRACK_ID = "_id";
//        public static final String KEY_TRACK_NAME = "name";
//        public static final String KEY_TRACK_DESCRIPTION = "descr";
//        public static final String KEY_REMOTE_ID = "remoteId";
//        public static final String KEY_TRACK_STATE = "state";
//        public static final String KEY_TRACK_CAR_MANUFACTURER = "car_manufacturer";
//        public static final String KEY_TRACK_CAR_MODEL = "car_model";
//        public static final String KEY_TRACK_CAR_FUEL_TYPE = "fuel_type";
//        public static final String KEY_TRACK_CAR_YEAR = "car_construction_year";
//        public static final String KEY_TRACK_CAR_ENGINE_DISPLACEMENT = "engine_displacement";
//        public static final String KEY_TRACK_CAR_VIN = "vin";
//        public static final String KEY_TRACK_CAR_ID = "carId";
//        public static final String KEY_TRACK_METADATA = "trackMetadata";
//
//        public static final String[] ALL_TRACK_KEYS = new String[]{
//                KEY_TRACK_ID,
//                KEY_TRACK_NAME,
//                KEY_TRACK_DESCRIPTION,
//                KEY_REMOTE_ID,
//                KEY_TRACK_STATE,
//                KEY_TRACK_METADATA,
//                KEY_TRACK_CAR_MANUFACTURER,
//                KEY_TRACK_CAR_MODEL,
//                KEY_TRACK_CAR_FUEL_TYPE,
//                KEY_TRACK_CAR_ENGINE_DISPLACEMENT,
//                KEY_TRACK_CAR_YEAR,
//                KEY_TRACK_CAR_VIN,
//                KEY_TRACK_CAR_ID
//        };
//
//        protected static final String CREATE =
//                "create table " + TABLE_TRACK + " " +
//                        "(" + KEY_TRACK_ID + " INTEGER primary key, " +
//                        KEY_TRACK_NAME + " BLOB, " +
//                        KEY_TRACK_DESCRIPTION + " BLOB, " +
//                        KEY_REMOTE_ID + " BLOB, " +
//                        KEY_TRACK_STATE + " BLOB, " +
//                        KEY_TRACK_METADATA + " BLOB, " +
//                        KEY_TRACK_CAR_MANUFACTURER + " BLOB, " +
//                        KEY_TRACK_CAR_MODEL + " BLOB, " +
//                        KEY_TRACK_CAR_FUEL_TYPE + " BLOB, " +
//                        KEY_TRACK_CAR_ENGINE_DISPLACEMENT + " BLOB, " +
//                        KEY_TRACK_CAR_YEAR + " BLOB, " +
//                        KEY_TRACK_CAR_VIN + " BLOB, " +
//                        KEY_TRACK_CAR_ID + " BLOB);";
//
//        protected static final String DELETE = "DROP TABLE IF EXISTS " + TABLE_TRACK;
//
//        public static ContentValues toContentValues(Track track) {
//            ContentValues values = new ContentValues();
//            if (track.getTrackID() != null && track.getTrackID().getId() != 0) {
//                values.put(KEY_TRACK_ID, track.getTrackID().getId());
//            }
//            values.put(KEY_TRACK_NAME, track.getName());
//            values.put(KEY_TRACK_DESCRIPTION, track.getDescription());
//            if (track.isRemoteTrack()) {
//                values.put(KEY_REMOTE_ID, track.getRemoteID());
//            }
//            values.put(KEY_TRACK_STATE, track.getTrackStatus().toString());
//            if (track.getCar() != null) {
//                values.put(KEY_TRACK_CAR_MANUFACTURER, track.getCar().getManufacturer());
//                values.put(KEY_TRACK_CAR_MODEL, track.getCar().getModel());
//                values.put(KEY_TRACK_CAR_FUEL_TYPE, track.getCar().getFuelType().name());
//                values.put(KEY_TRACK_CAR_ID, track.getCar().getId());
//                values.put(KEY_TRACK_CAR_ENGINE_DISPLACEMENT, track.getCar()
//                        .getEngineDisplacement());
//                values.put(KEY_TRACK_CAR_YEAR, track.getCar().getConstructionYear());
//            }
//
//            if (track.getMetadata() != null) {
//                try {
//                    values.put(KEY_TRACK_METADATA, track.getMetadata().toJsonString());
//                } catch (JSONException e) {
//                    LOG.warn(e.getMessage(), e);
//                }
//            }
//
//            return values;
//        }
//
//        public static Track parseCursor(Cursor c) {
//            Track track = new TrackImpl();
//            track.setTrackID(new Track.TrackId(c.getLong(c.getColumnIndex(KEY_TRACK_ID))));
//            track.setRemoteID(c.getString(c.getColumnIndex(KEY_REMOTE_ID)));
//            track.setName(c.getString(c.getColumnIndex(KEY_TRACK_NAME)));
//            track.setDescription(c.getString(c.getColumnIndex(KEY_TRACK_DESCRIPTION)));
//
//            int statusColumn = c.getColumnIndex(KEY_TRACK_STATE);
//            if (statusColumn != -1) {
//                track.setTrackStatus(Track.TrackStatus.valueOf(c.getString(statusColumn)));
//            } else {
//                track.setTrackStatus(Track.TrackStatus.FINISHED);
//            }
//
//            String metadata = c.getString(c.getColumnIndex(KEY_TRACK_METADATA));
//            if (metadata != null) {
//                try {
//                    track.setMetadata(TrackMetadata.fromJson(c.getString(c.getColumnIndex
//                            (KEY_TRACK_METADATA))));
//                } catch (JSONException e) {
//                    LOG.warn(e.getMessage(), e);
//                }
//            }
//
//            track.setCar(createCarFromCursor(c));
//
//            return track;
//        }
//
//        private static Car createCarFromCursor(Cursor c) {
//            Log.e("tag", "" + c.getString(c.getColumnIndex(KEY_TRACK_CAR_MANUFACTURER)) + " "
//                    + c
//                    .getString(c.getColumnIndex(KEY_TRACK_CAR_ID)));
//            if (c.getString(c.getColumnIndex(KEY_TRACK_CAR_MANUFACTURER)) == null ||
//                    c.getString(c.getColumnIndex(KEY_TRACK_CAR_MODEL)) == null ||
//                    //                c.getString(c.getColumnIndex(KEY_TRACK_CAR_ID)) == null ||
//                    c.getString(c.getColumnIndex(KEY_TRACK_CAR_YEAR)) == null ||
//                    c.getString(c.getColumnIndex(KEY_TRACK_CAR_FUEL_TYPE)) == null ||
//                    c.getString(c.getColumnIndex(KEY_TRACK_CAR_ENGINE_DISPLACEMENT)) == null) {
//                return null;
//            }
//
//            String manufacturer = c.getString(c.getColumnIndex(KEY_TRACK_CAR_MANUFACTURER));
//            String model = c.getString(c.getColumnIndex(KEY_TRACK_CAR_MODEL));
//            String carId = c.getString(c.getColumnIndex(KEY_TRACK_CAR_ID));
//            Car.FuelType fuelType = Car.FuelType.valueOf(c.getString(c.getColumnIndex
//                    (KEY_TRACK_CAR_FUEL_TYPE)));
//            int engineDisplacement = c.getInt(c.getColumnIndex(KEY_TRACK_CAR_ENGINE_DISPLACEMENT));
//            int year = c.getInt(c.getColumnIndex(KEY_TRACK_CAR_YEAR));
//
//            return new CarImpl(carId, manufacturer, model, fuelType, year, engineDisplacement);
//        }
//    }
//
//    public abstract static class MeasurementTable {
//        public static final String TABLE_NAME = "measurements";
//        public static final String KEY_MEASUREMENT_TIME = "time";
//        public static final String KEY_MEASUREMENT_LONGITUDE = "longitude";
//        public static final String KEY_MEASUREMENT_LATITUDE = "latitude";
//        public static final String KEY_MEASUREMENT_ROWID = "_id";
//        public static final String KEY_MEASUREMENT_PROPERTIES = "properties";
//        public static final String KEY_MEASUREMENT_TRACK = "track";
//
//        public static final String[] ALL_MEASUREMENT_KEYS = new String[]{
//                KEY_MEASUREMENT_ROWID,
//                KEY_MEASUREMENT_TIME,
//                KEY_MEASUREMENT_LONGITUDE,
//                KEY_MEASUREMENT_LATITUDE,
//                KEY_MEASUREMENT_PROPERTIES,
//                KEY_MEASUREMENT_TRACK
//        };
//
//        protected static final String CREATE =
//                "create table " + TABLE_NAME + " " +
//                        "(" + KEY_MEASUREMENT_ROWID + " INTEGER primary key autoincrement, " +
//                        KEY_MEASUREMENT_LATITUDE + " BLOB, " +
//                        KEY_MEASUREMENT_LONGITUDE + " BLOB, " +
//                        KEY_MEASUREMENT_TIME + " BLOB, " +
//                        KEY_MEASUREMENT_PROPERTIES + " BLOB, " +
//                        KEY_MEASUREMENT_TRACK + " INTEGER);";
//
//        protected static final String DELETE = "DROP TABLE IF EXISTS " + TABLE_NAME;
//
//        public static ContentValues toContentValues(Measurement measurement) throws
//                MeasurementSerializationException {
//            ContentValues values = new ContentValues();
//            values.put(KEY_MEASUREMENT_LATITUDE, measurement.getLatitude());
//            values.put(KEY_MEASUREMENT_LONGITUDE, measurement.getLongitude());
//            values.put(KEY_MEASUREMENT_TIME, measurement.getTime());
//            values.put(KEY_MEASUREMENT_TRACK, measurement.getTrackId().getId());
//            try {
//                values.put(KEY_MEASUREMENT_PROPERTIES, createPropertiesString(measurement));
//            } catch (JSONException e) {
//                LOG.error("Error while parsing measurement properties.", e);
//                throw new MeasurementSerializationException(e);
//            }
//            return values;
//        }
//
//        private static String createPropertiesString(Measurement measurement) throws JSONException {
//            JSONObject result = new JSONObject();
//            Map<Measurement.PropertyKey, Double> properties = measurement.getAllProperties();
//            for (Measurement.PropertyKey key : properties.keySet()) {
//                result.put(key.name(), properties.get(key));
//            }
//            return result.toString();
//        }
//
//        public static Measurement fromCursor(Cursor c) {
//            Measurement measurement = new MeasurementImpl();
//            measurement.setLatitude(c.getDouble(c.getColumnIndex(KEY_MEASUREMENT_LATITUDE)));
//            measurement.setLongitude(c.getDouble(c.getColumnIndex(KEY_MEASUREMENT_LONGITUDE)));
//            measurement.setTime(c.getLong(c.getColumnIndex(KEY_MEASUREMENT_TIME)));
//            measurement.setTrackId(new Track.TrackId(
//                    c.getLong(c.getColumnIndex(KEY_MEASUREMENT_TRACK))));
//
//            String rawData = c.getString(c.getColumnIndex(KEY_MEASUREMENT_PROPERTIES));
//            if (rawData != null) {
//                try {
//                    JSONObject json = new JSONObject(rawData);
//                    JSONArray names = json.names();
//                    if (names != null) {
//                        for (int j = 0; j < names.length(); j++) {
//                            String key = names.getString(j);
//                            measurement.setProperty(Measurement.PropertyKey.valueOf(key), json
//                                    .getDouble(key));
//                        }
//                    }
//                } catch (JSONException e) {
//                    LOG.severe("could not load properties", e);
//                }
//            }
//            return measurement;
//        }
//    }
//
//}
