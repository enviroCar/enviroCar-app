package org.envirocar.app.views;


import android.app.Activity;
import android.text.Html;
import android.text.Spanned;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.app.exception.NotLoggedInException;
import org.envirocar.app.exception.ServerException;
import org.envirocar.app.handler.TermsOfUseManager;
import org.envirocar.app.handler.UserHandler;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.logging.Logger;

import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.schedulers.Schedulers;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class ReactiveTermsOfUseDialog {
    private static Logger LOG = Logger.getLogger(ReactiveTermsOfUseDialog.class);

    private final Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread()
            .createWorker();
    private final Scheduler.Worker backgroundWorker = Schedulers.io().createWorker();


    private final Activity activityContext;
    private final UserHandler userHandler;
    private final TermsOfUseManager termsOfUseManager;

    /**
     * Constructor.
     *
     * @param activityContext
     */
    public ReactiveTermsOfUseDialog(Activity activityContext, UserHandler userHandler,
                                    TermsOfUseManager termsOfUseManager) {
        this.activityContext = activityContext;
        this.userHandler = userHandler;
        this.termsOfUseManager = termsOfUseManager;
    }

    public <T> Observable<T> asObservable(T t) {
        LOG.info("asObservable()");
        return Observable.create(new Observable.OnSubscribe<T>() {
            private MaterialDialog termsOfUseDialog;

            @Override
            public void call(Subscriber<? super T> subscriber) {
                LOG.info("asObservable().call()");

                // Check whether the user is correctly logged in.
                if (!userHandler.isLoggedIn())
                    subscriber.onError(new NotLoggedInException("No user is logged in."));

                try {
                    // Create the terms of use dialog.
                    MaterialDialog.Builder builder = createDialogBuilder(
                            () -> {
                                LOG.info("onClick() the positive button");
                                subscriber.onNext(t);
                            }, () -> {
                                LOG.info("onClick() the negative button.");
                                subscriber.onError(new NotAcceptedTermsOfUseException(
                                        activityContext.getString(R.string
                                                .terms_of_use_cant_continue)));
                            });

                    // Show the dialog
                    mainThreadWorker.schedule(() -> termsOfUseDialog = builder.show());
                } catch (ServerException e) {
                    LOG.error("Error while creating terms of use dialog = " + e.getMessage(), e);
                    subscriber.onError(e);
                    return;
                }


                // Add an additional subscription to the subscriber that dismisses the terms of
                // use dialog on unsubscribe.
                subscriber.add(new Subscription() {
                    @Override
                    public void unsubscribe() {
                        if (termsOfUseDialog != null)
                            termsOfUseDialog.dismiss();
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
     * Creates the dialog for accepting the terms of use.
     *
     * @param onPositive the action that gets called when the user accepts the terms of use.
     * @param onNegative the action that gets called when the user rejects the terms of use.
     * @return the created dialog instance.
     */
    private MaterialDialog.Builder createDialogBuilder(Action0 onPositive, Action0 onNegative)
            throws ServerException {
        LOG.info("createDialog()");
        return new MaterialDialog.Builder(activityContext)
                .title(R.string.terms_of_use_title)
                .content(createTermsOfUseMarkup())
                .positiveText(R.string.terms_of_use_accept)
                .negativeText(R.string.terms_of_use_reject)
                .cancelable(false)
                .onPositive((materialDialog, dialogAction) ->
                        backgroundWorker.schedule(onPositive))
                .onNegative((materialDialog, dialogAction) ->
                        backgroundWorker.schedule(onNegative));
    }

    private Spanned createTermsOfUseMarkup() throws ServerException {
        boolean firstTime = userHandler.getUser().getTermsOfUseVersion() == null;
        TermsOfUse currentTermsOfUse = termsOfUseManager.getCurrentTermsOfUse();

        StringBuilder sb = new StringBuilder();

        sb.append("<p>");
        if (!firstTime) {
            sb.append(activityContext.getString(R.string.terms_of_use_sorry));
        } else {
            sb.append(activityContext.getString(R.string.terms_of_use_info));
        }
        sb.append(":</p>");
        sb.append(currentTermsOfUse.getContents().replace("</li>", "<br/></li>"));

        return Html.fromHtml(sb.toString());
    }
}
