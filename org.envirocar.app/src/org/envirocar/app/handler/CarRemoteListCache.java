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
package org.envirocar.app.handler;

import android.content.SharedPreferences;

import org.envirocar.core.logging.Logger;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class CarRemoteListCache {
    private static final Logger LOG = Logger.getLogger(CarRemoteListCache.class);

    private static final String TAG_HAS_LIST = "cache_has_list";
    private static final String TAG_CARLIST = "cache_car_list";

    private final SharedPreferences sharedPreferences;
    private final DAOProvider daoProvider;

    @Inject
    public CarRemoteListCache(SharedPreferences sharedPreferences, DAOProvider daoProvider){
        this.sharedPreferences = sharedPreferences;
        this.daoProvider = daoProvider;
    }

//    public Observable<List<Car>> getCachedCars(){
//        return Observable.just(sharedPreferences.getBoolean(TAG_HAS_LIST, false))
//                .flatMap(new Func1<Boolean, Observable<?>>() {
//                    @Override
//                    public Observable<?> call(Boolean aBoolean) {
//                        if(aBoolean && sharedPreferences.get){
//
//                        } else {
//
//                        }
//                    }
//                });
//        return Observable.create(new Observable.OnSubscribe<List<Car>>() {
//            @Override
//            public void call(Subscriber<? super List<Car>> subscriber) {
//
//            }
//        });
//    }
}
