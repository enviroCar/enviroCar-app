package org.envirocar.app.model.dao.remote;

import org.envirocar.app.model.User;
import org.envirocar.app.model.UserStatistics;
import org.envirocar.app.model.dao.UserStatisticsDAO;
import org.envirocar.app.model.dao.exception.NotConnectedException;
import org.envirocar.app.model.dao.exception.UnauthorizedException;
import org.envirocar.app.model.dao.exception.UserRetrievalException;
import org.envirocar.app.model.dao.exception.UserStatisticsRetrievalException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * @author dewall
 */
public class RemoteUserStatisticsDAO extends BaseRemoteDAO
        implements UserStatisticsDAO, AuthenticatedDAO {

    @Override
    public UserStatistics getUserStatistics(User user)
            throws UserStatisticsRetrievalException,  UnauthorizedException {
        try{
            JSONObject json = readRemoteResource("/users/" + user.getUsername() + "/statistics/");
            return UserStatistics.fromJson(json);
        } catch (NotConnectedException e) {
            throw new UserStatisticsRetrievalException(e);
        } catch (JSONException e) {
            throw new UserStatisticsRetrievalException(e);
        } catch (IOException e) {
            throw new UserStatisticsRetrievalException(e);
        }
    }
}
