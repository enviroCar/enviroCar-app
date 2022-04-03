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
package org.envirocar.app.views.carselection;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.databinding.ActivityCarSelectionNewcarFragmentBinding;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Manufacturers;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarVehicleDB;

import java.util.List;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

public class CarSelectionAddCarFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(CarSelectionAddCarFragment.class);
    @Inject
    EnviroCarVehicleDB enviroCarVehicleDB;
    private CarSelectionPagerAdapter pagerAdapter;
    private List<Manufacturers> manufacturersList;

    private ActivityCarSelectionNewcarFragmentBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        binding = DataBindingUtil.inflate(inflater, R.layout.activity_car_selection_newcar_fragment, container, false);

        binding.envirocarToolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);

        binding.envirocarToolbar.setNavigationOnClickListener(v -> {
            hideKeyboard(v);
            closeThisFragment();
        });

        binding.getRoot().setOnClickListener(v -> hideKeyboard(binding.getRoot()));

        binding.mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    binding.segmentedGroup.check(R.id.HsnTsnSegmentedButton);
                } else {
                    binding.segmentedGroup.check(R.id.attributesSegmentedButton);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        binding.segmentedGroup.check(R.id.HsnTsnSegmentedButton);

        binding.segmentedGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i) {
                case R.id.HsnTsnSegmentedButton:
                    binding.mViewPager.setCurrentItem(0);
                    break;
                case R.id.attributesSegmentedButton:
                    binding.mViewPager.setCurrentItem(1);
                    break;
                default:
                    break;

            }
        });

        return binding.getRoot();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        fetchVehicles();
    }

    @Override
    public void onResume() {
        LOG.info("onResume()");
        super.onResume();
        ECAnimationUtils.animateShowView(getContext(), binding.envirocarToolbar,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), binding.toolbarExp,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), binding.mViewPager,
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

    private void fetchVehicles() {
        Observable<List<Manufacturers>> manufacturers = enviroCarVehicleDB.manufacturersDAO().getAllManufacturers();
        manufacturers.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<List<Manufacturers>>() {
                    @Override
                    public void onNext(List<Manufacturers> manufacturersList1) {
                        manufacturersList = manufacturersList1;
                        pagerAdapter = new CarSelectionPagerAdapter(getChildFragmentManager());
                        binding.mViewPager.setAdapter(pagerAdapter);
                    }

                    @Override
                    public void onError(Throwable e) {
                        LOG.info("manufactureFetch() :", e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });


    }

    public void closeThisFragment() {

        ECAnimationUtils.animateHideView(getContext(), R.anim
                .translate_slide_out_top_fragment, binding.envirocarToolbar, binding.toolbarExp);
        ECAnimationUtils.animateHideView(getContext(), binding.mViewPager, R.anim
                .translate_slide_out_bottom, () -> ((CarSelectionUiListener) getActivity()).onHideAddCarFragment());
        ECAnimationUtils.animateHideView(getContext(), binding.topView, R.anim.translate_slide_out_right);
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    class CarSelectionPagerAdapter extends FragmentStatePagerAdapter {

        private static final int PAGES = 2;
        private CarSelectionHsnTsnFragment hsnTsnFragment;
        private CarSelectionAttributesFragment attributesFragment;

        public CarSelectionPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
            hsnTsnFragment = new CarSelectionHsnTsnFragment(manufacturersList);
            attributesFragment = new CarSelectionAttributesFragment(manufacturersList);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            if (position == 0) {
                return hsnTsnFragment;
            } else {
                return attributesFragment;
            }
        }

        @Override
        public int getCount() {
            return PAGES;
        }
    }
}