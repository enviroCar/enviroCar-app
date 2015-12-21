package org.envirocar.app.views;


import android.content.Context;
import android.text.Spanned;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.core.logging.Logger;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class MaterialDialogObservable {
    private static Logger LOG = Logger.getLogger(MaterialDialogObservable.class);

    public static Observable<Boolean> createTermsOfUseDialogObservable(Context context, Spanned
            spanned) {
        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                MaterialDialog dialog = new MaterialDialog.Builder(context)
                        .title(R.string.terms_of_use_title)
                        .content(spanned)
                        .onPositive((materialDialog, dialogAction) -> {
                            LOG.info("onClick() the positive button");

                            subscriber.onNext(true);
                        })
                        .onNegative((materialDialog, dialogAction) -> {
                            LOG.info("onClick() the negative button. Cancel the trackUpload");
                            subscriber.onNext(false);
                            subscriber.onError(new NotAcceptedTermsOfUseException(
                                    context.getString(R.string.terms_of_use_cant_continue)));
                        })
                        .cancelable(false)
                        .show();

                subscriber.add(new Subscription() {
                    @Override
                    public void unsubscribe() {
                        dialog.dismiss();
                    }

                    @Override
                    public boolean isUnsubscribed() {
                        return subscriber.isUnsubscribed();
                    }
                });
            }
        });
    }


}
