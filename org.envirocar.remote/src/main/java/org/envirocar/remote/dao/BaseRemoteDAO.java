package org.envirocar.remote.dao;

import org.envirocar.core.UserManager;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.ResourceConflictException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.remote.util.EnvirocarServiceUtils;

import java.io.IOException;

import retrofit.Call;
import retrofit.Response;

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

    protected <T> Response<T> executeCall(Call<T> call) throws IOException,
            NotConnectedException, UnauthorizedException, ResourceConflictException {
        Response<T> response = call.execute();

        // assert the responsecode if it was not an success.
        if (!response.isSuccess()) {
            EnvirocarServiceUtils.assertStatusCode(response.code(),
                    response.message());
        }

        return response;
    }

}
