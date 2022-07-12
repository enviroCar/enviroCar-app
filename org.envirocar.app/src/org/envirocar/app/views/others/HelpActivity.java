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
import android.view.View;

import org.envirocar.app.R;
import org.envirocar.app.databinding.ActivityHelpLayoutBinding;
import org.envirocar.app.databinding.ActivityHelpLayoutGeneralBinding;




/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class HelpActivity extends AppCompatActivity {


    protected Toolbar toolbar;

    private ActivityHelpLayoutGeneralBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHelpLayoutGeneralBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();

        setContentView(view);

        toolbar = binding.activityHelpLayoutGeneralToolbar;

        // Inject views


        // Set Actionbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Help");
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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