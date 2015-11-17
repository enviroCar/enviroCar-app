/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.remote.dao;

import com.squareup.okhttp.ResponseBody;

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

import retrofit.Response;
import rx.Observable;
import rx.Subscriber;

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
        return Observable.create(new Observable.OnSubscribe<List<Fueling>>() {
            @Override
            public void call(Subscriber<? super List<Fueling>> subscriber) {
                try {
                    subscriber.onNext(getFuelings());
                } catch (Exception e) {
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
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
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    createFueling(fueling);
                } catch (NotConnectedException | ResourceConflictException |
                        UnauthorizedException e) {
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
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
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                try {
                    deleteFueling(fueling);
                } catch (NotConnectedException | UnauthorizedException e) {
                    subscriber.onError(e);
                }
                subscriber.onCompleted();
            }
        });
    }
}
