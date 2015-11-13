package org.envirocar.core.dao;


import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;

import rx.Observable;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface UserDAO {

    User getUser(String id) throws DataRetrievalFailureException, UnauthorizedException, NotConnectedException;

    Observable<User> getUserObservable(String id);

    void createUser(User user) throws DataUpdateFailureException, ResourceConflictException;

    void updateUser(User user) throws DataUpdateFailureException, UnauthorizedException;
}
