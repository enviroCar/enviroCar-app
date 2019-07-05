package org.envirocar.app.views.tracklist;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import org.envirocar.app.R;
import org.envirocar.app.handler.CarPreferenceHandler;
import org.envirocar.core.entity.Car;
import org.envirocar.core.logging.Logger;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FilterDialog extends Dialog {

    private static final Logger LOG = Logger.getLogger(FilterDialog.class);

    @Inject
    protected CarPreferenceHandler carPreferenceHandler;
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

    private Context context;
    private FragmentActivity activity;
    private int mYear;
    private int mMonth;
    private int mDay;
    private Date startDate = null;
    protected Date endDate = null;
    protected String carName = null;
    private boolean error ;
    private boolean datesSet = false;
    private boolean carSet = false;
    private FilterViewModel filterViewModel;

    public FilterDialog(@NonNull Context context, @NonNull FragmentActivity activity) {
        super(context);
        this.context = context;
        this.activity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
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
                if(checkBoxDate.isChecked() && startDate!=null && endDate!=null && startDate.before(endDate)){
                    filterViewModel.setFilterDates(startDate, endDate);
                    filterViewModel.setFilterDate(true);
                    error = false;
                }
                if(checkBoxCar.isChecked() && carName!=null){
                    filterViewModel.setFilterCarName(carName);
                    filterViewModel.setFilterCar(true);
                    error = false;
                }
                if(!error){
                    dismiss();
                }
            }
        });

        checkBoxDate.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton  group, boolean isChecked) {
                if (isChecked){
                    dateLayout.setVisibility(View.VISIBLE);
                }
                else {
                    dateLayout.setVisibility(View.GONE);
                }
            }

        });

        checkBoxCar.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton  group, boolean isChecked) {
                if (isChecked){
                    spinnerCar.setVisibility(View.VISIBLE);
                }
                else {
                    spinnerCar.setVisibility(View.GONE);
                }
            }

        });

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
                                if(endDate!=null)
                                    temp2 = endDate;
                                if(!temp.after(temp2))
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
                                if(startDate!=null)
                                    temp2 = startDate;
                                if(!temp.before(temp2))
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


    public void setSpinner(){
        List<Car> carList = carPreferenceHandler.getDeserialzedCars();
        ArrayList<String> carNames = new ArrayList<>();
        for(int i=0; i<carList.size();++i){
            carNames.add(carList.get(i).getModel());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter(activity,android.R.layout.simple_spinner_item, carNames);
        spinnerCar.setAdapter(adapter);
    }

    public void checkViewModelStatus(){
        try {
            datesSet = filterViewModel.getFilterDate().getValue();
            checkBoxDate.setChecked(datesSet);
            if(datesSet){
                startDate = filterViewModel.getFilterDateAfter().getValue();
                endDate = filterViewModel.getFilterDateBefore().getValue();
                setDateHeader(1);
                setDateHeader(2);
                dateLayout.setVisibility(View.VISIBLE);
            }
            else
            {
                startDate = null;
                endDate = null;
                setDateHeader(0);
                dateLayout.setVisibility(View.GONE);
            }

        }catch (Exception e){
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
            if(carSet){
                carName = filterViewModel.getFilterCarName().getValue();
                spinnerCar.setVisibility(View.VISIBLE);
            }else
                spinnerCar.setVisibility(View.GONE);
        }catch (Exception e){
            checkBoxCar.setChecked(false);
            spinnerCar.setVisibility(View.GONE);
        }
    }

    public void setDateHeader(int choice){
        if(choice == 1)
        {
            String header = new SimpleDateFormat("dd MMMM").format(startDate);
            start.setText(header);
        }
        else if(choice == 2){
            String header = new SimpleDateFormat("dd MMMM").format(endDate);
            end.setText(header);
        }
        else{
            start.setText("Start Date");
            end.setText("End Date");
        }
    }
}
