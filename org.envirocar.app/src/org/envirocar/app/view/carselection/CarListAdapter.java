package org.envirocar.app.view.carselection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.app.model.Car;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * @author dewall
 */
public class CarListAdapter extends ArrayAdapter<Car> {
    private final Context mContext;
    private final List<Car> mCars;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     * @param values  the values to show in the list.
     */
    public CarListAdapter(Context context, List<Car> values) {
        super(context, -1, values);
        this.mContext = context;
        this.mCars = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // First get the car for which the view needs to be created.
        final Car car = mCars.get(position);

        // Then inflate a new view for the car and create a holder
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context
                .LAYOUT_INFLATER_SERVICE);

        CarViewHolder holder = null;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout
                    .activity_car_selection_layout_carlist_entry, parent, false);
            holder = new CarViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (CarViewHolder) convertView.getTag();
        }

        // set the views
//        if(car.getManufacturer().equals("Mercedes Benz"))
//            holder.mIconView.setImageResource(R.drawable.ic_check_black_24dp);
        holder.mFirstLineText.setText(String.format("%s - %s", car.getManufacturer(), car
                .getModel()));
        holder.mSecondLineText.setText(String.format("%s %s ccm", car.getFuelType().toString(),
                car.getEngineDisplacement()));
        holder.mYearText.setText(Integer.toString(car.getConstructionYear()));
        holder.mGasolineText.setText(car.getFuelType().toString());
        holder.mEngineText.setText(String.format("%s ccm",
                Integer.toString(car.getEngineDisplacement())));

        // Return the created view.
        return convertView;
    }

    @Override
    public Car getItem(int position) {
        return mCars.get(position);
    }

    /**
     * Adds a new {@link Car} to the list and finally invalidates the lsit.
     *
     * @param car the car to add to the list
     */
    protected void addCarItem(Car car) {
        this.mCars.add(car);
        notifyDataSetChanged();
    }

    /**
     * Removes a {@link Car} from the list and finally invalidates the list.
     *
     * @param car the car to remove from the list.
     */
    protected void removeCarItem(Car car) {
        if (mCars.contains(car)) {
            mCars.remove(car);
            notifyDataSetChanged();
        }
    }

    /**
     * Static view holder class that holds all necessary views of a list-row.
     */
    static class CarViewHolder {

        protected final View mCoreView;

        @InjectView(R.id.activity_car_selection_layout_carlist_entry_icon)
        protected ImageView mIconView;
        @InjectView(R.id.activity_car_selection_layout_carlist_entry_firstline)
        protected TextView mFirstLineText;
        @InjectView(R.id.activity_car_selection_layout_carlist_entry_secondline)
        protected TextView mSecondLineText;

        @InjectView(R.id.activity_car_selection_layout_carlist_entry_engine)
        protected TextView mEngineText;
        @InjectView(R.id.activity_car_selection_layout_carlist_entry_gasoline)
        protected TextView mGasolineText;
        @InjectView(R.id.activity_car_selection_layout_carlist_entry_year)
        protected TextView mYearText;

        /**
         * Constructor.
         *
         * @param view
         */
        CarViewHolder(View view) {
            this.mCoreView = view;
            ButterKnife.inject(this, view);
        }
    }
}