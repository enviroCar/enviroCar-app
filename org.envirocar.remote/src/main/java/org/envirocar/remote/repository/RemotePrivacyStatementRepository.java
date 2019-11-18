package org.envirocar.remote.repository;

import org.envirocar.core.InternetAccessProvider;
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.repository.PrivacyStatementRepository;
import org.envirocar.remote.service.PrivacyStatementService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import retrofit2.Call;

/**
 * @author dewall
 */
@Singleton
public class RemotePrivacyStatementRepository extends RemoteRepository<PrivacyStatementService> implements PrivacyStatementRepository {

    /**
     * Constructor.
     *
     * @param remoteService the created retrofit rest service object.
     */
    @Inject
    public RemotePrivacyStatementRepository(PrivacyStatementService remoteService, InternetAccessProvider accessProvider) {
        super(remoteService, accessProvider);
    }

    @Override
    public PrivacyStatement getPrivacyStatement(String id) throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<PrivacyStatement> getPrivacyStatementObservable(String id) {
        Call<PrivacyStatement> call = remoteService.getPrivacyStatement(id);
        return wrapObservableHandling(() -> wrapExecuteCallReturnBody(call));
    }

    @Override
    public List<PrivacyStatement> getPrivacyStatements() throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<List<PrivacyStatement>> getPrivacyStatementsObservable() {
        Call<List<PrivacyStatement>> call = remoteService.getPrivacyStatements();
        return wrapObservableHandling(() -> wrapExecuteCallReturnBody(call));
    }
}
