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


import org.envirocar.core.dao.TermsOfUseDAO;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.TermsOfUseService;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import rx.Observable;

/**
 * Data access object that handles the access to the terms of use at the envirocar remoteService.
 * It uses the {@link TermsOfUseService} to get access to the remoteService endpoint
 *
 * @author dewall
 */
public class RemoteTermsOfUseDAO extends BaseRemoteDAO<TermsOfUseDAO, TermsOfUseService>
        implements TermsOfUseDAO {
    private static final Logger LOG = Logger.getLogger(RemoteTermsOfUseDAO.class);

    /**
     * Constructor.
     *
     * @param cacheDao the DAO for cached terms of use instances.
     */
    @Inject
    public RemoteTermsOfUseDAO(CacheTermsOfUseDAO cacheDao, TermsOfUseService service) {
        super(cacheDao, service);
    }

    @Override
    public TermsOfUse getTermsOfUse(String id) throws DataRetrievalFailureException, NotConnectedException {
        LOG.info("getTermsOfUseInstance(%s)", id);
        Call<TermsOfUse> call = this.remoteService.getTermsOfUseByID(id);
        return wrapExecuteCallReturnBody(call);
    }

    @Override
    public Observable<TermsOfUse> getTermsOfUseObservable(String id) {
        return wrapObservableHandling(() -> getTermsOfUse(id));
    }

    @Override
    public List<TermsOfUse> getAllTermsOfUse() throws DataRetrievalFailureException, NotConnectedException {
        LOG.info("Requesting list of terms of use");
        Call<List<TermsOfUse>> call = this.remoteService.getAllTermsOfUse();
        return wrapExecuteCallReturnBody(call);
    }

    @Override
    public Observable<List<TermsOfUse>> getAllTermsOfUseObservable() {
        return wrapObservableHandling(() -> getAllTermsOfUse());
    }

}
