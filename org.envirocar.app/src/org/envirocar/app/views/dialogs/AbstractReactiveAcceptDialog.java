/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.app.views.dialogs;

import android.app.Activity;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.core.entity.BaseEntity;
import org.envirocar.core.entity.User;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;


/**
 * @author dewall
 */
public abstract class AbstractReactiveAcceptDialog<T extends BaseEntity> {
    // some worker
    protected final Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    protected final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();

    // instance specific variables
    protected final Activity activityContext;
    protected final User user;
    protected final T entity;

    /**
     * Constructor.
     *
     * @param activityContext the context of the currently visible activity
     * @param entity          the entity to show the dialog for.
     */
    public AbstractReactiveAcceptDialog(Activity activityContext, User user, T entity) {
        this.activityContext = activityContext;
        this.user = user;
        this.entity = entity;
    }

    /**
     * @return the dialog observable.
     */
    public Observable<T> asObservable() {
        return Observable.create(new ObservableOnSubscribe<T>() {
            private MaterialDialog dialog;

            @Override
            public void subscribe(ObservableEmitter<T> emitter) throws Exception {
                MaterialDialog.Builder builder = createDialogBuilder(emitter);
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
    protected abstract MaterialDialog.Builder createDialogBuilder(ObservableEmitter<T> subscriber);
}
