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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AutoCompleteTextView;
import android.widget.ListAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jakewharton.rxbinding3.widget.RxTextView;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.databinding.FragmentCarSelectionAttributesBinding;
import org.envirocar.app.databinding.FragmentCarSelectionHsnTsnBinding;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.core.entity.Manufacturers;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.core.logging.Logger;
import org.envirocar.storage.EnviroCarVehicleDB;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;





import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CarSelectionHsnTsnFragment extends BaseInjectorFragment {


    protected AutoCompleteTextView hsnEditText;

    protected AutoCompleteTextView tsnEditText;
    protected BottomSheetFragment bottomSheetFragment;

    private static final Logger LOG = Logger.getLogger(CarSelectionAttributesFragment.class);

    @Inject
    EnviroCarVehicleDB enviroCarVehicleDB;
    private Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private Set<String> hsn = new HashSet<>();
    private Set<String> tsn = new HashSet<>();
    private Map<String, Set<String>> mHsnToTsn = new ConcurrentHashMap<>();
    private CompositeDisposable disposable = new CompositeDisposable();
    private static final int ERROR_DEBOUNCE_TIME = 750;
    List<Manufacturers> manufacturersList;
    private Drawable error;

    CarSelectionHsnTsnFragment(List<Manufacturers> manufacturersList) {
        this.manufacturersList = manufacturersList;
    }

    private FragmentCarSelectionHsnTsnBinding binding;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentCarSelectionHsnTsnBinding.inflate(inflater,container,false);
        View view = binding.getRoot();

        hsnEditText = binding.fragmentHsntsnHsnInput;
        tsnEditText = binding.fragmentHsntsnTsnInput;
        //tsnEditText.onEditorAction(this::implicitSubmit);

        fetchAllVehicles();
        reactiveTexFieldCheck();
        focusChangeListener();
        error = getResources().getDrawable(R.drawable.ic_error_red_24dp);
        error.setBounds(-50, 0, 0, error.getIntrinsicHeight());
        hsnEditText.setOnItemClickListener((parent, view1, position, id) -> requestNextTextFieldFocus(hsnEditText));
        tsnEditText.setOnItemClickListener((parent, view1, position, id) -> requestNextTextFieldFocus(tsnEditText));
        return view;
    }

    @Override
    public void onDestroy() {
        disposable.dispose();
        super.onDestroy();

    }

    private void requestNextTextFieldFocus(TextView textView) {
        try {
            TextView nextField = (TextView) textView.focusSearch(View.FOCUS_DOWN);
            nextField.requestFocus();
        } catch (Exception e) {
            LOG.warn("Unable to find next field or to request focus to next field.");
        }
        hideKeyboard(textView);
    }

    //@OnClick(R.id.fragment_search_vehicle)
    protected void onSearchClicked(View view) {
        String hsnWithManufactureName = hsnEditText.getText().toString().trim();
        String tsn = tsnEditText.getText().toString().trim();
        View focusView = null;
        if (hsn.isEmpty()) {
            hsnEditText.setError(getString(R.string.car_selection_error_empty_input), error);
            focusView = hsnEditText;
        }
        if (tsn.isEmpty()) {
            tsnEditText.setError(getString(R.string.car_selection_error_empty_input), error);
            focusView = tsnEditText;
        }

        //focus on last and stop searching and return
        if (focusView != null) {
            focusView.requestFocus();
            return;
        }

        // also stop searching if already error becuase of values not in list
        if (hsnEditText.getError() != null || tsnEditText.getError() != null)
            return;
        String hsn = hsnWithManufactureName.substring(0, 4);

        Single<Vehicles> vehiclesSingle = enviroCarVehicleDB.vehicleDAO().getHsnTsnVehicle(hsn, tsn);
        vehiclesSingle.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribeWith(new DisposableSingleObserver<Vehicles>() {
                    @Override
                    public void onSuccess(Vehicles vehicles) {
                        bottomSheetFragment = new BottomSheetFragment(vehicles);
                        bottomSheetFragment.show(getFragmentManager(), bottomSheetFragment.getTag());
                        bottomSheetFragment.setShowsDialog(true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("fetchTSNHSN", e.getMessage());
                    }
                });
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    //@OnEditorAction(R.id.fragment_hsntsn_tsn_input)
    protected void implicitSubmit(TextView var1, int var2, KeyEvent var3) {
        onSearchClicked(var1);
    }

    private void fetchAllVehicles() {
        // we have to skip first row
        if (manufacturersList != null)
            for (int i = 1; i < manufacturersList.size(); i++) {
                if (!hsn.contains(manufacturersList.get(i).getId() + " " + manufacturersList.get(i).getName()))
                    hsn.add(manufacturersList.get(i).getId() + " " + manufacturersList.get(i).getName());
            }
        updateHsnView(hsn);
    }

    private void updateHsnView(Set<String> hsn) {
        hsnEditText.setAdapter(((CarSelectionActivity) getActivity()).sortedAdapter(getContext(), hsn));
    }

    private void focusChangeListener() {
        hsnEditText.setOnFocusChangeListener((v, focus) -> {
            if (!focus) {
                tsnEditText.setText("");
                String hsnWithManufactureName = hsnEditText.getText().toString();
                try {
                    String hsn = hsnWithManufactureName.substring(0, 4);
                    updateTsnView(hsn);
                } catch (Exception e) {
                }

            } else {
                // if focus on hsneditText reset error in tsnEditText
                tsnEditText.setError(null);
            }
        });
    }

    private void updateTsnView(String hsn) {

        Observable<List<Vehicles>> getManufacturersVehicles = enviroCarVehicleDB.vehicleDAO().getManufacturerVehiclesId(hsn);

        getManufacturersVehicles.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableObserver<List<Vehicles>>() {
                    @Override
                    public void onNext(List<Vehicles> vehiclesList) {
                        for (int i = 0; i < vehiclesList.size(); i++) {
                            if (!mHsnToTsn.containsKey(vehiclesList.get(i).getManufacturer_id()))
                                mHsnToTsn.put(vehiclesList.get(i).getManufacturer_id(), new HashSet<>());
                            mHsnToTsn.get(vehiclesList.get(i).getManufacturer_id()).add(vehiclesList.get(i).getId());
                        }

                        if (mHsnToTsn.containsKey(hsn)) {
                            tsnEditText.setAdapter(((CarSelectionActivity) getActivity()).sortedAdapter(getContext(), mHsnToTsn.get(hsn)));
                        } else {
                            tsnEditText.setAdapter(null);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("vehicleFetch():",e.getMessage());
                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    private void reactiveTexFieldCheck() {
        disposable.add(RxTextView.textChanges(hsnEditText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(t -> t.toString())
                .subscribe(hsn -> {
                    ListAdapter adapter = hsnEditText.getAdapter();
                    int flag = 0;
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (adapter.getItem(i).toString().compareTo(hsn) == 0) {
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 0) {
                        hsnEditText.setError(getString(R.string.car_selection_error_select_from_list), error);
                        hsnEditText.requestFocus();
                    } else {
                        hsnEditText.setError(null);
                    }
                }));

        disposable.add(RxTextView.textChanges(tsnEditText)
                .skipInitialValue()
                .debounce(ERROR_DEBOUNCE_TIME, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .map(t -> t.toString())
                .subscribe(tsn -> {
                    try {
                        ListAdapter adapter = tsnEditText.getAdapter();
                        int flag = 0;
                        for (int i = 0; i < adapter.getCount(); i++) {
                            if (adapter.getItem(i).toString().compareTo(tsn) == 0) {
                                flag = 1;
                                break;
                            }
                        }

                        if (flag == 0) {
                            tsnEditText.setError(getString(R.string.car_selection_error_select_from_list), error);
                            tsnEditText.requestFocus();
                        } else {
                            tsnEditText.setError(null);
                        }
                    } catch (Exception e) {
                    }
                }));
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}