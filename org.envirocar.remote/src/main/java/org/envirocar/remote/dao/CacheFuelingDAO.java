/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.remote.dao;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import org.envirocar.core.dao.AbstractCacheDAO;
import org.envirocar.core.dao.FuelingDAO;
import org.envirocar.core.entity.Announcement;
import org.envirocar.core.entity.Fueling;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.remote.serializer.FuelingSerializer;
import org.reactivestreams.Subscriber;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class CacheFuelingDAO extends AbstractCacheDAO implements FuelingDAO {
    private static final String FUELING_CACHE = "fuelings";

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Fueling.class, new FuelingSerializer()).create();

    @Inject
    public CacheFuelingDAO() {
    }

    @Override
    public List<Fueling> getFuelings() throws DataRetrievalFailureException {
        try {
            return GSON.fromJson(readCache(FUELING_CACHE),
                    new TypeToken<List<Announcement>>() {
                    }.getType());
        } catch (IOException e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<List<Fueling>> getFuelingsObservable() {
        return Observable.create(emitter -> {
            try {
                emitter.onNext(getFuelings());
            } catch (DataRetrievalFailureException e) {
                emitter.onError(e);
            }
            emitter.onComplete();
        });
    }

    @Override
    public void createFueling(Fueling fueling) throws NotConnectedException {
        throw new NotConnectedException("CacheFuelingDAO does not support saving.");
    }

    @Override
    public Observable<Void> createFuelingObservable(Fueling fueling) {
        return Observable.error(new NotConnectedException(
                "CacheFuelingDAO does not support saving."));
    }

    @Override
    public void deleteFueling(Fueling fueling) throws NotConnectedException {
        throw new NotConnectedException("CacheFuelingDAO does not support saving.");
    }

    @Override
    public Observable<Void> deleteFuelingObservable(Fueling fueling) {
        return Observable.error(new NotConnectedException(
                "CacheFuelingDAO does not support saving."));
    }
}
