package org.envirocar.app.views.dialogs;

import android.app.Activity;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.core.entity.BaseEntity;
import org.envirocar.core.entity.User;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;


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
        return Observable.create(new Observable.OnSubscribe<T>() {
            private MaterialDialog dialog;

            @Override
            public void call(Subscriber<? super T> subscriber) {
                MaterialDialog.Builder builder = createDialogBuilder(subscriber);
                mainThreadWorker.schedule(() -> dialog = builder.show());

                subscriber.add(new Subscription() {
                    @Override
                    public void unsubscribe() {
                        if (dialog != null) {
                            dialog.dismiss();
                        }
                    }

                    @Override
                    public boolean isUnsubscribed() {
                        return subscriber.isUnsubscribed();
                    }
                });
            }
        });
    }

    /**
     * @return the ready designed dialog builder.
     */
    protected abstract MaterialDialog.Builder createDialogBuilder(Subscriber subscriber);
}
