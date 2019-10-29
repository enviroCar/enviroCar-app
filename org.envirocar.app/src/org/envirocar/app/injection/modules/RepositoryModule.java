package org.envirocar.app.injection.modules;

import org.envirocar.core.repository.UserStatisticRepository;
import org.envirocar.remote.repository.RemoteUserStatisticRepository;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class RepositoryModule {

    @Provides
    @Singleton
    UserStatisticRepository provideUserStatisticRepository(RemoteUserStatisticRepository repository){
        return repository;
    }
}
