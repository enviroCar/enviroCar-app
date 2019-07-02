/**
 * Copyright (C) 2013 - 2019 the enviroCar community
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
package org.envirocar.app.views.dialogs;


import android.app.Activity;
import android.text.Html;
import android.text.Spanned;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.app.exception.NotAcceptedTermsOfUseException;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.User;
import org.envirocar.core.logging.Logger;

import rx.Subscriber;
import rx.functions.Action0;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class ReactiveTermsOfUseDialog extends AbstractReactiveAcceptDialog<TermsOfUse> {
    private static Logger LOG = Logger.getLogger(ReactiveTermsOfUseDialog.class);

    /**
     * Constructor.
     *
     * @param activityContext the current activity context
     * @param entity          the terms of use to show.
     */
    public ReactiveTermsOfUseDialog(Activity activityContext, TermsOfUse entity) {
        this(activityContext, null, entity);
    }

    /**
     * Constructor.
     *
     * @param activityContext
     */
    public ReactiveTermsOfUseDialog(Activity activityContext, User user, TermsOfUse entity) {
        super(activityContext, user, entity);
    }


    @Override
    protected MaterialDialog.Builder createDialogBuilder(Subscriber subscriber) {
        // Create the terms of use dialog.
        if (user != null) {
            boolean firstTime = user.getTermsOfUseVersion() == null;
            return createAcceptDialogBuilder(
                    createTermsOfUseMarkup(entity, firstTime),
                    // OnPositive callback
                    () -> {
                        LOG.info("onClick() the positive button");
                        subscriber.onNext(entity);
                    },
                    // OnNegative callback.
                    () -> {
                        LOG.info("onClick() the negative button.");
                        subscriber.onError(new NotAcceptedTermsOfUseException(
                                activityContext.getString(R.string
                                        .terms_of_use_cant_continue)));
                    });

        } else {
            return createInfoDialogBuilder(createSimpleTermsOfUseMarkup(entity));
        }
    }

    /**
     * Creates the dialog for accepting the terms of use.
     *
     * @param onPositive the action that gets called when the user accepts the terms of use.
     * @param onNegative the action that gets called when the user rejects the terms of use.
     * @return the created dialog instance.
     */
    private MaterialDialog.Builder createAcceptDialogBuilder(Spanned content, Action0 onPositive, Action0 onNegative) {
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

    /**
     * Creates the dialog for showing the terms of use
     *
     * @param content the terms of use string
     * @return
     */
    private MaterialDialog.Builder createInfoDialogBuilder(Spanned content) {
        return new MaterialDialog.Builder(activityContext)
                .title(R.string.terms_of_use_simple)
                .content(content)
                .cancelable(true)
                .negativeText(R.string.ok)
                .onNegative((dialog, which) -> LOG.info("Dialog closed."));
    }

    private Spanned createSimpleTermsOfUseMarkup(TermsOfUse termsOfUse) {
        StringBuilder sb = new StringBuilder();
        sb.append(entity.getContents().replace("</li>", "<br/></li>"));
        return Html.fromHtml(sb.toString());
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
