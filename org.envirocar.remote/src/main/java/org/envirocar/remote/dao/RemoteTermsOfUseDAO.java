/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.TermsOfUseService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.util.List;

import javax.inject.Inject;

import retrofit.Call;
import retrofit.Response;
import rx.Observable;
import rx.Subscriber;

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
    public TermsOfUse getTermsOfUse(String id) throws DataRetrievalFailureException,
            NotConnectedException {
        LOG.info(String.format("getTermsOfUseInstance(%s)", id));

        // Get the remoteService and initiate the call.
        final TermsOfUseService touService = EnviroCarService.getTermsOfUseService();
        Call<TermsOfUse> termsOfUseCall = touService.getTermsOfUseByID(id);

        try {
            // Execute the call
            Response<TermsOfUse> touResponse = termsOfUseCall.execute();

            // check the response for success
            if (!touResponse.isSuccess()) {
                EnvirocarServiceUtils.assertStatusCode(touResponse.code(), touResponse
                        .message());
            }

            // Store the downloaded instance in the cache.
            //            if (mCache != null) {
            //                mCache.storeTermsOfUseInstance(touResponse.raw().body().string
            // (), id);
            //            }

            // Return the terms of use instance.
            return touResponse.body();
        } catch (Exception e) {
            LOG.warn("Error while retrieving terms of use", e);
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<TermsOfUse> getTermsOfUseObservable(String id) {
        return null;
    }

    @Override
    public List<TermsOfUse> getAllTermsOfUse() throws DataRetrievalFailureException,
            NotConnectedException {
        LOG.info("getTermsOfUse()");
        // Get the remoteService and instantiate the call to the remoteService endpoint in order
        // to get the
        // terms of use.
        final TermsOfUseService touService = EnviroCarService.getTermsOfUseService();
        Call<List<TermsOfUse>> termsOfUseCall = touService.getAllTermsOfUse();

        try {
            // Execute the call
            Response<List<TermsOfUse>> touResponse = termsOfUseCall.execute();

            // check the response for success
            if (!touResponse.isSuccess()) {
                EnvirocarServiceUtils.assertStatusCode(touResponse.code(), touResponse
                        .message());
            }

            // Return the terms of use.
            return touResponse.body();
        } catch (Exception e) {
            LOG.severe("Error while retrieving terms of use.", e);
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<List<TermsOfUse>> getAllTermsOfUseObservable() {
        return Observable.create(new Observable.OnSubscribe<List<TermsOfUse>>() {
            @Override
            public void call(Subscriber<? super List<TermsOfUse>> subscriber) {
                try {
                    subscriber.onNext(getAllTermsOfUse());
                } catch (DataRetrievalFailureException e) {
                    subscriber.onError(e);
                } catch (NotConnectedException e) {
                    subscriber.onError(e);
                }
            }
        });
    }


    //    @Override
    //    public TermsOfUse getTermsOfUse() throws TermsOfUseRetrievalException {
    //        LOG.info("getTermsOfUse()");
    //        // Get the remoteService and instantiate the call to the remoteService endpoint in
    // order to get
    // the
    //        // terms of use.
    //        final TermsOfUseService touService = EnviroCarService.getTermsOfUseService();
    //        Call<TermsOfUse> termsOfUseCall = touService.getTermsOfUse();
    //
    //        try {
    //            // Execute the call
    //            Response<TermsOfUse> touResponse = termsOfUseCall.execute();
    //
    //            // check the response for success
    //            if (touResponse.isSuccess()) {
    //                EnvirocarServiceUtils.assertStatusCode(touResponse.code(), touResponse
    // .message());
    //            }
    //
    //            // Success
    //            if (mCache != null) {
    ////                mCache.storeTermsOfUse(touResponse.body()touResponse.raw().body().string());
    //            }
    //
    //            // Return the terms of use.
    //            return touResponse.body();
    //        } catch (Exception e) {
    //            LOG.severe("Error while retrieving terms of use.", e);
    //            throw new TermsOfUseRetrievalException(e);
    //        }
    //    }
    //
    //    @Override
    //    public Observable<TermsOfUse> getTermsOfUseObservable() {
    //        return Observable.just(true)
    //                .map(aBoolean -> {
    //                    try {
    //                        return getTermsOfUse();
    //                    } catch (TermsOfUseRetrievalException e) {
    //                        throw OnErrorThrowable.from(e);
    //                    }
    //                });
    //    }
    //
    //    @Override
    //    public TermsOfUseInstance getTermsOfUseInstance(String id) throws
    // TermsOfUseRetrievalException {
    //        LOG.info(String.format("getTermsOfUseInstance(%s)", id));
    //
    //        // Get the remoteService and initiate the call.
    //        final TermsOfUseService touService = EnviroCarService.getTermsOfUseService();
    //        Call<TermsOfUseInstance> termsOfUseCall = touService.getTermsOfUseByID(id);
    //
    //        try {
    //            // Execute the call
    //            Response<TermsOfUseInstance> touResponse = termsOfUseCall.execute();
    //
    //            // Store the downloaded instance in the cache.
    //            if (mCache != null) {
    ////                mCache.storeTermsOfUseInstance(touResponse.raw().body().string(), id);
    //            }
    //
    //            // Return the terms of use instance.
    //            return touResponse.body();
    //        } catch (Exception e) {
    //            LOG.warn("Error while retrieving terms of use", e);
    //            throw new TermsOfUseRetrievalException(e);
    //        }
    //    }
    //
    //    @Override
    //    public Observable<TermsOfUseInstance> getTermsOfUseInstanceObservable(final String id) {
    //        return Observable.just(true)
    //                .map(aBoolean -> {
    //                    try {
    //                        return getTermsOfUseInstance(id);
    //                    } catch (TermsOfUseRetrievalException e) {
    //                        throw OnErrorThrowable.from(e);
    //                    }
    //                });
    //    }
}
