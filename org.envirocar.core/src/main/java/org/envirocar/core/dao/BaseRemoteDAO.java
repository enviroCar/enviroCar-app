package org.envirocar.core.dao;

import org.envirocar.core.UserManager;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class BaseRemoteDAO<C, S> {

    protected final C cacheDao;
    protected final S remoteService;
    protected final UserManager userManager;



    /**
     * Constructor.
     *
     * @param cacheDao      the caching dao.
     * @param remoteService the remote service of this dao.
     */
    public BaseRemoteDAO(C cacheDao, S remoteService) {
        this(cacheDao, remoteService, null);
    }

    /**
     * Constructor.
     *
     * @param cacheDao      the cache dao
     * @param remoteService the remote service of this dao
     * @param userManager   the user manager
     */
    public BaseRemoteDAO(C cacheDao, S remoteService, UserManager userManager) {
        this.userManager = userManager;
        this.cacheDao = cacheDao;
        this.remoteService = remoteService;
    }
}
