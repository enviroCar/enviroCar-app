package org.envirocar.remote.dao;

import org.envirocar.core.dao.PrivacyStatementDAO;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.PrivacyStatementService;

import javax.inject.Inject;

/**
 * @author dewall
 */
public class RemotePrivacyStatementDAO extends BaseRemoteDAO<PrivacyStatementDAO, PrivacyStatementService> {
    private static final Logger LOG = Logger.getLogger(RemotePrivacyStatementDAO.class);

    /**
     * Constructor.
     *
     * @param cacheDao the DAO for cached items #TODO remove
     * @param service
     */
    @Inject
    public RemotePrivacyStatementDAO(CachePrivacyStatementDAO cacheDao, PrivacyStatementService service) {
        super(cacheDao, service);
    }


}
