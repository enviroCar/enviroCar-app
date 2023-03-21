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
package org.envirocar.app.interactor;

import android.app.Activity;

import org.envirocar.app.R;
import org.envirocar.app.handler.DAOProvider;
import org.envirocar.core.InternetAccessProvider;
import org.envirocar.core.UserManager;
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.TermsOfUseException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectIOScheduler;
import org.envirocar.core.injection.InjectUIScheduler;
import org.envirocar.core.interactor.GetLatestPrivacyStatement;
import org.envirocar.core.interactor.GetLatestTermsOfUse;
import org.envirocar.core.interactor.Interactor;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.rx.Optional;
import org.envirocar.core.utils.rx.dialogs.AbstractReactiveAcceptDialog;
import org.envirocar.core.utils.rx.dialogs.ReactivePrivacyStatementDialog;
import org.envirocar.core.utils.rx.dialogs.ReactiveTermsOfUseDialog;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;

/**
 * @author dewall
 */
public class ValidateAcceptedTerms extends Interactor<Boolean, ValidateAcceptedTerms.Params> {
    private static final Logger LOG = Logger.getLogger(ValidateAcceptedTerms.class);

    private final InternetAccessProvider mInternetAccessProvider;

    private final UserManager userManager;
    private final DAOProvider daoProvider;
    private final GetLatestTermsOfUse getLatestTermsOfUse;
    private final GetLatestPrivacyStatement getLatestPrivacyStatement;

    /**
     * Cosntructor.
     *
     * @param observeOn                 the thread to observe on.
     * @param subscribeOn               the thread to subscribe on.
     * @param mInternetAccessProvider
     * @param userManager
     * @param daoProvider
     * @param getLatestTermsOfUse
     * @param getLatestPrivacyStatement
     */
    @Inject
    public ValidateAcceptedTerms(@InjectUIScheduler Scheduler observeOn, @InjectIOScheduler Scheduler subscribeOn,
                                 InternetAccessProvider mInternetAccessProvider, UserManager userManager,
                                 DAOProvider daoProvider, GetLatestTermsOfUse getLatestTermsOfUse,
                                 GetLatestPrivacyStatement getLatestPrivacyStatement) {
        super(observeOn, subscribeOn);
        this.mInternetAccessProvider = mInternetAccessProvider;
        this.userManager = userManager;
        this.daoProvider = daoProvider;
        this.getLatestTermsOfUse = getLatestTermsOfUse;
        this.getLatestPrivacyStatement = getLatestPrivacyStatement;
    }

    @Override
    protected Observable<Boolean> buildObservable(Params params) {
        return Observable.defer(() -> termsOfUseAcceptance(params.user, params.activity))
                // .onErrorResumeNext(Observable.just(new TermsOfUseImpl()))
                .flatMap(x -> privacyStatementAcceptance(params.user, params.activity))
                // .onErrorResumeNext(Observable.just(new PrivacyStatementImpl()))
                .map(x -> true);
    }

    private Observable<TermsOfUse> termsOfUseAcceptance(User user, Activity activity) {
        return getLatestTermsOfUse.execute(null)
                .map(Optional::getOptional)
                .flatMap(checkTermsOfUseAcceptance(user, activity));
    }

    private Observable<PrivacyStatement> privacyStatementAcceptance(User user, Activity activity) {
        return getLatestPrivacyStatement.execute(null)
                .map(Optional::getOptional)
                .flatMap(checkPrivacyStatement(user, activity));
    }

    private Function<TermsOfUse, Observable<TermsOfUse>> checkTermsOfUseAcceptance(User user, Activity activity) {
        return termsOfUse -> {
            if (user == null) {
                throw new TermsOfUseException("The user has not accepted the terms of use. No user was available.");
            }
            LOG.info(String.format("Retrieved terms of use for user [%s] with version [%s]",
                    user.getUsername(), user.getTermsOfUseVersion()));

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
                throw new TermsOfUseException("The user has not accepted the terms of use");
            }
        };
    }

    private Function<PrivacyStatement, Observable<PrivacyStatement>> checkPrivacyStatement(User user, Activity activity) {
        return privacyStatement -> {
            LOG.info(String.format("Retrieved privacy statement for user [%s] with version [%s]",
                    user.getUsername(), user.getPrivacyStatementVersion()));

            boolean hasAccepted = false;
            if (user.getPrivacyStatementVersion() != null) {
                hasAccepted = privacyStatement.getIssuedDate().equals(user.getPrivacyStatementVersion());
            }

            // If the user has accepted, then just return the generic type
            if (hasAccepted) {
                return Observable.just(privacyStatement);
            }
            // If the input activity is not null, then create an dialog observable.
            else if (activity != null) {
                return createPrivacyStatementObservable(user, privacyStatement, activity);
            }
            // Otherwise, throw an exception.
            else {
                throw new TermsOfUseException("The user has not accepted the privacy statement");
            }
        };
    }

    private Observable<TermsOfUse> createTermsOfUseDialogObservable(
            User user, TermsOfUse currentTermsOfUse, Activity activity) {
        return new ReactiveTermsOfUseDialog(activity, currentTermsOfUse, getTermsOfUseDialogParams(user))
                .asObservable()
                .map(termsOfUse -> {
                    LOG.info("TermsOfUseDialog: the user has accepted the ToU.");

                    try {
                        // set the terms of use
                        user.setTermsOfUseVersion(termsOfUse.getIssuedDate());
                        daoProvider.getUserDAO().updateUser(user);
                        userManager.setUser(user);

                        LOG.info("TermsOfUseDialog: User successfully updated");

                        return termsOfUse;
                    } catch (DataUpdateFailureException | UnauthorizedException e) {
                        LOG.warn(e.getMessage(), e);
                        throw e;
                    }
                });
    }

    private Observable<PrivacyStatement> createPrivacyStatementObservable(
            User user, PrivacyStatement currentPrivacyStatement, Activity activity) {
        return new ReactivePrivacyStatementDialog(activity, currentPrivacyStatement, getPrivacyStatementDialogParams(user))
                .asObservable()
                .map(privacyStatement -> {
                    LOG.info("PrivacyStatementDialog: the user has accepted the PS.");

                    try {
                        // set the terms of use
                        user.setPrivacyStatementVersion(privacyStatement.getIssuedDate());
                        daoProvider.getUserDAO().updateUser(user);
                        userManager.setUser(user);

                        LOG.info("PrivacyStatementDialog: User successfully updated");

                        return privacyStatement;
                    } catch (DataUpdateFailureException | UnauthorizedException e) {
                        LOG.warn(e.getMessage(), e);
                        throw e;
                    }
                });
    }

    private AbstractReactiveAcceptDialog.Params getTermsOfUseDialogParams(User user) {
        return new AbstractReactiveAcceptDialog.Params(user, R.string.terms_of_use_title,
                R.string.terms_of_use_accept, R.string.terms_of_use_reject, true);
    }

    private AbstractReactiveAcceptDialog.Params getPrivacyStatementDialogParams(User user) {
        return new AbstractReactiveAcceptDialog.Params(user, R.string.privacy_statement_title,
                R.string.terms_of_use_accept, R.string.terms_of_use_reject, true);
    }

    public static final class Params {
        private final User user;
        private final Activity activity;

        public Params(User user, Activity activity) {
            this.user = user;
            this.activity = activity;
        }

        public User getUser() {
            return user;
        }

        public Activity getActivity() {
            return activity;
        }
    }

    public final class ValidationTransformer<T> implements ObservableTransformer<T, T> {

        @Override
        public ObservableSource<T> apply(Observable<T> upstream) {
            return null;
        }
    }
}
