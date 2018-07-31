/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.handler;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.squareup.otto.Bus;

import org.envirocar.app.R;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.views.utils.ReactiveTermsOfUseDialog;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.util.InjectApplicationScope;
import org.envirocar.core.logging.Logger;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import rx.Observable;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
@Singleton
public class TermsOfUseManager {
    private static final Logger LOGGER = Logger.getLogger(TermsOfUseManager.class);
    // Mutex for locking when downloading.
    private final Object mMutex = new Object();
    protected List<TermsOfUse> list;

    // Injected variables.
    private final Context mContext;
    private final Bus mBus;
    private final UserHandler mUserManager;
    private final DAOProvider mDAOProvider;

    private TermsOfUse current;

    /**
     * Constructor.
     *
     * @param context
     */
    @Inject
    public TermsOfUseManager(@InjectApplicationScope Context context, Bus bus, UserHandler
            userHandler, DAOProvider daoProvider) {
        this.mContext = context;
        this.mBus = bus;
        this.mUserManager = userHandler;
        this.mDAOProvider = daoProvider;
    }

    public Observable<TermsOfUse> verifyTermsOfUse(Activity activity) {
        LOGGER.info("verifyTermsOfUse()");
        return getCurrentTermsOfUseObservable()
                .flatMap(checkTermsOfUseAcceptance(activity));
    }

    public <T> Observable<T> verifyTermsOfUse(Activity activity, T t) {
        return verifyTermsOfUse(activity)
                .map(termsOfUse -> {
                    LOGGER.info("User has accepted terms of use.");
                    return t;
                });
    }

    public Observable<TermsOfUse> getCurrentTermsOfUseObservable() {
        LOGGER.info("getCurrentTermsOfUseObservable()");
        return current != null ? Observable.just(current) : getRemoteTermsOfUseObservable();
    }

    private Observable<TermsOfUse> getRemoteTermsOfUseObservable() {
        LOGGER.info("getRemoteTermsOfUse() TermsOfUse are null. Try to fetch the last TermsOfUse.");
        return mDAOProvider.getTermsOfUseDAO()
                .getAllTermsOfUseObservable()
                .map(termsOfUses -> {
                    if (termsOfUses == null || termsOfUses.isEmpty())
                        throw OnErrorThrowable.from(new NotConnectedException(
                                "Error while retrieving terms of use: " +
                                        "Result set was null or empty"));

                    // Set the list of terms of uses.
                    TermsOfUseManager.this.list = termsOfUses;

                    try {
                        // Get the id of the first terms of use instance and fetch
                        // the terms of use
                        String id = termsOfUses.get(0).getId();
                        TermsOfUse inst = mDAOProvider.getTermsOfUseDAO().getTermsOfUse(id);
                        return inst;
                    } catch (DataRetrievalFailureException | NotConnectedException e) {
                        LOGGER.warn(e.getMessage(), e);
                        throw OnErrorThrowable.from(e);
                    }
                });
    }

