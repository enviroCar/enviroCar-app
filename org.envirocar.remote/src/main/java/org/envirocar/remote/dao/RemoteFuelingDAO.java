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
import org.envirocar.core.dao.BaseRemoteDAO;
import org.envirocar.core.dao.FuelingDAO;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.FuelingService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import retrofit.Call;
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

        Call<List<Fueling>> getFuelingsCall = remoteService.getFuelings(
                userManager.getUser().getUsername());

        try {
            Response<List<Fueling>> getFuelingsResponse = getFuelingsCall.execute();

            // assert the responsecode if it was not an success.
            if (!getFuelingsResponse.isSuccess()) {
                EnvirocarServiceUtils.assertStatusCode(getFuelingsResponse.code(),
                        getFuelingsResponse.message());
            }

            return getFuelingsResponse.body();
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

        // Instantiate the fueling remoteService and the upload fueling call
        final FuelingService fuelingService = EnviroCarService.getFuelingService();
        Call<ResponseBody> uploadFuelingCall = fuelingService.uploadFuelings(
                userManager.getUser().getUsername(), fueling);

        try {
            // Execute the call
            Response<ResponseBody> uploadFuelingResponse = uploadFuelingCall.execute();

            // assert the responsecode if it was not an success.
            if (!uploadFuelingResponse.isSuccess()) {
                EnvirocarServiceUtils.assertStatusCode(uploadFuelingResponse.code(),
                        uploadFuelingResponse.message());
            }
        } catch (IOException e) {
            throw new NotConnectedException(e);
        }
    }
}
