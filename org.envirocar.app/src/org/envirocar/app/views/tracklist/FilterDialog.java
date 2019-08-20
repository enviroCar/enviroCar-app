package org.envirocar.app.views.tracklist;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import org.envirocar.app.R;
import org.envirocar.app.handler.PreferenceConstants;
import org.envirocar.app.handler.PreferencesHandler;
import org.envirocar.core.entity.Car;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.utils.CarUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FilterDialog extends Dialog implements AdapterView.OnItemSelectedListener {

    private static final Logger LOG = Logger.getLogger(FilterDialog.class);

    @BindView(R.id.spinnerCar)
    protected Spinner spinnerCar;
    @BindView(R.id.dateLayout)
    protected LinearLayout dateLayout;
    @BindView(R.id.before)
    protected Button end;
    @BindView(R.id.after)
    protected Button start;
    @BindView(R.id.submit)
    protected Button submit;
    @BindView(R.id.checkBoxCar)
    protected CheckBox checkBoxCar;
    @BindView(R.id.checkBoxDate)
    protected CheckBox checkBoxDate;
    @BindView(R.id.filterDialog)
    protected ConstraintLayout filterDialog;

    private Context context;
    private FragmentActivity activity;

    // For the date picker dialog
    private int mYear;
    private int mMonth;
    private int mDay;

    // Variables for the date filter
    private Date startDate = null;
    protected Date endDate = null;
    private boolean datesSet = false;

    // Variables for the car filter
    protected String carName = null;
    private boolean carSet = false;
    ArrayList<String> carNames = new ArrayList<>();

    // Have the required values for the active filters been set
    private boolean error;


    private FilterViewModel filterViewModel;

    public FilterDialog(@NonNull Context context, @NonNull FragmentActivity activity) {
        super(context);
        this.context = context;
        this.activity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_tracklist_filter_dialog);
        ButterKnife.bind(this);

        filterViewModel = ViewModelProviders.of(activity).get(FilterViewModel.class);
        Calendar c = Calendar.getInstance();
        mYear = c.get(Calendar.YEAR);
        mMonth = c.get(Calendar.MONTH);
        mDay = c.get(Calendar.DAY_OF_MONTH);

        setSpinner();
        checkViewModelStatus();

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                error = true;

                if (!checkBoxDate.isChecked() || !checkBoxCar.isChecked()) {
                    error = false;
                    if (!checkBoxDate.isChecked())
                        filterViewModel.setFilterDate(false);
                    if (!checkBoxCar.isChecked())
                        filterViewModel.setFilterCar(false);
                }

                if (checkBoxDate.isChecked() && startDate!=null && endDate!=null && startDate.before(endDate)) {
                    filterViewModel.setFilterDates(startDate, endDate);
                    filterViewModel.setFilterDate(true);
                    error = false;
                }
                if (checkBoxCar.isChecked() && carName!=null) {
                    filterViewModel.setFilterCarName(carName);
                    filterViewModel.setFilterCar(true);
                    error = false;
                }
                if (!error) {
                    if (filterViewModel.getFilterActive().getValue() == null)
                        filterViewModel.setFilterActive(true);
                    else
                        filterViewModel.setFilterActive(!filterViewModel.getFilterActive().getValue());
                    dismiss();
                }
            }
        });

        // if checked, show the date filtering options
        checkBoxDate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton  group, boolean isChecked) {
                if (isChecked) {
                    dateLayout.setVisibility(View.VISIBLE);
                } else {
                    dateLayout.setVisibility(View.GONE);
                }
            }

        });

        // if checked, show the car filtering options
        checkBoxCar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton  group, boolean isChecked) {
                if (isChecked) {
                    spinnerCar.setVisibility(View.VISIBLE);
                } else {
                    spinnerCar.setVisibility(View.GONE);
                }
            }

        });

        // Button to set the start date for the date filter
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DatePickerDialog datePickerDialog =  new DatePickerDialog(context,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                Calendar c = Calendar.getInstance();
                                c.set(Calendar.YEAR,year);
                                c.set(Calendar.MONTH, monthOfYear);
                                c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                Date temp = c.getTime();
                                Date temp2 = Calendar.getInstance().getTime();
                                if (endDate!=null)
                                    temp2 = endDate;
                                if (!temp.after(temp2))
                                    startDate = c.getTime();
                                else
                                    startDate = temp2;
                                setDateHeader(1);
                                end.setEnabled(true);
                            }
                        }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });

        // Button to set the end date for the date filter
        end.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                        new DatePickerDialog.OnDateSetListener() {

                            @Override
                            public void onDateSet(DatePicker view, int year,
                                                  int monthOfYear, int dayOfMonth) {
                                Calendar c = Calendar.getInstance();
                                c.set(Calendar.YEAR,year);
                                c.set(Calendar.MONTH, monthOfYear);
                                c.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                Date temp = c.getTime();
                                Date temp2 = Calendar.getInstance().getTime();
                                if (startDate!=null)
                                    temp2 = startDate;
                                if (!temp.before(temp2))
                                    endDate = c.getTime();
                                else
                                    endDate = temp2;
                                setDateHeader(2);
                                datesSet = true;
                            }
                        }, mYear, mMonth, mDay);
                datePickerDialog.show();
            }
        });
    }

    /**
     * Get the users list of cars to set up the Car Spinner
     */
    public void setSpinner() {
        Set<String> temp  = PreferencesHandler.getSharedPreferences(context)
                .getStringSet(PreferenceConstants.PREFERENCE_TAG_CARS, new HashSet<>());
        List<String> carList = new ArrayList<>();

        if(temp != null)
            carList = new ArrayList<>(temp);

        for(int i = 0; i < carList.size(); ++i){
            Car car = CarUtils.instantiateCar(carList.get(i));
            carNames.add(car.getManufacturer() + " " + car.getModel());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter(activity,R.layout.spinner_item, carNames);
        spinnerCar.setAdapter(adapter);
        spinnerCar.setOnItemSelectedListener(this);
    }

    /**
     * If the filter values already exist, get them and set the respective fields. If they
     * dont exist, set the default values for them
     */
    public void checkViewModelStatus() {
        try {
            datesSet = filterViewModel.getFilterDate().getValue();
            checkBoxDate.setChecked(datesSet);
            if (datesSet) {
                startDate = filterViewModel.getFilterDateStart().getValue();
                endDate = filterViewModel.getFilterDateEnd().getValue();
                setDateHeader(1);
                setDateHeader(2);
                dateLayout.setVisibility(View.VISIBLE);
            } else {
                startDate = null;
                endDate = null;
                setDateHeader(0);
                dateLayout.setVisibility(View.GONE);
            }

        } catch (Exception e){
            filterViewModel.getFilterDate().setValue(false);
            datesSet = false;
            checkBoxDate.setChecked(datesSet);
            startDate = null;
            endDate = null;
            setDateHeader(0);
            dateLayout.setVisibility(View.GONE);
        }

        try {
            carSet = filterViewModel.getFilterCar().getValue();
            checkBoxCar.setChecked(carSet);
            if (carSet) {
                carName = filterViewModel.getFilterCarName().getValue();
                spinnerCar.setVisibility(View.VISIBLE);
                spinnerCar.setSelection(carNames.indexOf(carName));
            } else
                spinnerCar.setVisibility(View.GONE);
        } catch (Exception e){
            checkBoxCar.setChecked(false);
            spinnerCar.setVisibility(View.GONE);
        }
    }

    /**
     * Set the start and end date in the Filter Dialog
     * @param choice if 1: Set Start
     *               else if 2 :  Set End
     *               else : Set both with default strings
     */
    public void setDateHeader(int choice) {
        if (choice == 1) {
            String header = new SimpleDateFormat("dd MMMM", Locale.getDefault()).format(startDate);
            start.setText(header);
        } else if(choice == 2) {
            String header = new SimpleDateFormat("dd MMMM", Locale.getDefault()).format(endDate);
            end.setText(header);
        } else {
            start.setText("Start Date");
            end.setText("End Date");
        }
    }

    /**
     * When the spinner item is selected, change the value of the LiveData so that the
     * observers get notified
     */
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        carName = carNames.get(position);
        filterViewModel.setFilterCarName(carName);
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }
}
