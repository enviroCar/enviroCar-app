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

public class SortDialog extends Dialog implements AdapterView.OnItemSelectedListener{
    private static final Logger LOG = Logger.getLogger(SortDialog.class);

    @BindView(R.id.radioGroup)
    protected RadioGroup radioGroup;
    @BindView(R.id.spinnerSort)
    protected Spinner spinnerSort;
    @BindView(R.id.asc)
    protected RadioButton asc;
    @BindView(R.id.desc)
    protected RadioButton desc;
    @BindView(R.id.submit)
    protected Button submit;
    private SortViewModel sortViewModel;
    private Integer choice;
    private Boolean order;

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
        if(order)
            radioGroup.check(R.id.asc);
        else
            radioGroup.check(R.id.desc);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                if(i == R.id.asc)
                    order = true;
                else
                    order = false;
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sortViewModel.setSortOrder(order);
                sortViewModel.setSortChoice(choice);
                if(sortViewModel.getSortActive().getValue() == null)
                    sortViewModel.setSortActive(true);
                else
                    sortViewModel.setSortActive(!sortViewModel.getSortActive().getValue());

                dismiss();
            }
        });
    }

    public void setSpinner()
    {
        List<String> list = Arrays.asList(activity.getResources().getStringArray(R.array.sort_options));
        ArrayAdapter<String> adapter = new ArrayAdapter(activity,R.layout.spinner_item, list);
        spinnerSort.setAdapter(adapter);
        spinnerSort.setOnItemSelectedListener(this);
    }

    void checkViewModelStatus(){

        choice = sortViewModel.getSortChoice().getValue();
        order = sortViewModel.getSortOrder().getValue();
        if(choice == null)
            choice = 0;
        if(order == null)
            order = true;

    }
    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id) {
        LOG.info("Item "+position+" clicked.");
        choice = position;
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0) {
    }
}
