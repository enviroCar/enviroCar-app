package org.envirocar.app.injection.modules;

import org.envirocar.core.repository.PrivacyStatementRepository;
import org.envirocar.core.repository.TermsOfUseRepository;
import org.envirocar.core.repository.UserStatisticRepository;
import org.envirocar.remote.repository.RemotePrivacyStatementRepository;
import org.envirocar.remote.repository.RemoteTermsOfUseRepository;
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

    @Provides
    @Singleton
    TermsOfUseRepository provideTermsOfUseReposistory(RemoteTermsOfUseRepository repository){
        return repository;
    }

    @Provides
    @Singleton
    PrivacyStatementRepository providePrivacyStatementRepository(RemotePrivacyStatementRepository repository){
        return repository;
    }
}
