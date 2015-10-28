package org.envirocar.remote.dao;


import org.envirocar.core.UserManager;
import org.envirocar.core.dao.BaseRemoteDAO;
import org.envirocar.core.dao.UserDAO;
import org.envirocar.core.dao.UserStatisticsDAO;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserStatistics;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.remote.service.EnviroCarService;
import org.envirocar.remote.service.UserService;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;

import javax.inject.Inject;

import retrofit.Call;
import retrofit.Response;
import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class RemoteUserStatisticsDAO extends BaseRemoteDAO<UserDAO, UserService> implements
        UserStatisticsDAO {

    /**
     * Constructor.
     *
     * @param cacheDao    the cache dao for users.
     * @param userService the user service.
     * @param userManager the user manager.
     */
    @Inject
    public RemoteUserStatisticsDAO(CacheUserDAO cacheDao, UserService userService, UserManager
            userManager) {
        super(cacheDao, userService, userManager);
    }

    @Override
    public UserStatistics getUserStatistics(User user) throws DataRetrievalFailureException,
            UnauthorizedException {
        final UserService userService = EnviroCarService.getUserService();
        Call<UserStatistics> userStatistics = userService.getUserStatistics(user.getUsername());

        try {
            Response<UserStatistics> userStatisticsResponse = userStatistics.execute();

            if (userStatisticsResponse.isSuccess()) {
                return userStatisticsResponse.body();
            } else {
                // If the execution was successful, then throw an exception.
                int responseCode = userStatisticsResponse.code();
                EnvirocarServiceUtils.assertStatusCode(responseCode, userStatisticsResponse
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
    public Observable<UserStatistics> getUserStatisticsObservable(String user) {
        return EnviroCarService
                .getUserService()
                .getUserStatisticsObservable(user);
    }
}
