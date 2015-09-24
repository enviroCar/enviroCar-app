package org.envirocar.app.model.dao;

import org.envirocar.app.model.User;
import org.envirocar.app.model.UserStatistics;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.model.dao.exception.UserStatisticsRetrievalException;

import rx.Observable;

/**
 * @author dewall
 */
public interface UserStatisticsDAO {

    UserStatistics getUserStatistics(User user)
            throws UserStatisticsRetrievalException, UnauthorizedException;

    Observable<UserStatistics> getUserStatisticsObservable(String user);
}
