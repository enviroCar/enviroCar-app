package org.envirocar.remote.util;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Response;


import org.envirocar.core.UserManager;
import org.envirocar.core.entity.User;

import java.io.IOException;

/**
 * @author dewall
 */
public class AuthenticationInterceptor implements Interceptor {

    protected final UserManager mUserManager;

    /**
     * Constructor.
     *
     * @param userManager the user manager.
     */
    public AuthenticationInterceptor(UserManager userManager) {
        this.mUserManager = userManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        if (mUserManager.isLoggedIn()) {
            User user = mUserManager.getUser();
            return chain.proceed(
                    chain.request()
                            .newBuilder()
                            .addHeader("X-User", user.getUsername())
                            .addHeader("X-Token", user.getToken())
                            .build());
        } else {
            return chain.proceed(chain.request());
        }
    }
}
