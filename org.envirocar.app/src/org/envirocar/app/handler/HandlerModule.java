package org.envirocar.app.handler;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * TODO JavaDOc
 *
 * @author dewall
 */
@Module(
        complete = false,
        library = true,
        injects = {
                BluetoothHandler.class,
                CarPreferenceHandler.class,
                LocationHandler.class,
                TemporaryFileManager.class,
                TermsOfUseManager.class,
                TrackDAOHandler.class,
                TrackRecordingHandler.class,
                TrackUploadHandler.class,
                UserHandler.class
        }
)
public class HandlerModule {

    @Provides
    @Singleton
    org.envirocar.core.UserManager provideUserManagerImpl(UserHandler userHandler) {
        return userHandler;
    }

}
