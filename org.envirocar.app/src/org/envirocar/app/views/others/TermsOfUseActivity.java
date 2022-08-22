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
package org.envirocar.app.views.others;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.widget.TextView;
import android.text.Html;
import android.text.Spanned;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import org.envirocar.app.R;
import org.envirocar.app.handler.agreement.AgreementManager;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.core.utils.rx.Optional;
import org.envirocar.core.entity.TermsOfUse;
import org.envirocar.core.entity.User;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.interactor.GetLatestTermsOfUse;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.core.utils.rx.Optional;
import org.envirocar.app.handler.preferences.UserPreferenceHandler;
import org.envirocar.voicecommand.handler.MetadataHandler;

import butterknife.ButterKnife;
import butterknife.BindView;

import java.util.function.Consumer;
import javax.inject.Inject;

/**
 * TODO JavaDoc
 *
 * @author matthesrieke
 */
public class TermsOfUseActivity extends BaseInjectorActivity {

    private static final Logger LOG = Logger.getLogger(TermsOfUseActivity.class);

    @BindView(R.id.tou_text_view)
    protected TextView touTextView;

    @BindView(R.id.activity_tou_layout_toolbar)
    protected Toolbar toolbar;

    @Inject
    protected GetLatestTermsOfUse getLatestTermsOfUse;

    @Inject
    protected AgreementManager mAgreementManager;

    @Inject
    protected UserPreferenceHandler userHandler;

    @Inject
    protected MetadataHandler metadataHandler;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tou_layout);

        // Inject views
        ButterKnife.bind(this);

        // Set Actionbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(getString(R.string.terms_of_use_simple));
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // get tou html content
        String htmlAsString = "<h2>Loading...</h2>";
        Spanned htmlAsSpanned = Html.fromHtml(htmlAsString);

        // set the html content on a TextView
        TextView textView = (TextView) findViewById(R.id.tou_text_view);
        textView.setText(htmlAsSpanned);

        getLatestTermsOfUse.asObservable()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(termsOfUse -> {
                LOG.info("Terms Of Use loaded: " + termsOfUse);
                if (!termsOfUse.isEmpty()) {
                    Spanned htmlContent = Html.fromHtml(termsOfUse.getOptional().getContents());
                    textView.setText(htmlContent);
                }
            });

        // check if the user accepted the latest terms
        LOG.info("Checking TermsOfUse");
        mAgreementManager.verifyTermsOfUse(null, true)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(termsOfUse -> {
                if (termsOfUse != null) {
                    LOG.warn("TermsOfUse verified");
                } else {
                    LOG.warn("No TermsOfUse received from verification");
                    initAcceptanceWorkflow();
                }
            }, e -> {
                LOG.warn("Error during TermsOfUse verification", e);
                initAcceptanceWorkflow();
            });

        // set `isDashboardFragment` to false
        metadataHandler.makeIsDashboardFragmentFalse();
    }

    private void initAcceptanceWorkflow() {
        LOG.info("initializeTermsOfUseAcceptanceWorkflow from ToU Activity");
        User user = userHandler.getUser();
        mAgreementManager.initializeTermsOfUseAcceptanceWorkflow(user, this, null, new Consumer<Optional<TermsOfUse>>() {
            public void accept(Optional<TermsOfUse> tou) {
                if (tou.isEmpty()) {
                    LOG.info("User did not accept ToU");
                } else {
                    LOG.info("User accepted ToU");
                }   
            }
        });
    }

    private TermsOfUse resolveTermsOfUse() {
        TermsOfUse tous;
        try {
            tous = mAgreementManager.verifyTermsOfUse(this, true)
                .subscribeOn(Schedulers.io())
                .doOnError(LOG::error)
                .blockingFirst();
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
            tous = null;
        }
        return tous;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        super.onOptionsItemSelected(item);
        return false;
    }
}
