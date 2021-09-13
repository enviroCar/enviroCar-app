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
package org.envirocar.remote.repository;

import org.envirocar.core.InternetAccessProvider;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.repository.TermsOfUseRepository;
import org.envirocar.remote.service.TermsOfUseService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import retrofit2.Call;

/**
 * @author dewall
 */
@Singleton
public class RemoteTermsOfUseRepository extends RemoteRepository<TermsOfUseService> implements TermsOfUseRepository {

    /**
     * Constructor.
     *
     * @param remoteService the created retrofit rest service object.
     */
    @Inject
    public RemoteTermsOfUseRepository(TermsOfUseService remoteService, InternetAccessProvider accessProvider) {
        super(remoteService, accessProvider);
    }

    @Override
    public TermsOfUse getTermsOfUse(String id) throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<TermsOfUse> getTermsOfUseObservable(String id) {
        Call<TermsOfUse> call = remoteService.getTermsOfUseByID(id);
        return wrapObservableHandling(() -> wrapExecuteCallReturnBody(call));
    }

    @Override
    public List<TermsOfUse> getAllTermsOfUse() throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<List<TermsOfUse>> getAllTermsOfUseObservable() {
        Call<List<TermsOfUse>> call = remoteService.getAllTermsOfUse();
        return wrapObservableHandling(() -> wrapExecuteCallReturnBody(call));
    }
}
