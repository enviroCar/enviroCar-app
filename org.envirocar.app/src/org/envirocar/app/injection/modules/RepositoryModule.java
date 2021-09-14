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
