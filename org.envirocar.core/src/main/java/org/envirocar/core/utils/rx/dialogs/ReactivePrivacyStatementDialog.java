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
package org.envirocar.core.utils.rx.dialogs;

import android.app.Activity;
import android.text.Html;
import android.text.Spanned;

import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.exception.TermsOfUseException;
import org.envirocar.core.logging.Logger;

import io.reactivex.ObservableEmitter;

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
    public ReactivePrivacyStatementDialog(Activity activityContext, PrivacyStatement entity, Params params) {
        super(activityContext, entity, params);
    }

    @Override
    protected Spanned createMarkup(PrivacyStatement entity, boolean firstTime) {
        StringBuilder sb = new StringBuilder();
        sb.append(entity.getContents().replace("</li>", "<br/></li>"));
        return Html.fromHtml(sb.toString());
    }

    @Override
    protected Runnable onPositive(final ObservableEmitter<PrivacyStatement> subscriber) {
        return () -> {
            LOG.info("onClick() the positive button");
            subscriber.onNext(entity);
        };
    }

    @Override
    protected Runnable onNegative(final ObservableEmitter<PrivacyStatement> subscriber) {
        return () -> {
            LOG.info("onClick() the negative button.");
            subscriber.onError(new TermsOfUseException());
        };
    }

}
