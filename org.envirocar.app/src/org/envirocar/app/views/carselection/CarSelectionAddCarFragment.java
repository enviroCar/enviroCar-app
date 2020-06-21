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
package org.envirocar.app.views.carselection;


import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.logging.Logger;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CarSelectionAddCarFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(CarSelectionAddCarFragment.class);

    private static final int ERROR_DEBOUNCE_TIME = 750;

    @BindView(R.id.envirocar_toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.activity_car_selection_newcar_toolbar_exp)
    protected View toolbarExp;
    @BindView(R.id.activity_car_selection_newcar_content_view)
    protected View contentView;
    @BindView(R.id.activity_car_selection_top)
    protected View topView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.activity_car_selection_newcar_fragment, container, false);
        ButterKnife.bind(this, view);

        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        toolbar.inflateMenu(R.menu.menu_logbook_add_fueling);
        toolbar.setNavigationOnClickListener(v -> {
            hideKeyboard(v);
            closeThisFragment();
        });

        return view;
    }

    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();
        ECAnimationUtils.animateShowView(getContext(), toolbar,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), toolbarExp,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), contentView,
                R.anim.translate_slide_in_bottom_fragment);
    }

    @Override
    public void onDestroy() {
        LOG.info("onDestroy()");

        super.onDestroy();
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    public void closeThisFragment() {
        // ^^
        ECAnimationUtils.animateHideView(getContext(),
                ((CarSelectionActivity) getActivity()).overlayView, R.anim.fade_out);
        ECAnimationUtils.animateHideView(getContext(), R.anim
                .translate_slide_out_top_fragment, toolbar, toolbarExp);
        ECAnimationUtils.animateHideView(getContext(), contentView, R.anim
                .translate_slide_out_bottom, () -> ((CarSelectionUiListener) getActivity()).onHideAddCarFragment());
        ECAnimationUtils.animateHideView(getContext(),topView,R.anim.translate_slide_out_right);
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}