package org.envirocar.core.dao;

import org.envirocar.core.UserManager;

import javax.inject.Inject;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class BaseRemoteDAO<C> {

    protected final C cacheDao;

    @Inject
    protected UserManager userManager;

    /**
     * Constructor.
     */
    public BaseRemoteDAO(){
        this(null);
    }

    /**
     * Constructor.
     *
     * @param cacheDao data access object for local entities.
     */
    public BaseRemoteDAO(C cacheDao) {
        this(cacheDao, null);
    }

    /**
     * Constructor.
     *
     * @param cacheDao    data access object for local entities.
     * @param userManager an instance of the user manager.
     */
    public BaseRemoteDAO(C cacheDao, UserManager userManager) {
        this.userManager = userManager;
        this.cacheDao = cacheDao;
    }
}