    private Func1<TermsOfUse, Observable<TermsOfUse>> checkTermsOfUseAcceptance(Activity activity) {
        LOGGER.info("checkTermsOfUseAcceptance()");
        return new Func1<TermsOfUse, Observable<TermsOfUse>>() {
            @Override
            public Observable<TermsOfUse> call(TermsOfUse termsOfUse) {
                User user = mUserManager.getUser();
                if (user == null) {
                    throw OnErrorThrowable.from(new NotLoggedInException(
                            mContext.getString(R.string.trackviews_not_logged_in)));
                }

                LOGGER.info(String.format("Retrieved terms of use for user [%s] with terms of" +
                        " use version [%s]", user.getUsername(), user.getTermsOfUseVersion()));

                boolean hasAccepted = termsOfUse
                        .getIssuedDate().equals(user.getTermsOfUseVersion());

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
                    throw OnErrorThrowable.from(new NotAcceptedTermsOfUseException(
                            "The user has not accepted the terms of use"));
                }
            }
        };
    }

    public Observable createTermsOfUseDialogObservable(
            User user, TermsOfUse currentTermsOfUse, Activity activity) {
        return new ReactiveTermsOfUseDialog(activity, user, currentTermsOfUse)
                .asObservable()
                .map(new Func1<TermsOfUse, TermsOfUse>() {
                    @Override
                    public TermsOfUse call(TermsOfUse termsOfUse) {
                        LOGGER.info("TermsOfUseDialog: the user has accepted the ToU.");

                        try {
                            // set the terms of use
                            user.setTermsOfUseVersion(termsOfUse.getIssuedDate());
                            mDAOProvider.getUserDAO().updateUser(user);
                            mUserManager.setUser(user);

                            LOGGER.info("TermsOfUseDialog: User successfully updated");

                            return termsOfUse;
                        } catch (DataUpdateFailureException | UnauthorizedException e) {
                            LOGGER.warn(e.getMessage(), e);
                            throw OnErrorThrowable.from(e);
                        }
                    }
                });
    }


    //    /**
    //     * Checks if the Terms are accepted. If not, open Dialog. On positive
    //     * feedback, update the User.
    //     *
    //     * @param user
    //     * @param callback
    //     */
    //    public void askForTermsOfUseAcceptance(final User user, final PositiveNegativeCallback
    //            callback) {
    //        boolean verified = false;
    //        try {
    //            verified = verifyTermsUseOfVersion(user.getTermsOfUseVersion());
    //        } catch (ServerException e) {
    //            LOGGER.warn(e.getMessage(), e);
    //            return;
    //        }
    //        if (!verified) {
    //
    //            final TermsOfUse current;
    //            try {
    //                current = getCurrentTermsOfUse();
    //            } catch (ServerException e) {
    //                LOGGER.warn("This should never happen!", e);
    //                return;
    //            }
    //
    //            new MaterialDialog.Builder(mContext)
    //                    .title(R.string.terms_of_use_title)
    //                    .content((user.getTermsOfUseVersion() == null) ?
    //                            R.string.terms_of_use_sorry :
    //                            R.string.terms_of_use_info)
    //                    .onPositive((materialDialog, dialogAction) -> {
    //                        userAcceptedTermsOfUse(user, current.getIssuedDate());
    //                        Toast.makeText(mContext, R.string.terms_of_use_updating_server, Toast
    //                                .LENGTH_LONG).show();
    //                        if (callback != null) {
    //                            callback.positive();
    //                        }
    //                    })
    //                    .onNegative((materialDialog, dialogAction) -> {
    //                        LOGGER.info("User did not accept the ToU.");
    //                        Toast.makeText(mContext, R.string.terms_of_use_cant_continue, Toast
    //                                .LENGTH_LONG).show();
    //                        if (callback != null) {
    //                            callback.negative();
    //                        }
    //                    })
    //                    .show();
    //        } else {
    //            LOGGER.info("User has accpeted ToU in current version.");
    //        }
    //    }


    public TermsOfUse getCurrentTermsOfUse() {
        if (this.current == null) {
            mDAOProvider.getTermsOfUseDAO()
                    .getAllTermsOfUseObservable()
                    .map(new Func1<List<TermsOfUse>, TermsOfUse>() {
                        @Override
                        public TermsOfUse call(List<TermsOfUse> termsOfUses) {
                            if (termsOfUses != null) {
                                list = termsOfUses;
                                String id = termsOfUses.get(0).getId();
                                try {
                                    TermsOfUse inst = mDAOProvider.getTermsOfUseDAO()
                                            .getTermsOfUse(id);
                                    current = inst;
                                } catch (DataRetrievalFailureException e) {
                                    LOGGER.warn(e.getMessage(), e);
                                    throw OnErrorThrowable.from(e);
                                } catch (NotConnectedException e) {
                                    LOGGER.warn(e.getMessage(), e);
                                    throw OnErrorThrowable.from(e);
                                }
                            } else {
                                LOGGER.warn("Could not retrieve latest instance as their is no " +
                                        "list available!");
                            }
                            LOGGER.info("Successfully retrieved the current terms of use.");
                            return current;
                        }
                    })
                    .toBlocking()
                    .first();
        }
        LOGGER.info("Returning the current terms of use.");
        return current;
    }


    //    private void retrieveTermsOfUse() throws ServerException {
    //
    //        mDAOProvider.getTermsOfUseDAO()
    //                .getAllTermsOfUseObservable()
    //                .subscribeOn(Schedulers.io())
    //                .obser
    //
    //
    //        new Thread(new Runnable() {
    //            @Override
    //            public void run() {
    //                List<TermsOfUse> response;
    //                try {
    //                    response = mDAOProvider.getTermsOfUseDAO().getAllTermsOfUse();
    //                    setList(response);
    //                    retrieveLatestInstance();
    //                } catch (DataRetrievalFailureException e) {
    //                    LOGGER.warn(e.getMessage(), e);
    //                } catch (NotConnectedException e) {
    //                    LOGGER.warn(e.getMessage(), e);
    //                }
    //            }
    //        }).start();
    //
    //        synchronized (mMutex) {
    //            while (current == null) {
    //                try {
    //                    mMutex.wait(5000);
    //
    //                    if (current == null) {
    //                        throw new ServerException(new TimeoutException("Waiting to long for
    // a " +
    //                                "response."));
    //                    }
    //                } catch (InterruptedException e) {
    //                    throw new ServerException(e);
    //                }
    //            }
    //        }
    //    }

    //    private void retrieveLatestInstance() {
    //        if (list != null && list != null && list.size() > 0) {
    //            String id = list.get(0).getId();
    //            try {
    //                TermsOfUse inst = mDAOProvider.getTermsOfUseDAO().getTermsOfUse(id);
    //                setCurrent(inst);
    //            } catch (DataRetrievalFailureException e) {
    //                LOGGER.warn(e.getMessage(), e);
    //            } catch (NotConnectedException e) {
    //                LOGGER.warn(e.getMessage(), e);
    //            }
    //        } else {
    //            LOGGER.warn("Could not retrieve latest instance as their is no list available!");
    //        }
    //    }

    //    private void setCurrent(TermsOfUse t) {
    //        LOGGER.info("Current Terms Of Use: " + t.getIssuedDate());
    //        current = t;
    //
    //        synchronized (mMutex) {
    //            mMutex.notifyAll();
    //        }
    //    }

    public void userAcceptedTermsOfUse(final User user, final String issuedDate) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    // set the terms of use in the user of the normal preferences.
                    user.setTermsOfUseVersion(issuedDate);
                    mDAOProvider.getUserDAO().updateUser(user);
                    mUserManager.setUser(user);
                    LOGGER.info("User successfully updated.");
                } catch (DataUpdateFailureException e) {
                    LOGGER.warn(e.getMessage(), e);
                } catch (UnauthorizedException e) {
                    LOGGER.warn(e.getMessage(), e);
                }
                return null;
            }
        }.execute();
    }


    public static class TermsOfUseValidator<T> implements Observable.Transformer<T, T> {
        private final TermsOfUseManager termsOfUseManager;
        private final Activity activity;

        public static <T> TermsOfUseValidator<T> create(
                TermsOfUseManager termsOfUseManager,
                Activity activity) {
            return new TermsOfUseValidator<T>(termsOfUseManager, activity);
        }

        /**
         * Constructor.
         *
         * @param termsOfUseManager the manager for the terms of use.
         */
        public TermsOfUseValidator(TermsOfUseManager termsOfUseManager) {
            this(termsOfUseManager, null);
        }

        /**
         * Constructor.
         *
         * @param termsOfUseManager the manager for the terms of use.
         * @param activity          the activity for the case when the user has not accepted the
         *                          terms of use. Then it creates a Dialog for acceptance.
         */
        public TermsOfUseValidator(TermsOfUseManager termsOfUseManager, Activity activity) {
            this.termsOfUseManager = termsOfUseManager;
            this.activity = activity;
        }

        @Override
        public Observable<T> call(Observable<T> tObservable) {
            return tObservable.flatMap(t ->
                    termsOfUseManager.getCurrentTermsOfUseObservable()
                            .flatMap(termsOfUseManager.checkTermsOfUseAcceptance(activity))
                            .flatMap(termsOfUse -> Observable.just(t)));
        }
    }
}
