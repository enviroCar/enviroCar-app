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
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.app.views.utils.ECAnimationUtils;
import org.envirocar.core.entity.Manufacturers;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarVehicleDB;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import info.hoang8f.android.segmented.SegmentedGroup;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CarSelectionAddCarFragment extends BaseInjectorFragment {
    private static final Logger LOG = Logger.getLogger(CarSelectionAddCarFragment.class);


    @BindView(R.id.envirocar_toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.activity_car_selection_newcar_toolbar_exp)
    protected View toolbarExp;
    @BindView(R.id.activity_car_selection_top)
    protected View topView;
    @BindView(R.id.carSelectionSegmentedGroup)
    protected SegmentedGroup segmentedGroup;
    @BindView(R.id.activity_car_selection_newcar_content_view)
    protected ViewPager mViewPager;

    @Inject
    EnviroCarVehicleDB enviroCarVehicleDB;

    private CarSelectionPagerAdapter pagerAdapter;
    private List<Manufacturers> manufacturersList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.activity_car_selection_newcar_fragment, container, false);
        ButterKnife.bind(this, view);

        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        toolbar.setNavigationOnClickListener(v -> {
            hideKeyboard(v);
            closeThisFragment();
        });

        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    segmentedGroup.check(R.id.HsnTsnSegmentedButton);
                } else {
                    segmentedGroup.check(R.id.attributesSegmentedButton);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        segmentedGroup.check(R.id.HsnTsnSegmentedButton);

        segmentedGroup.setOnCheckedChangeListener((radioGroup, i) -> {
            switch (i) {
                case R.id.HsnTsnSegmentedButton:
                    mViewPager.setCurrentItem(0);
                    break;
                case R.id.attributesSegmentedButton:
                    mViewPager.setCurrentItem(1);
                    break;
                default:
                    break;

            }
        });

        return view;
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
        ECAnimationUtils.animateShowView(getContext(), toolbar,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), toolbarExp,
                R.anim.translate_slide_in_top_fragment);
        ECAnimationUtils.animateShowView(getContext(), mViewPager,
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
                        mViewPager.setAdapter(pagerAdapter);
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

        ECAnimationUtils.animateHideView(getContext(),
                ((CarSelectionActivity) getActivity()).overlayView, R.anim.fade_out);
        ECAnimationUtils.animateHideView(getContext(), R.anim
                .translate_slide_out_top_fragment, toolbar, toolbarExp);
        ECAnimationUtils.animateHideView(getContext(), mViewPager, R.anim
                .translate_slide_out_bottom, () -> ((CarSelectionUiListener) getActivity()).onHideAddCarFragment());
        ECAnimationUtils.animateHideView(getContext(), topView, R.anim.translate_slide_out_right);
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