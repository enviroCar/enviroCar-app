package org.envirocar.remote.repository;

import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.repository.TermsOfUseRepository;
import org.envirocar.remote.service.TermsOfUseService;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import retrofit2.Call;

/**
 * @author dewall
 */
@Singleton
public class RemoteTermsOfUseRepository extends RemoteRepository<TermsOfUseService> implements TermsOfUseRepository {

    /**
     * Constructor.
     *
     * @param remoteService the created retrofit rest service object.
     */
    @Inject
    public RemoteTermsOfUseRepository(TermsOfUseService remoteService) {
        super(remoteService);
    }

    @Override
    public TermsOfUse getTermsOfUse(String id) throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<TermsOfUse> getTermsOfUseObservable(String id) {
        Call<TermsOfUse> call = remoteService.getTermsOfUseByID(id);
        return wrapObservableHandling(() -> wrapExecuteCallReturnBody(call));
    }

    @Override
    public List<TermsOfUse> getAllTermsOfUse() throws DataRetrievalFailureException, NotConnectedException {
        return null;
    }

    @Override
    public Observable<List<TermsOfUse>> getAllTermsOfUseObservable() {
        Call<List<TermsOfUse>> call = remoteService.getAllTermsOfUse();
        return wrapObservableHandling(() -> wrapExecuteCallReturnBody(call));
    }
}
