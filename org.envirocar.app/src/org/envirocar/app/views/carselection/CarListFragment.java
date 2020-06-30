package org.envirocar.app.views.carselection;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import org.envirocar.app.R;
import org.envirocar.core.entity.Vehicles;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CarListFragment extends BottomSheetDialogFragment {

    @BindView(R.id.fragment_car_list_view)
    RecyclerView recyclerView;
   List<Vehicles> vehiclesList;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_car_list, container, false);
        ButterKnife.bind(this, view);
        CarSelectionAttributeListAdapter carListAdapter = new CarSelectionAttributeListAdapter(getContext(), vehiclesList,
                new OnCarInteractionCallback() {

                    @Override
                    public String resolveFuelType(String power_source_id) {
                        return (getContext().getString((((CarSelectionActivity)getActivity()).getFuel(power_source_id)).getStringResource()));
                    }

                    @Override
                    public void addAndRegisterCar(Vehicles vehicle) {
                        ((CarSelectionActivity)getActivity()).registerCar(vehicle);
                    }
                });
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(carListAdapter);
        return view;
    }

    CarListFragment(List<Vehicles> vehiclesList) {
      this.vehiclesList = vehiclesList;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
