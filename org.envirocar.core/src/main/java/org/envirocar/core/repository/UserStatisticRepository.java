package org.envirocar.core.repository;

import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserStatistic;

import io.reactivex.Observable;

/**
 * @author dewall
 */
public interface UserStatisticRepository {

    /**
     * Get an {@link Observable} which will emit the userstatistic for a given {@link User}.
     *
     * @param user the user to retrieve the statistics for.
     */
    Observable<UserStatistic> user(final User user);

}
    