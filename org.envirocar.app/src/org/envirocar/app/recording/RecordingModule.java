package org.envirocar.app.recording;

import android.content.Context;

import com.squareup.otto.Bus;

import org.envirocar.algorithm.MeasurementProvider;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.InterpolationMeasurementProvider;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.recording.notification.SpeechOutput;
import org.envirocar.app.recording.provider.LocationProvider;
import org.envirocar.app.recording.provider.RecordingDetailsProvider;
import org.envirocar.app.recording.provider.TrackDatabaseSink;
import org.envirocar.app.recording.strategy.GPSRecordingStrategy;
import org.envirocar.app.recording.strategy.OBDRecordingStrategy;
import org.envirocar.app.recording.strategy.RecordingStrategy;
import org.envirocar.app.recording.strategy.obd.OBDConnectionHandler;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.storage.EnviroCarDB;

import dagger.Module;
import dagger.Provides;

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

//    @Provides
//    @RecordingScope
//    public RecordingNotification provideRecordingNotification(@InjectApplicationScope Context context, Bus eventBus) {
//        return new RecordingNotification(context, eventBus);
//    }

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
            TrackDatabaseSink trackDatabaseSink, LocationProvider locationProvider, CarPreferenceHandler carPreferenceHandler) {
        return () -> {
            RecordingType recordingType = PreferencesHandler.getSelectedRecordingTypeObservable(context).blockingFirst();
            switch (recordingType) {
                default:
                case OBD_ADAPTER_BASED:
                    return new OBDRecordingStrategy(context, eventBus, speechOutput,
                            bluetoothHandler, obdConnectionHandler, measurementProvider,
                            trackDatabaseSink, locationProvider, carPreferenceHandler);
                case ACTIVITY_RECOGNITION_BASED:
                    return new GPSRecordingStrategy(context, eventBus, locationProvider, measurementProvider,
                            trackDatabaseSink, carPreferenceHandler);
            }
        };
    }
}
