package org.envirocar.app.model.service.utils;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.envirocar.app.application.UserManager;
import org.envirocar.app.model.User;

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
