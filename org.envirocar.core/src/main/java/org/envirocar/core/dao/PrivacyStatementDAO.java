package org.envirocar.core.dao;

import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;

import java.util.List;

import rx.Observable;


/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public interface PrivacyStatementDAO {

    PrivacyStatement getPrivacyStatement(String id) throws DataRetrievalFailureException, NotConnectedException;

    Observable<PrivacyStatement> getPrivacyStatementObservable(String id);

    List<PrivacyStatement> getPrivacyStatements() throws DataRetrievalFailureException, NotConnectedException;

    Observable<List<PrivacyStatement>> getPrivacyStatementsObservable();
    
}
