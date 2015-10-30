package org.envirocar.app.services;

import com.squareup.otto.Bus;

import org.envirocar.app.events.TrackDetailsProvider;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * @author dewall
 */
@Module(
        complete = false,
        library = true,
        injects = {}
)
public class OBDServiceModule {

//    @Singleton
//    @Provides
//    TextToSpeech provideTextToSpeech(){
//        return new TextToSpeech()
//    }

    @Singleton
    @Provides
    TrackDetailsProvider provideTrackDetails(Bus bus){
        return new TrackDetailsProvider(bus);
    }
}
