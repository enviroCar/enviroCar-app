package org.envirocar.app.views.carselection;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.jakewharton.rxbinding3.widget.RxTextView;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.storage.EnviroCarVehicleDB;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnEditorAction;
import butterknife.OnTextChanged;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class CarSelectionHsnTsnFragment extends BaseInjectorFragment {

    @BindView(R.id.fragment_hsntsn_hsn_input)
    protected AutoCompleteTextView hsnEditText;
    @BindView(R.id.fragment_hsntsn_tsn_input)
    protected AutoCompleteTextView tsnEditText;
    protected BottomSheetFragment bottomSheetFragment;

    @Inject
    EnviroCarVehicleDB enviroCarVehicleDB;
    private Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();
    private Set<String> hsn = new HashSet<>();
    private Set<String> tsn = new HashSet<>();
    private Map<String, Set<String>> mHsnToTsn = new ConcurrentHashMap<>();
    private CompositeDisposable disposable = new CompositeDisposable();
    private static final int ERROR_DEBOUNCE_TIME = 750;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_car_selection_hsn_tsn, container, false);
        ButterKnife.bind(this, view);
        fetchAllVehicles();
        reactiveTexFieldCheck();
        focusChangeListener();

        hsnEditText.setOnItemClickListener((parent, view1, position, id) -> requestNextTextFieldFocus(hsnEditText));
        return view;
    }

    private void requestNextTextFieldFocus(TextView textView) {
        try {
            TextView nextField = (TextView) textView.focusSearch(View.FOCUS_DOWN);
            nextField.requestFocus();
        } catch (Exception e) {

        }
    }

    @OnClick(R.id.fragment_search_vehicle)
    protected void onSearchClicked() {
        String hsn = hsnEditText.getText().toString().trim();
        String tsn = tsnEditText.getText().toString().trim();
        View focusView = null;
        if (hsn.isEmpty()) {
            hsnEditText.setError("empty values");
            focusView = hsnEditText;
        }
        if (tsn.isEmpty()) {
            tsnEditText.setError("empty values");
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

    @OnEditorAction(R.id.fragment_hsntsn_tsn_input)
    protected void implicitSubmit() {
        onSearchClicked();
    }

    private void fetchAllVehicles() {
        Single<List<Vehicles>> vehicle = enviroCarVehicleDB.vehicleDAO().getManufacturerVehicles();
        vehicle.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribeWith(new DisposableSingleObserver<List<Vehicles>>() {
                    @Override
                    public void onSuccess(List<Vehicles> vehicles) {
                        // we have to skip first row
                        for (int i = 1; i < vehicles.size(); i++) {
                            if (!hsn.contains(vehicles.get(i).getManufacturer_id()))
                                hsn.add(vehicles.get(i).getManufacturer_id());
                            if (!mHsnToTsn.containsKey(vehicles.get(i).getManufacturer_id()))
                                mHsnToTsn.put(vehicles.get(i).getManufacturer_id(), new HashSet<>());
                            mHsnToTsn.get(vehicles.get(i).getManufacturer_id()).add(vehicles.get(i).getId());
                        }
                        mainThreadWorker.schedule(() -> {
                            updateHsnView(hsn);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });
    }

    private void updateHsnView(Set<String> hsn) {
        hsnEditText.setAdapter(((CarSelectionActivity) getActivity()).sortedAdapter(getContext(), hsn));
    }

    private void focusChangeListener() {
        hsnEditText.setOnFocusChangeListener((v, focus) -> {
            if (!focus) {
                tsnEditText.setText("");
                String hsn = hsnEditText.getText().toString();
                updateTsnView(hsn);
            }
        });
    }

    private void updateTsnView(String hsn) {
        if (mHsnToTsn.containsKey(hsn)) {
            tsnEditText.setAdapter(((CarSelectionActivity) getActivity()).sortedAdapter(getContext(), mHsnToTsn.get(hsn)));
        } else {
            tsnEditText.setAdapter(null);
        }
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
                        hsnEditText.setError("Not in list");
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
                    ListAdapter adapter = tsnEditText.getAdapter();
                    int flag = 0;
                    for (int i = 0; i < adapter.getCount(); i++) {
                        if (adapter.getItem(i).toString().compareTo(tsn) == 0) {
                            flag = 1;
                            break;
                        }
                    }
                    if (flag == 0) {
                        tsnEditText.setError("Not in list");
                        tsnEditText.requestFocus();
                    } else {
                        tsnEditText.setError(null);
                    }
                }));
    }
}
