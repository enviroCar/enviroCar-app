/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.handler;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;

import com.squareup.otto.Bus;

import org.envirocar.app.R;
import org.envirocar.app.activity.DialogUtil;
import org.envirocar.app.activity.DialogUtil.PositiveNegativeCallback;
import org.envirocar.app.exception.ServerException;
import org.envirocar.remote.DAOProvider;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.User;
import org.envirocar.core.exception.DataRetrievalFailureException;
import org.envirocar.core.exception.DataUpdateFailureException;
import org.envirocar.core.exception.NotConnectedException;
import org.envirocar.core.exception.UnauthorizedException;
import org.envirocar.core.injection.InjectApplicationScope;
import org.envirocar.core.injection.Injector;
import org.envirocar.core.logging.Logger;

import java.util.List;

import javax.inject.Inject;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

public class TermsOfUseManager {
    private static final Logger LOGGER = Logger.getLogger(TermsOfUseManager.class);
    // Mutex for locking when downloading.
    private final Object mMutex = new Object();
    protected List<TermsOfUse> list;

    // Injected variables.
    @Inject
    @InjectApplicationScope
    protected Context mContext;
    @Inject
    protected Bus mBus;
    @Inject
    protected UserHandler mUserManager;
    @Inject
    protected DAOProvider mDAOProvider;


    private TermsOfUse current;

    public TermsOfUseManager(Context context) {

        // Inject ourselves.
        ((Injector) context).injectObjects(this);

        //        try {
        //            retrieveTermsOfUse();
        //        } catch (ServerException e) {
        //            LOGGER.warn(e.getMessage(), e);
        //        }
    }

    /**
     * Checks if the Terms are accepted. If not, open Dialog. On positive
     * feedback, update the User.
     *
     * @param user
     * @param activity
     * @param callback
     */
    public void askForTermsOfUseAcceptance(final User user, final Activity activity,
                                           final PositiveNegativeCallback callback) {
        boolean verified = false;
        try {
            verified = verifyTermsUseOfVersion(user.getTermsOfUseVersion());
        } catch (ServerException e) {
            LOGGER.warn(e.getMessage(), e);
            return;
        }
        if (!verified) {

            final TermsOfUse current;
            try {
                current = getCurrentTermsOfUse();
            } catch (ServerException e) {
                LOGGER.warn("This should never happen!", e);
                return;
            }

            DialogUtil.createTermsOfUseDialog(current,
                    user.getTermsOfUseVersion() == null, new DialogUtil.PositiveNegativeCallback() {

                        @Override
                        public void negative() {
                            LOGGER.info("User did not accept the ToU.");
                            Crouton.makeText(activity, activity.getString(R.string
                                    .terms_of_use_cant_continue), Style.ALERT).show();
                            if (callback != null) {
                                callback.negative();
                            }
                        }

                        @Override
                        public void positive() {
                            userAcceptedTermsOfUse(user, current.getIssuedDate());
                            Crouton.makeText(activity, activity.getString(R.string
                                    .terms_of_use_updating_server), Style.INFO).show();
                            if (callback != null) {
                                callback.positive();
                            }
                        }

                    }, activity);
        } else {
            LOGGER.info("User has accpeted ToU in current version.");
        }
    }

    public TermsOfUse getCurrentTermsOfUse() throws ServerException {
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
//                        throw new ServerException(new TimeoutException("Waiting to long for a " +
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

    private void setList(List<TermsOfUse> termsOfUse) {
        LOGGER.info("List of TermsOfUse size: " + termsOfUse.size());
        list = termsOfUse;
    }

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

    /**
     * verify the user's accepted terms of use version
     * against the latest from the server
     *
     * @param acceptedTermsOfUseVersion the accepted version of the current user
     * @return true, if the provided version is the latest
     * @throws ServerException if the server did not respond (as expected)
     */
    public boolean verifyTermsUseOfVersion(
            String acceptedTermsOfUseVersion) throws ServerException {
        if (acceptedTermsOfUseVersion == null)
            return false;

        return getCurrentTermsOfUse().getIssuedDate().equals(acceptedTermsOfUseVersion);
    }

}
