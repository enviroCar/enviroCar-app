package org.envirocar.app.views.carselection;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.storage.EnviroCarVehicleDB;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_car_selection_hsn_tsn, container, false);
        ButterKnife.bind(this, view);
        fetchAllVehicles();
        return view;
    }

    @OnClick(R.id.fragment_search_vehicle)
    protected void onSearchClicked() {
        String hsn = hsnEditText.getText().toString().trim();
        String tsn = tsnEditText.getText().toString().trim();
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

    private void fetchAllVehicles() {
        Single<List<Vehicles>> vehicle = enviroCarVehicleDB.vehicleDAO().getManufacturerVehicles();
        vehicle.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribeWith(new DisposableSingleObserver<List<Vehicles>>() {
                    @Override
                    public void onSuccess(List<Vehicles> vehicles) {
                        for (Vehicles vehicles1 : vehicles) {
                            if (!hsn.contains(vehicles1.getManufacturer_id()))
                                hsn.add(vehicles1.getManufacturer_id());
                            if (!tsn.contains(vehicles1.getId()))
                                tsn.add(vehicles1.getId());
                        }
                        mainThreadWorker.schedule(() -> {
                            updateView(hsn, tsn);
                        });
                    }

                    @Override
                    public void onError(Throwable e) {

                    }
                });
    }

    private void updateView(Set<String> hsn, Set<String> tsn) {
        hsnEditText.setAdapter(((CarSelectionActivity) getActivity()).sortedAdapter(getContext(), hsn));
        tsnEditText.setAdapter(((CarSelectionActivity) getActivity()).sortedAdapter(getContext(), tsn));
    }
}
