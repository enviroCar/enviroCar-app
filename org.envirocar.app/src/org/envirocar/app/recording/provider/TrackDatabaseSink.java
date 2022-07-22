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
package org.envirocar.app.recording.provider;

import android.content.Context;

import com.hwangjr.rxbus.Bus;

import org.envirocar.app.R;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Measurement;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.TrackImpl;
import org.envirocar.core.events.recording.RecordingNewMeasurementEvent;
import org.envirocar.core.exception.MeasurementSerializationException;
import org.envirocar.core.exception.TrackSerializationException;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.TrackMetadata;
import org.envirocar.core.util.Util;
import org.envirocar.core.utils.LocationUtils;
import org.envirocar.core.EnviroCarDB;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableTransformer;

/**
 * @author dewall
 */
public class TrackDatabaseSink {
    private static final Logger LOG = Logger.getLogger(TrackDatabaseSink.class);
    private static final DateFormat format = SimpleDateFormat.getDateTimeInstance();

    private final Context context;
    private final CarPreferenceHandler carHandler;
    private final EnviroCarDB enviroCarDB;
    private final Bus eventBus;
    private Track track;

    /**
     * Constructor.
     *
     * @param context
     * @param carHandler
     * @param enviroCarDB
     */
    public TrackDatabaseSink(Context context, CarPreferenceHandler carHandler, EnviroCarDB enviroCarDB, Bus eventBus) {
        this.context = context;
        this.carHandler = carHandler;
        this.enviroCarDB = enviroCarDB;
        this.eventBus = eventBus;
    }

    /**
     * @return
     */
    public ObservableTransformer<Measurement, Track> storeInDatabase() {
        return upstream -> upstream.flatMap(measurement -> Observable.create((ObservableOnSubscribe<Track>) emitter -> {
            LOG.info("Storing new measurement into database");

            // If no track exists, then create one.
            if (track == null) {
                try {
                    // TODO add default TrackMetadata (app version, measurement profile, NOT at this point: tou)
                    String profile = ApplicationSettings.getCampaignProfile(context);
                    String appVersion = Util.getVersionString(context);
                    TrackMetadata meta = new TrackMetadata(appVersion, null).add(TrackMetadata.MEASUREMENT_PROFILE, profile);


                    track = createNewTrack(measurement.getTime(), meta);
                    
                    emitter.onNext(track);
                } catch (TrackSerializationException e) {
                    LOG.error("Unable to create track instance", e);
                    emitter.onError(e);
                }
            }

            try {
                // inserting measurement
                measurement.setTrackId(track.getTrackID());
                enviroCarDB.insertMeasurement(measurement);

                // updating track information
                track.setEndTime(measurement.getTime());

                // update distance
                int numOfTracks = track.getMeasurements().size();
                if (numOfTracks > 0) {
                    Measurement lastMeasurement = track.getMeasurements().get(numOfTracks - 1);
                    double distanceToLast = LocationUtils.getDistance(lastMeasurement, measurement);
                    track.setLength(track.getLength() + distanceToLast);
                }

                // update track in database
                track.getMeasurements().add(measurement);
                enviroCarDB.updateTrack(track);
                eventBus.post(new RecordingNewMeasurementEvent(measurement));
                LOG.info("Measurement stored");
            } catch (MeasurementSerializationException e) {
                LOG.error(e.getMessage(), e);
                emitter.onError(e);
            }
        }))
                .doOnDispose(() -> finishTrack(track))
                .doOnComplete(() -> finishTrack(track));
    }

    private Track createNewTrack(long startTime, TrackMetadata metadata) throws TrackSerializationException {
        String date = format.format(new Date());
        Car car = carHandler.getCar();

        Track track = new TrackImpl();
        track.setCar(car);
        track.setName("Track " + date);
        track.setDescription(String.format(context.getString(R.string.default_track_description), car != null ? car.getModel() : "null"));
        track.setLength(0.0);
        track.setStartTime(startTime);

        if (metadata != null) {
            track.setMetadata(metadata);
        }

        enviroCarDB.insertTrack(track);
        return track;
    }

    private void finishTrack(Track track) {
        if (track == null)
            return;
        LOG.info(String.format("Finishing current track %s", track.getDescription()));

        if (track.getMeasurements().size() <= 1) {
            LOG.info("Track had not enough measurements. Deleting track.");
            enviroCarDB.deleteTrack(track);
        } else {
            track.setTrackStatus(Track.TrackStatus.FINISHED);
            enviroCarDB.updateTrack(track);
        }
        this.track = null;
    }

}
