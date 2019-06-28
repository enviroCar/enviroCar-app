package org.envirocar.remote.dao;

import org.envirocar.core.dao.PrivacyStatementDAO;
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.service.PrivacyStatementService;

import java.util.List;

import javax.inject.Inject;

import retrofit2.Call;
import rx.Observable;

/**
 * @author dewall
 */
public class RemotePrivacyStatementDAO extends BaseRemoteDAO<PrivacyStatementDAO, PrivacyStatementService> implements PrivacyStatementDAO {
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


    @Override
    public PrivacyStatement getPrivacyStatement(String id) throws DataRetrievalFailureException, NotConnectedException {
        LOG.info("Requesting Privacy Statement for id=%s", id);
        Call<PrivacyStatement> call = this.remoteService.getPrivacyStatement(id);
        return wrapExecuteCallReturnBody(call);
    }

    @Override
    public Observable<PrivacyStatement> getPrivacyStatementObservable(String id) {
        return wrapObservableHandling(() -> getPrivacyStatement(id));
    }

    @Override
    public List<PrivacyStatement> getPrivacyStatements() throws DataRetrievalFailureException, NotConnectedException {
        LOG.info("Requesting list of privacy statements");
        Call<List<PrivacyStatement>> call = this.remoteService.getPrivacyStatements();
        return wrapExecuteCallReturnBody(call);
    }

    @Override
    public Observable<List<PrivacyStatement>> getPrivacyStatementsObservable() {
        return wrapObservableHandling(() -> getPrivacyStatements());
    }
}
