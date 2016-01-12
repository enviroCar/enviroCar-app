package org.envirocar.app.handler;

import android.content.SharedPreferences;

import org.envirocar.core.logging.Logger;
import org.envirocar.remote.DAOProvider;

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
