/**
 * Copyright (C) 2013 - 2021 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.handler.agreement;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.appcompat.app.AlertDialog;

import org.envirocar.app.R;
import org.envirocar.core.exception.TermsOfUseException;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.core.utils.rx.Optional;
import org.envirocar.core.utils.rx.dialogs.AbstractReactiveAcceptDialog;
import org.envirocar.core.utils.rx.dialogs.ReactivePrivacyStatementDialog;
import org.envirocar.core.utils.rx.dialogs.ReactiveTermsOfUseDialog;
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.User;
import org.envirocar.core.entity.UserImpl;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.logging.Logger;

import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableTransformer;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.ObservableSource;
import io.reactivex.rxjava3.functions.Function;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.observers.DisposableCompletableObserver;
import io.reactivex.rxjava3.observers.DisposableObserver;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class AgreementManager {
    private static final Logger LOG = Logger.getLogger(AgreementManager.class);

    protected List<TermsOfUse> list;

    // Injected variables.
    private final Context mContext;
    private final UserPreferenceHandler mUserManager;
    private final DAOProvider mDAOProvider;

    private TermsOfUse current;

    /**
     * Constructor.
     *
     * @param context
     */
    @Inject
    public AgreementManager(@InjectApplicationScope Context context, UserPreferenceHandler
            userHandler, DAOProvider daoProvider) {
        this.mContext = context;
        this.mUserManager = userHandler;
        this.mDAOProvider = daoProvider;
    }

    public Observable<TermsOfUse> verifyTermsOfUse(Activity activity) {
        return verifyTermsOfUse(activity, false);
    }

    public Observable<TermsOfUse> verifyTermsOfUse(Activity activity, boolean forceReload) {
        LOG.info("verifyTermsOfUse()");
        return getCurrentTermsOfUseObservable(forceReload)
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
        return getCurrentTermsOfUseObservable(false);
    }

    public Observable<TermsOfUse> getCurrentTermsOfUseObservable(boolean forceReload) {
        LOG.info("getCurrentTermsOfUseObservable()");
        return (current != null && !forceReload) ? Observable.just(current) : getRemoteTermsOfUseObservable();
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
                .flatMap(termsOfUse -> new ReactiveTermsOfUseDialog(activity, termsOfUse, getTermsOfUseParams(null)).asObservable());
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
                        current = mDAOProvider.getTermsOfUseDAO().getTermsOfUse(id);
                        
                        return current;
                    } catch (DataRetrievalFailureException | NotConnectedException e) {
                        LOG.warn(e.getMessage(), e);
                        throw e;
                    }
                });
    }

    public Observable<PrivacyStatement> showLatestPrivacyStatementDialogObservable(Activity activity) {
        return getRemotePrivacyStatementObservable()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(privacyStatement -> new ReactivePrivacyStatementDialog(activity, privacyStatement, getPrivacyStatementParams(null)).asObservable());
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
            User user;
            try {
                user = checkUserLoggedInAndReturn();
            } catch (Exception e) {
                throw new TermsOfUseException("The getUserStatistic has not accepted the privacy statement: " + e.getMessage());
            }
            LOG.info("Retrieved privacy statement for getUserStatistic [%s] with version [%s]",
                    user.getUsername(), user.getPrivacyStatementVersion());

            boolean hasAccepted = privacyStatement.getIssuedDate().equals(user.getTermsOfUseVersion());

            // if the getUserStatistic has accepted, then just return the statement
            if (hasAccepted) {
                return Observable.just(privacyStatement);
            }

            // if the getUserStatistic has not accepted, then check whether to show a dialog.
            if (activity != null) {
                return createPrivacyStatementObservable(user, privacyStatement, activity);
            }

            // if no dialog is possible, throw an exception
            else {
                throw new TermsOfUseException("The getUserStatistic has not accepted the privacy statement");
            }
        };
    }

    private Function<TermsOfUse, Observable<TermsOfUse>> checkTermsOfUseAcceptance(Context activity) {
        LOG.info("checkTermsOfUseAcceptance()");
        return termsOfUse -> {
            User user;
            try {
                user = checkUserLoggedInAndReturn();
            } catch (Exception e) {
                throw new TermsOfUseException("The getUserStatistic has not accepted the terms of use: " + e.getMessage());
            }
            LOG.info(String.format("Retrieved terms of use for getUserStatistic [%s] with terms of" +
                    " use version [%s]", user.getUsername(), user.getTermsOfUseVersion()));

            boolean hasAccepted = termsOfUse.getIssuedDate().equals(user.getTermsOfUseVersion());

            // If the getUserStatistic has accepted, then just return the generic type
            if (hasAccepted){
                return Observable.just(termsOfUse);
            }
            // If the input activity is not null, then create an dialog observable.
            else if (activity != null) {
                LOG.info("Opening ToUs dialog with parent activity: " + activity);
                return createTermsOfUseDialogObservable(user, termsOfUse, activity);
            }
            // Otherwise, throw an exception.
            
            LOG.info("ToUs not accepted, throwing TermsOfUseException");
            throw new TermsOfUseException("The getUserStatistic has not accepted the terms of use");
        };
    }

    private User checkUserLoggedInAndReturn() throws Exception {
        User user = mUserManager.getUser();
        if (user == null) {
            throw new NotLoggedInException(mContext.getString(R.string.trackviews_not_logged_in));
        }
        try {
            user = mUserManager.retrieveUpdatedUser(user);
        } catch (NotConnectedException e) {
            // workaround to not jump out with an exception, the result is the same: user has
            // not accepted the latest tou version
            user.setTermsOfUseVersion("");
        }
        
        return user;
    }

    private AbstractReactiveAcceptDialog.Params getPrivacyStatementParams(User user){
        return new AbstractReactiveAcceptDialog.Params(
                user, R.string.privacy_statement_title, R.string.ok, true);
    }

    private AbstractReactiveAcceptDialog.Params getTermsOfUseParams(User user){
        return new AbstractReactiveAcceptDialog.Params(
                user, R.string.terms_of_use_title, R.string.ok, true);
    }

    public Observable<TermsOfUse> createTermsOfUseDialogObservable(
            User user, TermsOfUse currentTermsOfUse, Context activity) {
        return new ReactiveTermsOfUseDialog(activity, currentTermsOfUse, getTermsOfUseParams(user))
                .asObservable()
                .map(termsOfUse -> {
                    LOG.info("TermsOfUseDialog: the getUserStatistic has accepted the ToU.");

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
        return new ReactivePrivacyStatementDialog(activity, currentPrivacyStatement, getPrivacyStatementParams(user))
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

    public void initializeTermsOfUseAcceptanceWorkflow(String username, String password, Context ctx, Class<? extends Activity> targetActivity, Consumer<Optional<TermsOfUse>> termsOfUseConsumer) {
        User candidateUser = new UserImpl(username, password);
        // temp login so we can use the agreementManager
        mUserManager.setUser(candidateUser);

        initializeTermsOfUseAcceptanceWorkflow(candidateUser, ctx, targetActivity, termsOfUseConsumer);
    }

    public void initializeTermsOfUseAcceptanceWorkflow(User candidateUser, Context ctx, Class<? extends Activity> targetActivity, Consumer<Optional<TermsOfUse>> termsOfUseConsumer) {
        
        DisposableObserver disposable = Observable.just(candidateUser)
            // Verify whether the TermsOfUSe have been accepted.
            // When the TermsOfUse have not been accepted, create an
            // Dialog to accept and continue when the getUserStatistic has accepted.
            .compose(AgreementManager.TermsOfUseValidator.create(this, ctx))
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            // Continue when the TermsOfUse has been accepted, otherwise
            // throw an error
            .subscribeWith(new DisposableObserver<User>() {
                @Override
                public void onNext(User u) {
                    LOG.info("User accepted latest ToU");
                    mUserManager.logIn(candidateUser.getUsername(), candidateUser.getToken())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeWith(new DisposableCompletableObserver() {
                            private AlertDialog dialog;

                            @Override
                            protected void onStart() {
                                if (checkNetworkConnection()) {
                                    dialog = DialogUtils.createProgressBarDialogBuilder(ctx,
                                            R.string.activity_login_logging_in_dialog_title,
                                            R.drawable.ic_baseline_login_24,
                                            (String) null)
                                            .setCancelable(false)
                                            .show();
                                }
                            }

                            @Override
                            public void onComplete() {
                                if (checkNetworkConnection()) {
                                    dialog.dismiss();
                                }
                                
                                if (targetActivity != null) {
                                    Intent intent = new Intent(ctx, targetActivity);
                                    ctx.startActivity(intent);
                                }
                                
                            }

                            @Override
                            public void onError(Throwable e) {
                                LOG.error(e);
                                LOG.warn("Unexpected error in ToU mechanism.");
                                termsOfUseConsumer.accept(Optional.create(null));
                            }
                        });
                }

                @Override
                public void onError(Throwable e) {
                    LOG.error(e);
                    LOG.info("User did not accept latest ToU");
                    termsOfUseConsumer.accept(Optional.create(null));
                }

                @Override
                public void onComplete() {
                    LOG.info("ToU acceptance workflow complete.");
                    termsOfUseConsumer.accept(Optional.create(getCurrentTermsOfUseObservable().blockingFirst()));
                }
            });
    }


    private boolean checkNetworkConnection() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }


    public static class TermsOfUseValidator<T> implements ObservableTransformer<T, T> {
        private final AgreementManager agreementManager;
        private final Context ctx;

        public static <T> TermsOfUseValidator<T> create(AgreementManager agreementManager, Context ctx) {
            return new TermsOfUseValidator<T>(agreementManager, ctx);
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
         * @param activity         the activity for the case when the getUserStatistic has not accepted the
         *                         terms of use. Then it creates a Dialog for acceptance.
         */
        public TermsOfUseValidator(AgreementManager agreementManager, Context ctx) {
            this.agreementManager = agreementManager;
            this.ctx = ctx;
        }

        @Override
        public ObservableSource<T> apply(Observable<T> upstream) {
            return upstream.flatMap(t ->
                    agreementManager.getCurrentTermsOfUseObservable()
                            .flatMap(agreementManager.checkTermsOfUseAcceptance(ctx))
//                            .flatMap(o -> agreementManager.getCurrentPrivacyStatementObservable())
//                            .flatMap(agreementManager.checkPrivacyStatementAcceptance(activity))
                            .flatMap(termsOfUse -> Observable.just(t)));
        }
    }
}
