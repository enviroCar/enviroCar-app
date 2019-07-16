package org.envirocar.remote.dao;

import org.envirocar.core.UserManager;
import org.envirocar.core.dao.TrackDAO;
import org.envirocar.core.dao.TrackStatisticsDAO;
import org.envirocar.core.entity.TrackStatistics;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.TrackService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;

import javax.inject.Inject;

import retrofit2.Call;
import retrofit2.Response;
import rx.Observable;
import rx.Subscriber;

public class RemoteTrackStatisticsDAO extends BaseRemoteDAO<TrackDAO, TrackService> implements
        TrackStatisticsDAO {

    /**
     * Constructor.
     *
     * @param cacheDao    the cache dao for tracks.
     * @param trackService the track service.
     */
    @Inject
    public RemoteTrackStatisticsDAO(CacheTrackDAO cacheDao, TrackService trackService, UserManager userManager) {
        super(cacheDao, trackService, userManager);
    }

    @Override
    public TrackStatistics getTrackStatistics(String track) throws DataRetrievalFailureException {
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<TrackStatistics> trackStatistics = trackService.getTrackStatistics(userManager.getUser().getUsername(), track);

        try {
            Response<TrackStatistics> trackStatisticsResponse = trackStatistics.execute();

            if (trackStatisticsResponse.isSuccessful()) {
                return trackStatisticsResponse.body();
            } else {
                // If the execution was successful, then throw an exception.
                int responseCode = trackStatisticsResponse.code();
                EnvirocarServiceUtils.assertStatusCode(responseCode, trackStatisticsResponse
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
    public Observable<TrackStatistics> getTrackStatisticsObservable(String track) {
        return Observable.create(new Observable.OnSubscribe<TrackStatistics>() {
            @Override
            public void call(Subscriber<? super TrackStatistics> subscriber) {
                try {
                    TrackStatistics trackStatistics = getTrackStatistics(track);
                    subscriber.onNext(trackStatistics);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }

    @Override
    public TrackStatistics getTrackStatisticsByPhenomenon(String track, String phenomenon) throws DataRetrievalFailureException {
        final TrackService trackService = EnviroCarService.getTrackService();
        Call<TrackStatistics> trackStatistics = trackService.getTrackStatistics(userManager.getUser().getUsername(), track);

        try {
            Response<TrackStatistics> trackStatisticsResponse = trackStatistics.execute();

            if (trackStatisticsResponse.isSuccessful()) {
                return trackStatisticsResponse.body();
            } else {
                // If the execution was successful, then throw an exception.
                int responseCode = trackStatisticsResponse.code();
                EnvirocarServiceUtils.assertStatusCode(responseCode, trackStatisticsResponse
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
    public Observable<TrackStatistics> getTrackStatisticsByPhenomenonObservable(String track, String phenomenon) {
        return Observable.create(new Observable.OnSubscribe<TrackStatistics>() {
            @Override
            public void call(Subscriber<? super TrackStatistics> subscriber) {
                try {
                    TrackStatistics trackStatistics = getTrackStatisticsByPhenomenon(track, phenomenon);
                    subscriber.onNext(trackStatistics);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        });
    }
}
