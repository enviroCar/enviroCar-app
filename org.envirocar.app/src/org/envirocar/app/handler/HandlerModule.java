package org.envirocar.app.handler;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * TODO JavaDOc
 *
 * @author dewall
 */
@Module
public class HandlerModule {

    @Provides
    @Singleton
    org.envirocar.core.UserManager provideUserManagerImpl(UserHandler userHandler) {
        return userHandler;
    }

}
