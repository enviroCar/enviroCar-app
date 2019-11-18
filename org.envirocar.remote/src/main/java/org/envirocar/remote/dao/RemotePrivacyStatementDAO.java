/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.remote.dao;

import org.envirocar.core.repository.PrivacyStatementRepository;
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.PrivacyStatementService;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import retrofit2.Call;

/**
 * @author dewall
 */
public class RemotePrivacyStatementDAO extends BaseRemoteDAO<PrivacyStatementRepository, PrivacyStatementService> implements PrivacyStatementRepository {
    private static final Logger LOG = Logger.getLogger(RemotePrivacyStatementDAO.class);

    /**
     * Constructor.
     *
     * @param cacheDao the DAO for cached items #TODO remove
     * @param service
     */
    @Inject
    public RemotePrivacyStatementDAO(CachePrivacyStatementDAO cacheDao, PrivacyStatementService service) {
        super(cacheDao, service);
    }


    @Override
    public PrivacyStatement getPrivacyStatement(String id) throws DataRetrievalFailureException, NotConnectedException {
        LOG.info("Requesting Privacy Statement for id=%s", id);
        Call<PrivacyStatement> call = this.remoteService.getPrivacyStatement(id);
        return wrapExecuteCallReturnBody(call);
    }

    @Override
    public Observable<PrivacyStatement> getPrivacyStatementObservable(String id) {
        return wrapObservableHandling(() -> getPrivacyStatement(id));
    }

    @Override
    public List<PrivacyStatement> getPrivacyStatements() throws DataRetrievalFailureException, NotConnectedException {
        LOG.info("Requesting list of privacy statements");
        Call<List<PrivacyStatement>> call = this.remoteService.getPrivacyStatements();
        return wrapExecuteCallReturnBody(call);
    }

    @Override
    public Observable<List<PrivacyStatement>> getPrivacyStatementsObservable() {
        return wrapObservableHandling(() -> getPrivacyStatements());
    }
}
