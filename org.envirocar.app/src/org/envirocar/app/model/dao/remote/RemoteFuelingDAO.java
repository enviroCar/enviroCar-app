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
package org.envirocar.app.model.dao.remote;

import com.squareup.okhttp.ResponseBody;

import org.apache.http.client.methods.HttpPost;
import org.envirocar.app.ConstantsEnvirocar;
import org.envirocar.app.exception.InvalidObjectStateException;
import org.envirocar.app.json.FuelingEncoder;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.model.Fueling;
import org.envirocar.app.model.dao.FuelingDAO;
import org.envirocar.app.model.dao.cache.CacheFuelingDAO;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.ResourceConflictException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.model.dao.service.EnviroCarService;
import org.envirocar.app.model.dao.service.FuelingService;
import org.envirocar.app.model.dao.service.utils.EnvirocarServiceUtils;
import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;

import retrofit.Call;
import retrofit.Response;
import rx.Observable;

/**
 * The data access object for remote fuelings that are stored at the server.
 *
 * @author dewall
 */
public class RemoteFuelingDAO extends BaseRemoteDAO implements FuelingDAO, AuthenticatedDAO {
    private static final Logger LOG = Logger.getLogger(RemoteFuelingDAO.class);

    private CacheFuelingDAO cache;

    /**
     * Constructor.
     *
     * @param cacheFuelingDAO the cache DAO for fuelings.
     */
    public RemoteFuelingDAO(CacheFuelingDAO cacheFuelingDAO) {
        this.cache = cacheFuelingDAO;
    }

    @Override
    public void storeFueling(Fueling fueling) throws NotConnectedException,
            InvalidObjectStateException, UnauthorizedException {
        LOG.info("storeFueling()");

        // Instantiate the fueling service and the upload fueling call
        final FuelingService fuelingService = EnviroCarService.getFuelingService();
        Call<ResponseBody> uploadFuelingCall = fuelingService.uploadFuelings(
                mUserManager.getUser().getUsername(), fueling);

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
        } catch (ResourceConflictException e) {
            throw new InvalidObjectStateException(e.getMessage());
        }
    }

    @Override
    public List<Fueling> getFuelings() {
        // TODO implement
        return Collections.emptyList();
    }

    @Override
    public Observable<List<Fueling>> getFuelingsObservable() {
        // TODO implement
        return Observable.from(Collections.emptyList());
    }

}
