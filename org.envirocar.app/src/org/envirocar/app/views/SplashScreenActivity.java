package org.envirocar.app.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.envirocar.app.R;
import org.envirocar.core.logging.Logger;

import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import io.reactivex.Completable;

/**
 * @author dewall
 */
public class SplashScreenActivity extends Activity {
    private static final Logger LOG = Logger.getLogger(SplashScreenActivity.class);
    private static final String HAS_BEEN_SEEN_KEY = "has_been_seen";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null && savedInstanceState.getBoolean(HAS_BEEN_SEEN_KEY, false)){
            startMainActivity();
            return;
        }

        setContentView(R.layout.activity_splashscreen);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.cario_color_primary_dark));
        ButterKnife.bind(this);

        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            finish();
            return;
        }

        Completable.timer(2000, TimeUnit.MILLISECONDS).subscribe(() -> startMainActivity());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        LOG.info("onSaveInstanceState()");
        super.onSaveInstanceState(outState);
        outState.putBoolean(HAS_BEEN_SEEN_KEY, true);
    }

    private void startMainActivity(){
        Intent intent = new Intent(this, BaseMainActivity.class);
        this.startActivity(intent);
        finish();
    }
}
