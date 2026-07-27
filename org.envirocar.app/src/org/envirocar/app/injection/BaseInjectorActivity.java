/**
 * Copyright (C) 2013 - 2026 the enviroCar community
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

package org.envirocar.app.injection;

import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.MenuItem;

import com.squareup.otto.Bus;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.views.utils.WindowInsetsUtils;

import javax.inject.Inject;


/**
 * @author dewall
 */
public abstract class BaseInjectorActivity extends AppCompatActivity {
    protected abstract void injectDependencies(BaseApplicationComponent baseApplicationComponent);

    // Injected variables.
    @Inject
    protected Bus mBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies(BaseApplication.get(this).getBaseApplicationComponent());
    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        // Apps are displayed edge-to-edge (enforced for apps targeting API 35+, and
        // no longer opt-out-able on API 36). Offset the content for the system bars
        // unless the activity's layout already handles insets itself.
        if (applyDefaultWindowInsets()) {
            WindowInsetsUtils.applySystemBarInsetsAsPadding(this);
        }
    }

    /**
     * Whether the default system-bar inset padding should be applied to this
     * activity's content view. Activities whose layouts already handle insets
     * (e.g. a CoordinatorLayout with {@code android:fitsSystemWindows}) should
     * override this to return {@code false} to avoid doubled padding.
     *
     * @return {@code true} to apply the default inset padding.
     */
    protected boolean applyDefaultWindowInsets() {
        return true;
    }

    @Override
    protected void onStart(){
        super.onStart();

        // Register on the bus.
        mBus.register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Unregister from the bus.
        mBus.unregister(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        super.onOptionsItemSelected(item);
        return false;
    }

}
