package org.envirocar.app.views.carselection;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
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
        holder.hsnTsn.setText(vehicle.getManufacturer_id() + "/" + vehicle.getId());
        holder.manufacturerName.setText(vehicle.getManufacturer());
        holder.vehicleName.setText(vehicle.getCommerical_name());
        holder.constructionYear.setText(vehicle.getAllotment_date());
        holder.fuelType.setText(fuelType);
        if (fuelType.equalsIgnoreCase("electric")) {
            holder.engineView.setVisibility(View.GONE);

        } else
            holder.engineCapacity.setText(vehicle.getEngine_capacity() + " cm\u00B3");
        holder.power.setText(vehicle.getPower() + " KW");
        holder.imageView1.setTag("downTag");
        holder.imageView1.setOnClickListener(view -> {

            if (holder.imageView1.getTag() == "downTag") {
                holder.imageView1.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_keyboard_arrow_up_24));
                holder.imageView1.setTag("upTag");
            } else {
                holder.imageView1.setImageDrawable(context.getDrawable(R.drawable.ic_baseline_keyboard_arrow_down_24));
                holder.imageView1.setTag("downTag");
            }
            if (holder.expandCard.getVisibility() == View.VISIBLE) {
                holder.expandCard.setVisibility(View.GONE);
            } else {
                holder.expandCard.setVisibility(View.VISIBLE);
            }
        });

        holder.carDetailView.setOnClickListener(v -> {

            DialogUtils.createDefaultDialogBuilder(context,
                    R.string.create_car_dialog,
                    R.drawable.ic_directions_car_black_24dp,
                    holder.manufacturerName.getText() + " " + holder.vehicleName.getText() +
                            " " + holder.fuelType.getText() + " " + holder.engineCapacity.getText() + " ")
                    .positiveText(R.string.ok)
                    .onPositive((dialog, which) -> {
                        mCallback.addAndRegisterCar(vehicle);
                    })
                    .negativeText(R.string.cancel)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return vehiclesList.size();
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
        @BindView(R.id.car_layout_card)
        View carDetailView;
        @BindView(R.id.expandView)
        ImageView imageView1;
        @BindView(R.id.car_layout_engine_view)
        View engineView;
        @BindView(R.id.car_layout_expanded_card)
        View expandCard;
        @BindView(R.id.car_layout_hsn_tsn)
        TextView hsnTsn;

        public CarSelectionViewHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }
    }
}
