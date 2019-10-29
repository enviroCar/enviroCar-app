package org.envirocar.core.interactor;

import com.google.common.base.Preconditions;

import org.envirocar.core.EnviroCarDB;
import org.envirocar.core.entity.Track;
import org.envirocar.core.entity.UserStatistic;
import org.envirocar.core.entity.internal.AggregatedUserStatistic;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;
import org.envirocar.core.repository.UserStatisticRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Scheduler;

@Singleton
public class GetAggregatedUserStatistic extends Interactor<AggregatedUserStatistic, GetAggregatedUserStatistic.Params> {

    private final UserStatisticRepository repository;
    private final EnviroCarDB enviroCarDB;

    @Inject
    public GetAggregatedUserStatistic(
            @InjectIOScheduler Scheduler observeOn, @InjectUIScheduler Scheduler subscribeOn,
            UserStatisticRepository repository, EnviroCarDB enviroCarDB) {
        super(observeOn, subscribeOn);
        this.repository = repository;
        this.enviroCarDB = enviroCarDB;
    }

    @Override
    Observable<AggregatedUserStatistic> buildObservable(GetAggregatedUserStatistic.Params params) {
        Preconditions.checkNotNull(params);
        return Observable.create(emitter -> {
            AggregatedUserStatistic result = new AggregatedUserStatistic();

            if (params.username != null) {
                UserStatistic userStatistic = repository.getUserStatistic(params.username).blockingFirst();
                result.setNumTracks(result.getNumTracks() + userStatistic.getTrackCount());
                result.setTotalDistance(result.getTotalDistance() + userStatistic.getDistance());
                result.setTotalDuration(result.getTotalDuration() + userStatistic.getDuration());
            }

            enviroCarDB.getAllLocalTracks(true)
                    .doOnNext(tracks -> {
                        for (Track track : tracks){
                            result.setNumTracks(result.getNumTracks() + 1);
                            result.setTotalDistance(result.getTotalDistance() + track.getLength());
                            result.setTotalDuration(result.getTotalDuration() + track.getDuration());
                        }
                    }).subscribe();

            emitter.onNext(result);
            emitter.onComplete();
        });
    }

    public static final class Params {
        private final String username;

        public Params(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }
    }
}
