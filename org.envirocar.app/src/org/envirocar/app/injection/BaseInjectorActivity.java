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
package org.envirocar.app.injection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationItemView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.squareup.otto.Bus;

import org.envirocar.app.BaseApplication;
import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;

import javax.inject.Inject;

import butterknife.BindView;


/**
 * @author dewall
 */
public abstract class BaseInjectorActivity extends AppCompatActivity {
    protected abstract void injectDependencies(BaseApplicationComponent baseApplicationComponent);

    // Injected variables.
    @Inject
    protected Bus mBus;

    // BindView variables.
    @BindView(R.id.navigation)
    protected BottomNavigationView navigationBottomBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        injectDependencies(BaseApplication.get(this).getBaseApplicationComponent());
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

    public void GotoMYTRACKS(View view) {
        navigationBottomBar.setSelectedItemId(R.id.navigation_my_tracks);
    }
}
