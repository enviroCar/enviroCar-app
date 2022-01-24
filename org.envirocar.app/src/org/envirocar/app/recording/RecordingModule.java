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
package org.envirocar.app.recording;

import android.content.Context;
import android.os.PowerManager;

import com.squareup.otto.Bus;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.InterpolationMeasurementProvider;
import org.envirocar.app.handler.TrackUploadHandler;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.interactor.UploadTrack;
import org.envirocar.app.recording.notification.SpeechOutput;
import org.envirocar.app.recording.provider.LocationProvider;
import org.envirocar.app.recording.provider.RecordingDetailsProvider;
import org.envirocar.app.recording.provider.TrackDatabaseSink;
import org.envirocar.app.recording.strategy.GPSRecordingStrategy;
import org.envirocar.app.recording.strategy.OBDRecordingStrategy;
import org.envirocar.app.recording.strategy.RecordingStrategy;
import org.envirocar.app.recording.strategy.obd.OBDConnectionHandler;
import org.envirocar.app.services.trackchunks.TrackchunkUploadService;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.EnviroCarDB;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import org.envirocar.remote.dao.RemoteTrackDAO;

@Module
public class RecordingModule {

    @Provides
    @RecordingScope
    public SpeechOutput provideSpeechOutput(@InjectApplicationScope Context context, Bus eventBus) {
        return new SpeechOutput(context, eventBus);
    }

    @Provides
    @RecordingScope
    public MeasurementProvider provideMeasurementProvider() {
        return new InterpolationMeasurementProvider();
    }

    @Provides
    @RecordingScope
    public TrackDatabaseSink provideTrackDatabaseSink(
            @InjectApplicationScope Context context, CarPreferenceHandler carHandler, EnviroCarDB enviroCarDB, Bus eventBus) {
        return new TrackDatabaseSink(context, carHandler, enviroCarDB, eventBus);
    }

    @Provides
    @RecordingScope
    public OBDConnectionHandler provideOBDConnectionHandler(@InjectApplicationScope Context context) {
        return new OBDConnectionHandler(context);
    }

    @Provides
    @RecordingScope
    public LocationProvider provideLocationProvider(@InjectApplicationScope Context context, Bus eventBus) {
        return new LocationProvider(context, eventBus);
    }

    @Provides
    @RecordingScope
    public TrackchunkUploadService provideTrackchunkUploadService(@InjectApplicationScope Context context, EnviroCarDB enviroCarDB, Bus eventBus, TrackUploadHandler trackUploadHandler) {
        return new TrackchunkUploadService(context, enviroCarDB, eventBus, trackUploadHandler);
    }

//    @Provides
//    @RecordingScope
//    public RecordingNotification provideRecordingNotification(@InjectApplicationScope Context context, Bus eventBus) {
//        return new RecordingNotification(context, eventBus);
//    }

    @Provides
    @RecordingScope
    public PowerManager.WakeLock provideWakeLock(@InjectApplicationScope Context context) {
        return ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "org.envirocar.app:wakelock");
    }

    @Provides
    @RecordingScope
    public RecordingDetailsProvider provideTrackDetailsProvider(Bus eventBus) {
        return new RecordingDetailsProvider(eventBus);
    }

    @Provides
    @RecordingScope
    public RecordingStrategy.Factory provideRecordingStrategyFactory(
            @InjectApplicationScope Context context, Bus eventBus, SpeechOutput speechOutput, BluetoothHandler bluetoothHandler,
            OBDConnectionHandler obdConnectionHandler, MeasurementProvider measurementProvider,
            TrackDatabaseSink trackDatabaseSink, LocationProvider locationProvider, CarPreferenceHandler carPreferenceHandler, TrackchunkUploadService trackchunkUploadService) {
        return () -> {
            RecordingType recordingType = ApplicationSettings.getSelectedRecordingTypeObservable(context).blockingFirst();
            switch (recordingType) {
                default:
                case OBD_ADAPTER_BASED:
                    return new OBDRecordingStrategy(context, eventBus, speechOutput,
                            bluetoothHandler, obdConnectionHandler, measurementProvider,
                            trackDatabaseSink, locationProvider, carPreferenceHandler);
                case ACTIVITY_RECOGNITION_BASED:
                    return new GPSRecordingStrategy(context, eventBus, locationProvider, measurementProvider,
                            trackDatabaseSink, carPreferenceHandler, trackchunkUploadService);
            }
        };
    }
}
