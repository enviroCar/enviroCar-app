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
package org.envirocar.core.utils.rx.dialogs;

import android.app.Activity;
import android.text.Spanned;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.core.entity.BaseEntity;
import org.envirocar.core.entity.User;
import org.envirocar.core.logging.Logger;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.ObservableOnSubscribe;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;


/**
 * @author dewall
 */
public abstract class AbstractReactiveAcceptDialog<T extends BaseEntity> {
    private static final Logger LOG = Logger.getLogger(AbstractReactiveAcceptDialog.class);

    // some worker
    protected final Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    protected final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();

    // instance specific variables
    protected final Activity activityContext;
    protected final T entity;
    protected final Params params;

    /**
     * Constructor.
     *
     * @param activityContext the context of the currently visible activity
     * @param entity          the entity to show the dialog for.
     */
    public AbstractReactiveAcceptDialog(Activity activityContext, T entity, Params params) {
        this.activityContext = activityContext;
        this.entity = entity;
        this.params = params;
    }

    /**
     * @return the dialog observable.
     */
    public Observable<T> asObservable() {
        return Observable.create(new ObservableOnSubscribe<T>() {
            private MaterialDialog dialog;

            @Override
            public void subscribe(ObservableEmitter<T> emitter) throws Exception {
                MaterialDialog.Builder builder = createDialogBuilder(emitter, params);
                mainThreadWorker.schedule(() -> dialog = builder.show());

                emitter.setDisposable(new Disposable() {
                    @Override
                    public void dispose() {
                        if (dialog != null) {
                            dialog.dismiss();
                            dialog = null;
                        }
                    }

                    @Override
                    public boolean isDisposed() {
                        return emitter.isDisposed();
                    }
                });
            }
        });
    }

    /**
     * @return the ready designed dialog builder.
     */
    protected MaterialDialog.Builder createDialogBuilder(ObservableEmitter<T> subscriber, Params params) {
        // Create the terms of use dialog.
        if (params.getUser() != null) {
            User user = params.getUser();
            boolean firstTime = user.getTermsOfUseVersion() == null;
            return createAcceptDialogBuilder(createMarkup(entity, firstTime), this.onPositive(subscriber), this.onNegative(subscriber));
        } else {
            return createInfoDialogBuilder(createMarkup(entity, true));
        }
    }

    /**
     * Creates the dialog for accepting the terms of use.
     *
     * @param onPositive the action that gets called when the getUserStatistic accepts the terms of use.
     * @param onNegative the action that gets called when the getUserStatistic rejects the terms of use.
     * @return the created dialog instance.
     */
    private MaterialDialog.Builder createAcceptDialogBuilder(Spanned content, Runnable onPositive, Runnable onNegative) {
        return new MaterialDialog.Builder(activityContext)
                .title(params.getTitleRes())
                .content(content)
                .positiveText(params.getPositiveTextRes())
                .negativeText(params.getNegativeTextRes())
                .cancelable(params.isCancelable())
                .onPositive((materialDialog, dialogAction) -> backgroundWorker.schedule(onPositive))
                .onNegative((materialDialog, dialogAction) -> backgroundWorker.schedule(onNegative));
    }

    /**
     * Creates the dialog for showing the terms of use
     *
     * @param content the terms of use string
     * @return
     */
    private MaterialDialog.Builder createInfoDialogBuilder(Spanned content) {
        return new MaterialDialog.Builder(activityContext)
//                .title(params.getTitleRes())
                .content(content)
                .cancelable(true)
                .negativeText(params.getPositiveTextRes())
                .onPositive((dialog, which) -> LOG.info("Dialog closed."));
    }

    /**
     * @param entity
     * @param firstTime
     * @return markup content for the dialog
     */
    protected abstract Spanned createMarkup(T entity, boolean firstTime);

    /**
     * @return runnable for positive click events.
     */
    protected abstract Runnable onPositive(final ObservableEmitter<T> subscriber);

    /**
     * @return runnable for negative click events.
     */
    protected abstract Runnable onNegative(final ObservableEmitter<T> subscriber);

    /**
     * Class that contains the configuration parameters for the dialog.
     */
    public static class Params {
        private final User user;
        private final int titleRes;
        private final int positiveTextRes;
        private final int negativeTextRes;
        private final boolean cancelable;

        public Params(User user, int titleRes, int positiveTextRes, boolean cancelable) {
            this(user, titleRes, positiveTextRes, 0, cancelable);
        }

        public Params(User user, int titleRes, int positiveTextRes, int negativeTextRes, boolean cancelable) {
            this.user = user;
            this.titleRes = titleRes;
            this.positiveTextRes = positiveTextRes;
            this.negativeTextRes = negativeTextRes;
            this.cancelable = cancelable;
        }

        public User getUser() {
            return user;
        }

        public int getTitleRes() {
            return titleRes;
        }

        public int getPositiveTextRes() {
            return positiveTextRes;
        }

        public int getNegativeTextRes() {
            return negativeTextRes;
        }

        public boolean isCancelable() {
            return cancelable;
        }
    }
}
