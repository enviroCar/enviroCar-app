package org.envirocar.remote.dao;

import org.envirocar.core.UserManager;
import org.envirocar.core.dao.GlobalStatisticsDAO;
import org.envirocar.core.dao.UserDAO;
import org.envirocar.core.entity.GlobalStatistics;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.UserService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;

public class RemoteGlobalStatisticsDAO extends BaseRemoteDAO<UserDAO, UserService> implements
        GlobalStatisticsDAO {

    /**
     * Constructor.
     *
     * @param cacheDao    the cache dao for users.
     * @param userService the user service.
     */
    @Inject
    public RemoteGlobalStatisticsDAO(CacheUserDAO cacheDao, UserService userService, UserManager userManager) {
        super(cacheDao, userService, userManager);
    }

    @Override
    public GlobalStatistics getGlobalStatistics() throws DataRetrievalFailureException {
        final UserService userService = EnviroCarService.getUserService();
        Call<GlobalStatistics> globalStatistics = userService.getGlobalStatistics();

        try {
            Response<GlobalStatistics> globalStatisticsResponse = globalStatistics.execute();

            if (globalStatisticsResponse.isSuccessful()) {
                return globalStatisticsResponse.body();
            } else {
                // If the execution was successful, then throw an exception.
                int responseCode = globalStatisticsResponse.code();
                EnvirocarServiceUtils.assertStatusCode(responseCode, globalStatisticsResponse
                        .errorBody().string());
                return null;
            }

        } catch (IOException e) {
            throw new DataRetrievalFailureException(e);
        } catch (Exception e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<GlobalStatistics> getGlobalStatisticsObservable() {
        return Observable.create(new Observable.OnSubscribe<GlobalStatistics>() {
            @Override
            public void call(Subscriber<? super GlobalStatistics> subscriber) {
                try {
                    GlobalStatistics globalStatistics = getGlobalStatistics();
                    subscriber.onNext(globalStatistics);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    @Override
    public GlobalStatistics getGlobalStatisticsByPhenomenon(String phenomenon) throws DataRetrievalFailureException {
        final UserService userService = EnviroCarService.getUserService();
        Call<GlobalStatistics> globalStatistics = userService.getGlobalStatisticsByPhenomenon(phenomenon);

        try {
            Response<GlobalStatistics> globalStatisticsResponse = globalStatistics.execute();

            if (globalStatisticsResponse.isSuccessful()) {
                return globalStatisticsResponse.body();
            } else {
                // If the execution was successful, then throw an exception.
                int responseCode = globalStatisticsResponse.code();
                EnvirocarServiceUtils.assertStatusCode(responseCode, globalStatisticsResponse
                        .errorBody().string());
                return null;
            }

        } catch (IOException e) {
            throw new DataRetrievalFailureException(e);
        } catch (Exception e) {
            throw new DataRetrievalFailureException(e);
        }
    }

    @Override
    public Observable<GlobalStatistics> getGlobalStatisticsByPhenomenonObservable(String phenomenon) {
        return Observable.create(new Observable.OnSubscribe<GlobalStatistics>() {
            @Override
            public void call(Subscriber<? super GlobalStatistics> subscriber) {
                try {
                    GlobalStatistics globalStatistics = getGlobalStatisticsByPhenomenon(phenomenon);
                    subscriber.onNext(globalStatistics);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}