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
import android.text.Html;
import android.text.Spanned;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.entity.User;
import org.envirocar.core.logging.Logger;

import io.reactivex.ObservableEmitter;
import io.reactivex.disposables.Disposable;

/**
 * @author dewall
 */
public class ReactivePrivacyStatementDialog extends AbstractReactiveAcceptDialog<PrivacyStatement> {
    private static Logger LOG = Logger.getLogger(ReactivePrivacyStatementDialog.class);

    /**
     * Constructor.
     *
     * @param activityContext the context of the currently visible activity
     * @param entity          the entity to show the dialog for.
     */
    public ReactivePrivacyStatementDialog(Activity activityContext, PrivacyStatement entity) {
        this(activityContext, null, entity);
    }

    /**
     * Constructor.
     *
     * @param activityContext the context of the currently visible activity
     * @param user            the user entity
     * @param entity          the entity to show the dialog for.
     */
    public ReactivePrivacyStatementDialog(Activity activityContext, User user, PrivacyStatement entity) {
        super(activityContext, user, entity);
    }

    /**
     * @return
     */
    @Override
    protected MaterialDialog.Builder createDialogBuilder(ObservableEmitter<PrivacyStatement> subscriber) {
        return new MaterialDialog.Builder(activityContext)
                .title(R.string.privacy_statement_title)
                .content(createContentMarkup())
                .cancelable(true)
                .positiveText(R.string.ok)
                .onPositive((dialog, which) -> LOG.info("Dialog closed."));
    }

    /**
     * @return
     */
    private Spanned createContentMarkup() {
        StringBuilder builder = new StringBuilder();
        builder.append(entity.getContents().replace("</li>", "<br/></li>"));
        return Html.fromHtml(builder.toString());
    }

}
