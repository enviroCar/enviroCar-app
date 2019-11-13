package org.envirocar.core.interactor;

import com.google.common.base.Preconditions;

import org.envirocar.core.entity.UserStatistic;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;
import org.envirocar.core.repository.UserStatisticRepository;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.Scheduler;

/**
 * @author dewall
 */
@Singleton
public class GetUserStatistic extends Interactor<UserStatistic, GetUserStatistic.Params> {

    private final UserStatisticRepository repository;

    /**
     * Cosntructor.
     *
     * @param observeOn   the thread to observe on.
     * @param subscribeOn the thread to subscribe on.
     */
    @Inject
    public GetUserStatistic(UserStatisticRepository repository, @InjectIOScheduler Scheduler observeOn, @InjectUIScheduler Scheduler subscribeOn) {
        super(observeOn, subscribeOn);
        this.repository = repository;
    }

    @Override
    protected Observable<UserStatistic> buildObservable(Params params) {
        Preconditions.checkNotNull(params);
        return this.repository.getUserStatistic(params.username);
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
