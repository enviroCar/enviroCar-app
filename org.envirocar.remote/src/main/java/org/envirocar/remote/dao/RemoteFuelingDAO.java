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
import org.envirocar.core.dao.FuelingDAO;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.FuelingService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;

/**
 * The data access object for remote fuelings that are stored at the server.
 *
 * @author dewall
 */
public class RemoteFuelingDAO extends BaseRemoteDAO<FuelingDAO, FuelingService> implements
        FuelingDAO {
    private static final Logger LOG = Logger.getLogger(RemoteFuelingDAO.class);

    /**
     * Constructor.
     *
     * @param cacheDao the cache DAO for fuelings.
     */
    @Inject
    public RemoteFuelingDAO(CacheFuelingDAO cacheDao, FuelingService service, UserManager
            userManager) {
        super(cacheDao, service, userManager);
    }

    @Override
    public List<Fueling> getFuelings() throws NotConnectedException, UnauthorizedException {
        LOG.info("getFuelings()");
        try {
            // Execute the call
            return executeCall(remoteService.getFuelings(
                    userManager.getUser().getUsername())).body();
        } catch (ResourceConflictException e) {
            throw new NotConnectedException(e);
        } catch (IOException e) {
            throw new NotConnectedException(e);
        }
    }

    @Override
    public Observable<List<Fueling>> getFuelingsObservable() {
        return Observable.create(emitter -> {
            try {
                emitter.onNext(getFuelings());
            } catch (Exception e) {
                emitter.onError(e);
            }
            emitter.onComplete();
        });
    }

    @Override
    public void createFueling(Fueling fueling) throws NotConnectedException,
            ResourceConflictException, UnauthorizedException {
        LOG.info("createFueling()");
        try {
            // Execute the call
            Response<ResponseBody> response = executeCall(remoteService.uploadFuelings(
                    userManager.getUser().getUsername(), fueling));
            fueling.setRemoteID(
                    EnvirocarServiceUtils.resolveRemtoteID(
                            EnvirocarServiceUtils.resolveRemoteLocation(response)));
        } catch (IOException e) {
            throw new NotConnectedException(e);
        }
    }

    @Override
    public Observable<Void> createFuelingObservable(Fueling fueling) {
        LOG.info("createFuelingObservable()");
        return Observable.create(emitter -> {
            try {
                createFueling(fueling);
            } catch (NotConnectedException | ResourceConflictException |
                    UnauthorizedException e) {
                emitter.onError(e);
            }
            emitter.onComplete();
        });
    }

    @Override
    public void deleteFueling(Fueling fueling) throws NotConnectedException, UnauthorizedException {
        LOG.info(String.format("deleteFueling(%s)", fueling.getRemoteID()));
        try {
            // Execute the call
            executeCall(remoteService
                    .deleteFueling(userManager.getUser().getUsername(),
                            fueling.getRemoteID()));
        } catch (ResourceConflictException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new NotConnectedException(e);
        }
    }

    @Override
    public Observable<Void> deleteFuelingObservable(Fueling fueling) {
        return Observable.create(emitter -> {
            try {
                deleteFueling(fueling);
            } catch (NotConnectedException | UnauthorizedException e) {
                emitter.onError(e);
            }
            emitter.onComplete();
        });
    }
}
