package org.envirocar.remote.repository;

import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.logging.Logger;
import org.envirocar.remote.dao.BaseRemoteDAO;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;

import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

/**
 * @author dewall
 */
public abstract class RemoteRepository<Service> {
    private static final Logger LOG = Logger.getLogger(RemoteRepository.class);

    protected final Service remoteService;

    /**
     * Wrapper for calling a function with an exception.
     */
    interface FuncWithException<T> {
        T call() throws Exception;
    }

    /**
     * Constructor.
     *
     * @param remoteService the created retrofit rest service object.
     */
    public RemoteRepository(Service remoteService) {
        this.remoteService = remoteService;
    }

    protected <T> Response<T> executeCall(Call<T> call) throws IOException,
            NotConnectedException, UnauthorizedException, ResourceConflictException {
        Response<T> response = call.execute();

        // assert the responsecode if it was not an success.
        if (!response.isSuccessful()) {
            ResponseBody body = response.errorBody();
            EnvirocarServiceUtils.assertStatusCode(response.code(), response.message(), body.string());
        }

        return response;
    }

    protected <T> T executeCallReturnBody(Call<T> call) throws IOException,
            NotConnectedException, UnauthorizedException, ResourceConflictException {
        return this.executeCall(call).body();
    }

    protected <T> T wrapExecuteCallReturnBody(Call<T> call) throws DataRetrievalFailureException, NotConnectedException {
        try {
            return executeCallReturnBody(call);
        } catch (IOException | UnauthorizedException | ResourceConflictException e) {
            LOG.error(String.format("Error while requesting %s", remoteService.getClass().getSimpleName()), e);
            throw new DataRetrievalFailureException(e);
        }
    }

    protected <T> Observable<T> wrapObservableHandling(FuncWithException<T> func) {
        return Observable.create(subscriber -> {
            try {
                subscriber.onNext(func.call());
                subscriber.onComplete();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }
}
