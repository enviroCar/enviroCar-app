package org.envirocar.app.views;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.main.BaseApplicationComponent;
import org.envirocar.app.main.BaseMainActivityBottomBar;
import org.envirocar.app.views.carselection.CarSelectionActivity;

import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import io.reactivex.Completable;

/**
 * @author dewall
 */
public class SplashScreenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splashscreen);
        getWindow().setNavigationBarColor(getResources().getColor(R.color.cario_color_primary_dark));
        ButterKnife.bind(this);

        Completable.timer(2000, TimeUnit.MILLISECONDS)
                .subscribe(() -> {
                    Intent intent = new Intent(this, BaseMainActivityBottomBar.class);
                    intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                    this.startActivity(intent);
                    finish();
                });
    }
}
