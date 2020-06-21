package org.envirocar.app.views.carselection;


import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.injection.BaseInjectorFragment;
import org.envirocar.core.entity.Vehicles;
import org.envirocar.storage.EnviroCarVehicleDB;

import java.util.List;

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
    protected EditText hsnEditText;
    @BindView(R.id.fragment_hsntsn_tsn_input)
    protected EditText tsnEditText;

    @Inject
    EnviroCarVehicleDB enviroCarVehicleDB;
    private Scheduler.Worker mainThreadWorker = AndroidSchedulers.mainThread().createWorker();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_car_selection_hsn_tsn,container,false);
        ButterKnife.bind(this,view);
        return view;
    }

    @OnClick(R.id.fragment_search_vehicle)
    protected void onSearchClicked() {
        String hsn = hsnEditText.getText().toString().trim();
        String tsn = tsnEditText.getText().toString().trim();
        Single<Vehicles> vehiclesSingle = enviroCarVehicleDB.vehicleDAO().getHsnTsnVehicle(hsn,tsn);
        vehiclesSingle.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribeWith(new DisposableSingleObserver<Vehicles>() {
                    @Override
                    public void onSuccess(Vehicles vehicles) {
                        mainThreadWorker.schedule(()->{
                            Toast.makeText(getContext(),"negi"+vehicles.getEngine_capacity(),Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.i("fetchTSNHSN",e.getMessage());
                    }
                });
    }

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

}
