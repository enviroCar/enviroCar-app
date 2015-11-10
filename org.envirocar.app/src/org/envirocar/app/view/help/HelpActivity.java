package org.envirocar.app.view.help;

import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.core.injection.BaseInjectorActivity;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class HelpActivity extends BaseInjectorActivity {

    @InjectView(R.id.activity_help_test_test)
    protected TextView test;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_help_test);

        ButterKnife.inject(this);

        test.setText(Html.fromHtml("<li>first item</li><li>item 2</li>"));
    }
}
