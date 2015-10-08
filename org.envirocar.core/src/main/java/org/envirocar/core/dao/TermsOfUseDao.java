package org.envirocar.core.dao;


import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;

import java.util.List;

import rx.Observable;

/**
 * @author dewall
 */
public interface TermsOfUseDAO {

    TermsOfUse getTermsOfUse(String id) throws DataRetrievalFailureException, NotConnectedException;

    Observable<TermsOfUse> getTermsOfUseObservable(String id);

    List<TermsOfUse> getAllTermsOfUse() throws DataRetrievalFailureException, NotConnectedException;

    Observable<List<TermsOfUse>> getAllTermsOfUseObservable();
}
