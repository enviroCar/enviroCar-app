package org.envirocar.app.model.dao.remote;

import org.envirocar.app.model.User;
import org.envirocar.app.model.UserStatistics;
import org.envirocar.app.model.dao.UserStatisticsDAO;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.model.dao.exception.UserStatisticsRetrievalException;
import org.envirocar.app.model.dao.service.EnviroCarService;
import org.envirocar.app.model.dao.service.UserService;
import org.envirocar.app.model.dao.service.utils.EnvirocarServiceUtils;

import java.io.IOException;

import retrofit.Call;
import retrofit.Response;
import rx.Observable;

/**
 * @author dewall
 */
public class RemoteUserStatisticsDAO extends BaseRemoteDAO
        implements UserStatisticsDAO, AuthenticatedDAO {

    @Override
    public UserStatistics getUserStatistics(User user)
            throws UserStatisticsRetrievalException, UnauthorizedException {
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
            throw new UserStatisticsRetrievalException(e);
        } catch (Exception e) {
            throw new UserStatisticsRetrievalException(e);
        }
    }

    @Override
    public Observable<UserStatistics> getUserStatisticsObservable(String user) {
        return EnviroCarService.getUserService()
                .getUserStatisticsObservable(user);
    }
}
