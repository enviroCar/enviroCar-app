package org.envirocar.app.view.logbook;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.core.entity.Car;

import java.util.List;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * TODO JavaDoc
 *
 * @author dewall
 */
public class LogbookCarSpinnerAdapter extends ArrayAdapter<Car> {

    private final List<Car> cars;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     * @param objects
     */
    public LogbookCarSpinnerAdapter(Context context, List<Car> objects) {
        super(context, R.layout.activity_logbook_car_spinner_entry, R.id
                .activity_logbook_car_spinner_entry_firstline, objects);
        this.cars = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final Car car = cars.get(position);

        CarSpinnerEntryHolder holder = null;
        if (convertView == null) {
            // Then inflate a new view for the car and create a holder
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context
                    .LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.activity_logbook_car_spinner_entry,
                    parent, false);
            holder = new CarSpinnerEntryHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (CarSpinnerEntryHolder) convertView.getTag();
        }

        holder.title.setText(car.getManufacturer() + " " + car.getModel());
        holder.secondLine.setText(car.getConstructionYear() +
                "     " + car.getFuelType() +
                "     " + car.getEngineDisplacement() + " ccm");

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    static class CarSpinnerEntryHolder {

        @InjectView(R.id.activity_logbook_car_spinner_entry_firstline)
        protected TextView title;
        @InjectView(R.id.activity_logbook_car_spinner_entry_secondline)
        protected TextView secondLine;


        /**
         * Constructor.
         *
         * @param view the parent view of an entry in the car spinner.
         */
        CarSpinnerEntryHolder(View view) {
            ButterKnife.inject(this, view);
        }
    }
}
