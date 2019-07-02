package org.envirocar.app.views.dialogs;

import android.app.Activity;
import android.text.Html;
import android.text.Spanned;

import com.afollestad.materialdialogs.MaterialDialog;

import org.envirocar.app.R;
import org.envirocar.core.entity.PrivacyStatement;
import org.envirocar.core.entity.User;
import org.envirocar.core.logging.Logger;

import rx.Subscriber;

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
    protected MaterialDialog.Builder createDialogBuilder(Subscriber subscriber) {
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
