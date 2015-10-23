package org.envirocar.core.dao;

import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserStatistics;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.UnauthorizedException;

import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface UserStatisticsDAO {

    UserStatistics getUserStatistics(User user)
            throws DataRetrievalFailureException, UnauthorizedException;

    Observable<UserStatistics> getUserStatisticsObservable(String user);
}
