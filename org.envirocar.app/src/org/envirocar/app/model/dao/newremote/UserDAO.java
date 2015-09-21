package org.envirocar.app.model.dao.newremote;

import org.envirocar.app.model.User;

/**
 * @author dewall
 */
public interface UserDAO {

    
    public User getUser(String name);

    public void updateUser(User user);

    public void createUser(User user);
}
