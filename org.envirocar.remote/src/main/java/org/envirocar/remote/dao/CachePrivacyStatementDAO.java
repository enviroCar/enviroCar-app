package org.envirocar.remote.dao;

import org.envirocar.core.dao.AbstractCacheDAO;
import org.envirocar.core.dao.PrivacyStatementDAO;
import org.envirocar.core.dao.TermsOfUseDAO;
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;

import java.util.List;

import rx.Observable;

public class CachePrivacyStatementDAO extends AbstractCacheDAO implements PrivacyStatementDAO {
    @Override
    public PrivacyStatement getPrivacyStatement(String id) throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<PrivacyStatement> getPrivacyStatementObservable(String id) {
        return null;
    }

    @Override
    public List<PrivacyStatement> getPrivacyStatements() throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<List<PrivacyStatement>> getPrivacyStatementsObservable() {
        return null;
    }
}
