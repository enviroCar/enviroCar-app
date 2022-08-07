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
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.repository.PrivacyStatementRepository;
import org.envirocar.remote.service.PrivacyStatementService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import retrofit2.Call;

/**
 * @author dewall
 */
@Singleton
public class RemotePrivacyStatementRepository extends RemoteRepository<PrivacyStatementService> implements PrivacyStatementRepository {

    /**
     * Constructor.
     *
     * @param remoteService the created retrofit rest service object.
     */
    @Inject
    public RemotePrivacyStatementRepository(PrivacyStatementService remoteService, InternetAccessProvider accessProvider) {
        super(remoteService, accessProvider);
    }

    @Override
    public PrivacyStatement getPrivacyStatement(String id) throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<PrivacyStatement> getPrivacyStatementObservable(String id) {
        Call<PrivacyStatement> call = remoteService.getPrivacyStatement(id);
        return wrapObservableHandling(() -> wrapExecuteCallReturnBody(call));
    }

    @Override
    public List<PrivacyStatement> getPrivacyStatements() throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<List<PrivacyStatement>> getPrivacyStatementsObservable() {
        Call<List<PrivacyStatement>> call = remoteService.getPrivacyStatements();
        return wrapObservableHandling(() -> wrapExecuteCallReturnBody(call));
    }
}
