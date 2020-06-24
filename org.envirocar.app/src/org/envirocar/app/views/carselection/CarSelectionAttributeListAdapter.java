package org.envirocar.app.views.carselection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.envirocar.app.R;
import org.envirocar.app.views.utils.DialogUtils;
import org.envirocar.core.entity.Car;
import org.envirocar.core.entity.Vehicles;

import java.util.List;
import java.util.zip.Inflater;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CarSelectionAttributeListAdapter extends RecyclerView.Adapter<CarSelectionAttributeListAdapter.CarSelectionViewHolder> {


    private Context context;
    private final List<Vehicles> vehiclesList;
    private OnCarInteractionCallback mCallback;

    public CarSelectionAttributeListAdapter(Context mContext, List<Vehicles> vehicles, OnCarInteractionCallback mCallback) {
        this.context = mContext;
        this.vehiclesList = vehicles;
        this.mCallback = mCallback;
    }

    @NonNull
    @Override
    public CarSelectionAttributeListAdapter.CarSelectionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.car_detail_card_layout, parent, false);
        return new CarSelectionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CarSelectionAttributeListAdapter.CarSelectionViewHolder holder, int position) {
        final Vehicles vehicle = vehiclesList.get(position);

        // get fuel type name using power source id
        String fuelType = mCallback.resolveFuelType(vehicle.getPower_source_id());
        View contentView = LayoutInflater.from(context).inflate(
                R.layout.fragment_tracklist_delete_track_dialog, null, false);

        // set the details of vehicles in card view
        holder.hsnValue.setText(vehicle.getManufacturer_id());
        holder.tsnValue.setText(vehicle.getId());
        holder.manufacturerName.setText(vehicle.getManufacturer());
        holder.vehicleName.setText(vehicle.getCommerical_name());
        holder.constructionYear.setText(vehicle.getAllotment_date());
        holder.fuelType.setText(fuelType);
        holder.engineCapacity.setText(vehicle.getEngine_capacity());
        holder.power.setText(vehicle.getPower());

        holder.carDetailView.setOnClickListener(v -> {
            Car car = mCallback.createCar(vehicle);
            DialogUtils.createDefaultDialogBuilder(context,
                    "Create car?",
                    R.drawable.ic_directions_car_black_24dp,
                    contentView)
                    .positiveText("Create")
                    .negativeText("Cancel")
                    .onPositive((materialDialog,dialogAction)->{
                        mCallback.addAndRegisterCar(car);
                    });
        });
    }

    @Override
    public int getItemCount() {
        return 0;
    }

    public class CarSelectionViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.car_layout_manufacturer_name)
        TextView manufacturerName;
        @BindView(R.id.car_layout_vehcile_name)
        TextView vehicleName;
        @BindView(R.id.car_layout_construction_year)
        TextView constructionYear;
        @BindView(R.id.car_layout_fuel_type)
        TextView fuelType;
        @BindView(R.id.car_layout_engine_capacity)
        TextView engineCapacity;
        @BindView(R.id.car_layout_power)
        TextView power;
        @BindView(R.id.car_layout_hsn)
        TextView hsnValue;
        @BindView(R.id.car_layout_tsn)
        TextView tsnValue;
        @BindView(R.id.car_layout_card)
        View carDetailView;

        public CarSelectionViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
