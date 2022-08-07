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
package org.envirocar.remote.dao;

import org.envirocar.core.UserManager;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;

import io.reactivex.rxjava3.core.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class BaseRemoteDAO<Cache, Service> {
    private static final Logger LOG = Logger.getLogger(BaseRemoteDAO.class);

    protected final Cache cacheDao;
    protected final Service remoteService;
    protected final UserManager userManager;

    /**
     * Wrapper for calling a function with an exception.
     */
    interface FuncWithException<T> {
        T call() throws Exception;
    }

    /**
     * Constructor.
     *
     * @param cacheDao      the caching dao.
     * @param remoteService the remote service of this dao.
     */
    public BaseRemoteDAO(Cache cacheDao, Service remoteService) {
        this(cacheDao, remoteService, null);
    }

    /**
     * Constructor.
     *
     * @param cacheDao      the cache dao
     * @param remoteService the remote service of this dao
     * @param userManager   the getUserStatistic manager
     */
    public BaseRemoteDAO(Cache cacheDao, Service remoteService, UserManager userManager) {
        this.userManager = userManager;
        this.cacheDao = cacheDao;
        this.remoteService = remoteService;
    }

    protected <T> Response<T> executeCall(Call<T> call) throws IOException,
            NotConnectedException, UnauthorizedException, ResourceConflictException {
        Response<T> response = call.execute();

        // assert the responsecode if it was not an success.
        if (!response.isSuccessful()) {
            ResponseBody body = response.errorBody();
            EnvirocarServiceUtils.assertStatusCode(response.code(), response.message(), body.string(), call.request().url().toString());
        }

        return response;
    }

    protected <T> T executeCallReturnBody(Call<T> call) throws IOException,
            NotConnectedException, UnauthorizedException, ResourceConflictException {
        return this.executeCall(call).body();
    }

    protected <T> T wrapExecuteCallReturnBody(Call<T> call) throws DataRetrievalFailureException, NotConnectedException {
        try {
            return executeCallReturnBody(call);
        } catch (IOException | UnauthorizedException | ResourceConflictException e) {
            LOG.error(String.format("Error while requesting %s", remoteService.getClass().getSimpleName()), e);
            throw new DataRetrievalFailureException(e);
        }
    }

    protected <T> Observable<T> wrapObservableHandling(FuncWithException<T> func) {
        return Observable.create(subscriber -> {
            try {
                subscriber.onNext(func.call());
                subscriber.onComplete();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }

}
