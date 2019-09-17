/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.handler.agreement;

import android.app.Activity;
import android.content.Context;

import org.envirocar.app.R;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.preferences.UserHandler;
import org.envirocar.app.views.dialogs.ReactivePrivacyStatementDialog;
import org.envirocar.app.views.dialogs.ReactiveTermsOfUseDialog;
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class AgreementManager {
    private static final Logger LOG = Logger.getLogger(AgreementManager.class);
    // Mutex for locking when downloading.
    private final Object mMutex = new Object();
    protected List<TermsOfUse> list;

    // Injected variables.
    private final Context mContext;
    private final UserHandler mUserManager;
    private final DAOProvider mDAOProvider;

    private TermsOfUse current;

    /**
     * Constructor.
     *
     * @param context
     */
    @Inject
    public AgreementManager(@InjectApplicationScope Context context, UserHandler
            userHandler, DAOProvider daoProvider) {
        this.mContext = context;
        this.mUserManager = userHandler;
        this.mDAOProvider = daoProvider;
    }

    public Observable<TermsOfUse> verifyTermsOfUse(Activity activity) {
        LOG.info("verifyTermsOfUse()");
        return getCurrentTermsOfUseObservable()
                .flatMap(checkTermsOfUseAcceptance(activity));
    }

    public <T> Observable<T> verifyTermsOfUse(Activity activity, T t) {
        return verifyTermsOfUse(activity)
                .map(termsOfUse -> {
                    LOG.info("User has accepted terms of use.");
                    return t;
                });
    }

    public Observable<TermsOfUse> getCurrentTermsOfUseObservable() {
        LOG.info("getCurrentTermsOfUseObservable()");
        return current != null ? Observable.just(current) : getRemoteTermsOfUseObservable();
    }

    public Observable<PrivacyStatement> getCurrentPrivacyStatementObservable() {
        return getRemotePrivacyStatementObservable();
    }

    /**
     * @param activity currently visible activity
     * @return
     */
    public Observable<TermsOfUse> showLatestTermsOfUseDialogObservable(Activity activity) {
        return getRemoteTermsOfUseObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(termsOfUse -> new ReactiveTermsOfUseDialog(activity, termsOfUse).asObservable());
    }

    private Observable<TermsOfUse> getRemoteTermsOfUseObservable() {
        LOG.info("getRemoteTermsOfUse() TermsOfUse are null. Try to fetch the last TermsOfUse.");
        return mDAOProvider.getTermsOfUseDAO()
                .getAllTermsOfUseObservable()
                .map(checkNullElseThrowNotConnected())
                .map(termsOfUses -> {
                    // Set the list of terms of uses.
                    AgreementManager.this.list = termsOfUses;

                    try {
                        // Get the id of the first terms of use instance and fetch
                        // the terms of use
                        String id = termsOfUses.get(0).getId();
                        TermsOfUse inst = mDAOProvider.getTermsOfUseDAO().getTermsOfUse(id);
                        return inst;
                    } catch (DataRetrievalFailureException | NotConnectedException e) {
                        LOG.warn(e.getMessage(), e);
                        throw e;
                    }
                });
    }

    private Observable<PrivacyStatement> getRemotePrivacyStatementObservable() {
        return mDAOProvider.getPrivacyStatementDAO()
                .getPrivacyStatementsObservable()
                .map(checkNullElseThrowNotConnected())
                .map(privacyStatements -> {

                    try {
                        String id = privacyStatements.get(0).getId();
                        PrivacyStatement inst = mDAOProvider.getPrivacyStatementDAO().getPrivacyStatement(id);
                        return inst;
                    } catch (DataRetrievalFailureException | NotConnectedException e) {
                        LOG.warn(e.getMessage(), e);
                        throw e;
                    }
                });
    }

    private <T> Function<List<T>, List<T>> checkNullElseThrowNotConnected() {
        return t -> {
            if (t == null || t.isEmpty()) {
                throw new NotConnectedException("Error while retrieving Terms of Use and Privacy Statement");
            }
            return t;
        };
    }

    private Function<PrivacyStatement, Observable<PrivacyStatement>> checkPrivacyStatementAcceptance(Activity activity) {
        LOG.info("Check whether the Privacy Statement has been accepted.");
        return privacyStatement -> {
            User user = checkUserLoggedInAndReturn();
            LOG.info("Retrieved privacy statement for user [%s] with version [%s]",
                    user.getUsername(), user.getPrivacyStatementVersion());

            boolean hasAccepted = privacyStatement.getIssuedDate().equals(user.getTermsOfUseVersion());

            // if the user has accepted, then just return the statement
            if (hasAccepted) {
                return Observable.just(privacyStatement);
            }

            // if the user has not accepted, then check whether to show a dialog.
            if (activity != null) {
                return createPrivacyStatementObservable(user, privacyStatement, activity);
            }

            // if no dialog is possible, throw an exception
            else {
                throw new NotAcceptedTermsOfUseException("The user has not accepted the terms of use");
            }
        };
    }

    private Function<TermsOfUse, Observable<TermsOfUse>> checkTermsOfUseAcceptance(Activity activity) {
        LOG.info("checkTermsOfUseAcceptance()");
        return termsOfUse -> {
            User user = checkUserLoggedInAndReturn();
            LOG.info(String.format("Retrieved terms of use for user [%s] with terms of" +
                    " use version [%s]", user.getUsername(), user.getTermsOfUseVersion()));

            boolean hasAccepted = termsOfUse.getIssuedDate().equals(user.getTermsOfUseVersion());

            // If the user has accepted, then just return the generic type
            if (hasAccepted) {
                return Observable.just(termsOfUse);
            }
            // If the input activity is not null, then create an dialog observable.
            else if (activity != null) {
                return createTermsOfUseDialogObservable(user, termsOfUse, activity);
            }
            // Otherwise, throw an exception.
            else {
                throw new NotAcceptedTermsOfUseException("The user has not accepted the terms of use");
            }
        };
    }

    private User checkUserLoggedInAndReturn() throws Exception {
        User user = mUserManager.getUser();
        if (user == null) {
            throw new NotLoggedInException(mContext.getString(R.string.trackviews_not_logged_in));
        }
        return user;
    }

    public Observable<TermsOfUse> createTermsOfUseDialogObservable(
            User user, TermsOfUse currentTermsOfUse, Activity activity) {
        return new ReactiveTermsOfUseDialog(activity, user, currentTermsOfUse)
                .asObservable()
                .map(termsOfUse -> {
                    LOG.info("TermsOfUseDialog: the user has accepted the ToU.");

                    try {
                        // set the terms of use
                        user.setTermsOfUseVersion(termsOfUse.getIssuedDate());
                        mDAOProvider.getUserDAO().updateUser(user);
                        mUserManager.setUser(user);

                        LOG.info("TermsOfUseDialog: User successfully updated");

                        return termsOfUse;
                    } catch (DataUpdateFailureException | UnauthorizedException e) {
                        LOG.warn(e.getMessage(), e);
                        throw e;
                    }
                });
    }

    public Observable<PrivacyStatement> createPrivacyStatementObservable(User user, PrivacyStatement currentPrivacyStatement, Activity activity) {
        return new ReactivePrivacyStatementDialog(activity, user, currentPrivacyStatement)
                .asObservable()
                .map(privacyStatement -> {
                    LOG.info("The User has not accepted the latest privacy statement. Showing Dialog.");

                    try {
                        user.setPrivacyStatementVersion(privacyStatement.getIssuedDate());
                        mDAOProvider.getUserDAO().updateUser(user);
                        mUserManager.setUser(user);

                        LOG.info("PrivacyStatement successfully updated. Closing Dialog.");
                        return privacyStatement;
                    } catch (DataUpdateFailureException | UnauthorizedException e) {
                        LOG.warn(e.getMessage(), e);
                        throw e;
                    }
                });
    }


    public static class TermsOfUseValidator<T> implements ObservableTransformer<T, T> {
        private final AgreementManager agreementManager;
        private final Activity activity;

        public static <T> TermsOfUseValidator<T> create(
                AgreementManager agreementManager,
                Activity activity) {
            return new TermsOfUseValidator<T>(agreementManager, activity);
        }

        /**
         * Constructor.
         *
         * @param agreementManager the manager for the terms of use.
         */
        public TermsOfUseValidator(AgreementManager agreementManager) {
            this(agreementManager, null);
        }

        /**
         * Constructor.
         *
         * @param agreementManager the manager for the terms of use.
         * @param activity         the activity for the case when the user has not accepted the
         *                         terms of use. Then it creates a Dialog for acceptance.
         */
        public TermsOfUseValidator(AgreementManager agreementManager, Activity activity) {
            this.agreementManager = agreementManager;
            this.activity = activity;
        }

        @Override
        public ObservableSource<T> apply(Observable<T> upstream) {
            return upstream.flatMap(t ->
                    agreementManager.getCurrentTermsOfUseObservable()
                            .flatMap(agreementManager.checkTermsOfUseAcceptance(activity))
                            .flatMap(o -> agreementManager.getCurrentPrivacyStatementObservable())
                            .flatMap(agreementManager.checkPrivacyStatementAcceptance(activity))
                            .flatMap(termsOfUse -> Observable.just(t)));
        }
    }
}
