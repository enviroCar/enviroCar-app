package org.envirocar.app.views.tracklist;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import org.envirocar.app.R;
import org.envirocar.core.logging.Logger;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SortDialog extends Dialog implements AdapterView.OnItemSelectedListener {
    private static final Logger LOG = Logger.getLogger(SortDialog.class);

    @BindView(R.id.spinnerSort)
    protected Spinner spinnerSort;
    @BindView(R.id.radioGroup)
    protected RadioGroup radioGroup;
    @BindView(R.id.asc)
    protected RadioButton asc;
    @BindView(R.id.desc)
    protected RadioButton desc;
    @BindView(R.id.submit)
    protected Button submit;
    @BindView(R.id.radioGroup1)
    protected RadioGroup radioGroup1;
    @BindView(R.id.wMap)
    protected RadioButton wMap;
    @BindView(R.id.woMap)
    protected RadioButton woMap;

    private SortViewModel sortViewModel;

    // 1: By Date
    // 2: By Length
    // 3: By Duration
    // 4: By Car
    private Integer sortChoice;

    // True: Ascending
    // False: Descending
    private Boolean sortOrder;

    // True: With Map
    // False: Without Map
    private Boolean mapChoice;

    Context context;
    FragmentActivity activity;

    public SortDialog(@NonNull Context Context, @NonNull FragmentActivity Activity) {
        super(Context);
        context = Context;
        activity = Activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_tracklist_sort_dialog);
        ButterKnife.bind(this);

        sortViewModel = ViewModelProviders.of(activity).get(SortViewModel.class);

        checkViewModelStatus();
        setSpinner();

        if (sortOrder)
            radioGroup.check(R.id.asc);
        else
            radioGroup.check(R.id.desc);

        if (mapChoice)
            radioGroup1.check(R.id.wMap);
        else
            radioGroup1.check(R.id.woMap);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                sortOrder = i == R.id.asc;
            }
        });

        radioGroup1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                mapChoice = i == R.id.wMap;
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortViewModel.setSortOrder(sortOrder);
                sortViewModel.setSortChoice(sortChoice);
                sortViewModel.setMapChoice(mapChoice);
                if (sortViewModel.getSortActive().getValue() == null)
                    sortViewModel.setSortActive(true);
                else
                    sortViewModel.setSortActive(!sortViewModel.getSortActive().getValue());

                if (sortViewModel.getMapActive().getValue() == null)
                    sortViewModel.setMapActive(true);
                else
                    sortViewModel.setMapActive(!sortViewModel.getMapActive().getValue());
                dismiss();
            }
        });
    }

    /**
     * Set the spinner with the sort options
     */
    public void setSpinner() {
        List<String> list = Arrays.asList(activity.getResources().getStringArray(R.array.sort_options));
        ArrayAdapter<String> adapter = new ArrayAdapter(activity,R.layout.spinner_item, list);
        spinnerSort.setAdapter(adapter);
        spinnerSort.setOnItemSelectedListener(this);
        spinnerSort.setSelection(sortChoice);
    }

    /**
     * If the sort values already exist, get them and set the respective fields. If they
     * dont exist, set the default values for them
     */
    void checkViewModelStatus() {
        sortChoice = sortViewModel.getSortChoice().getValue();
        sortOrder = sortViewModel.getSortOrder().getValue();
        mapChoice = sortViewModel.getMapChoice().getValue();

        if (sortChoice == null)
            sortChoice = 0;
        if (sortOrder == null)
            sortOrder = true;
        if (mapChoice == null)
            mapChoice = true;

    }
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        sortChoice = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }
}
