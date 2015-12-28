package org.envirocar.app.views;


import android.app.Activity;
import android.text.Html;
import android.text.Spanned;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.User;
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
    private final User user;
    private final TermsOfUse currentTermsOfUse;


    /**
     * Constructor.
     *
     * @param activityContext
     */
    public ReactiveTermsOfUseDialog(Activity activityContext, User user, TermsOfUse currentToU) {
        this.activityContext = activityContext;
        this.user = user;
        this.currentTermsOfUse = currentToU;
    }

    public Observable<TermsOfUse> asObservable() {
        LOG.info("asObservable()");
        return Observable.create(new Observable.OnSubscribe<TermsOfUse>() {
            private MaterialDialog termsOfUseDialog;

            @Override
            public void call(Subscriber<? super TermsOfUse> subscriber) {
                LOG.info("asObservable().call()");

                boolean firstTime = user.getTermsOfUseVersion() == null;

                // Create the terms of use dialog.
                MaterialDialog.Builder builder = createDialogBuilder(
                        createTermsOfUseMarkup(currentTermsOfUse, firstTime),
                        // OnPositive callback
                        () -> {
                            LOG.info("onClick() the positive button");
                            subscriber.onNext(currentTermsOfUse);
                        },
                        // OnNegative callback.
                        () -> {
                            LOG.info("onClick() the negative button.");
                            subscriber.onError(new NotAcceptedTermsOfUseException(
                                    activityContext.getString(R.string
                                            .terms_of_use_cant_continue)));
                        });

                // Show the dialog
                mainThreadWorker.schedule(() -> termsOfUseDialog = builder.show());


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
    private MaterialDialog.Builder createDialogBuilder(Spanned content,
                                                       Action0 onPositive, Action0 onNegative) {
        LOG.info("createDialog()");
        return new MaterialDialog.Builder(activityContext)
                .title(R.string.terms_of_use_title)
                .content(content)
                .positiveText(R.string.terms_of_use_accept)
                .negativeText(R.string.terms_of_use_reject)
                .cancelable(false)
                .onPositive((materialDialog, dialogAction) ->
                        backgroundWorker.schedule(onPositive))
                .onNegative((materialDialog, dialogAction) ->
                        backgroundWorker.schedule(onNegative));
    }

    private Spanned createTermsOfUseMarkup(TermsOfUse currentTermsOfUse, boolean firstTime) {
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
