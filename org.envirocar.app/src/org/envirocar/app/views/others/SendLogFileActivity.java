/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 *
 * This file is part of the enviroCar app.
 *
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.others;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.preference.PreferenceManager;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.envirocar.app.BaseApplicationComponent;
import org.envirocar.app.R;
import org.envirocar.app.handler.ApplicationSettings;
import org.envirocar.app.handler.BluetoothHandler;
import org.envirocar.app.handler.preferences.CarPreferenceHandler;
import org.envirocar.app.injection.BaseInjectorActivity;
import org.envirocar.app.views.reportissue.CheckBoxItem;
import org.envirocar.app.views.reportissue.CheckboxBaseAdapter;
import org.envirocar.core.entity.Car;
import org.envirocar.core.logging.LocalFileHandler;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;
import org.envirocar.core.utils.CarUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity for reporting issues.
 *
 * @author matthes rieke
 */
public class SendLogFileActivity extends BaseInjectorActivity {

    private static final Logger LOG = Logger.getLogger(SendLogFileActivity.class);
    private static final String REPORTING_EMAIL = "envirocar@52north.org";
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.getDefault());
    private static final DateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final String PREFIX = "report-";
    private static final String OTHER_DETAILS_PREFIX = "extra-info";
    private static final String EXTENSION = ".zip";

    @BindView(R.id.report_issue_header)
    protected EditText title;
    @BindView(R.id.report_issue_time_since_crash)
    protected EditText whenField;
    @BindView(R.id.report_issue_desc)
    protected EditText comments;
    @BindView(R.id.report_issue_log_location)
    protected TextView locationText;
    @BindView(R.id.toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.report_issue_submit)
    protected Button submitIssue;
    @BindView(R.id.report_issue_checkbox_list)
    protected ListView checkBoxListView;
    @BindView(R.id.report_issue_other_file)
    protected TextView otherFileLocation;

    @Inject
    protected CarPreferenceHandler mCarPrefHandler;
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    protected List<CheckBoxItem> checkBoxItems;
    protected List<String> subjectHeaders;
    protected List<String> bodyHeaders;
    protected List<String> subjectTags;
    protected List<String> bodyTags;
    protected String extraInfo;

    @Override
    protected void injectDependencies(BaseApplicationComponent baseApplicationComponent) {
        baseApplicationComponent.inject(this);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_log_layout_new);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Report an Issue");

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                subjectHeaders = Arrays.asList(getResources().getStringArray(R.array.report_issue_subject_header));
                bodyHeaders = Arrays.asList(getResources().getStringArray(R.array.report_issue_body_header));
                subjectTags = Arrays.asList(getResources().getStringArray(R.array.report_issue_subject_tags));
                bodyTags = Arrays.asList(getResources().getStringArray(R.array.report_issue_body_tags));
                set();
            }
        });

        File reportBundle = null;
        try {
            removeOldReportBundles();

            final File tmpBundle = createReportBundle();
            final File otherFile = createVersionAndErrorDetailsFile();
            reportBundle = tmpBundle;
            if (reportBundle != null) {
                LOG.info("Report Location: " + reportBundle.getAbsolutePath());
                locationText.setText(reportBundle.getAbsolutePath());
            } else {
                LOG.info("Error: Report is NULL.");
                locationText.setError("Error allocating report bundle.");
                locationText.setText("An error occured while creating the report bundle. Please send in the logs available at " +
                        LocalFileHandler.effectiveFile.getParentFile().getAbsolutePath());
            }
            if(otherFile!=null){
                otherFileLocation.setText(otherFile.getAbsolutePath());
            } else {
                LOG.info("Error creating the versions txt file.");
            }
            submitIssue.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(checkIfCheckboxSelected())
                    {
                        sendLogFile(tmpBundle);
                    }
                    else
                    {
                        createDialog(tmpBundle);
                    }
                }
            });
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
        }



    }

    /**
     * function to set the names for the checkboxes
     */
    public void setCheckBoxes(){
        List<String> totalList = new ArrayList<>();
        totalList.addAll(subjectHeaders);
        totalList.addAll(bodyHeaders);
        for (int i = 0; i < totalList.size(); i++) {
            CheckBoxItem temp = new CheckBoxItem();
            temp.setChecked(false);
            temp.setItemText(totalList.get(i));
            checkBoxItems.add(temp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard(getCurrentFocus());
    }

    /**
     * @return true if at least one checkbox is ticked
     */
    public boolean checkIfCheckboxSelected(){

        LOG.info("Checking checkboxes.");
        for(int i=0;i<checkBoxItems.size();i++)
        {
            CheckBoxItem dto = checkBoxItems.get(i);
            LOG.info("Checkbox " + i + " : " + dto.isChecked());
            if(dto.isChecked())
            {
                LOG.info("Ticked Checkbox found.");
                return Boolean.TRUE;
            }
        }
        LOG.info("No checkboxes ticked.");
        return Boolean.FALSE;
    }

    /**
     * In case no checkbox has been ticked, a dialog is created urging the user to do so,
     * else continue
     * @param reportBundle
     */
    public void createDialog(File reportBundle){
        AlertDialog.Builder builder = new AlertDialog.Builder(SendLogFileActivity.this);
        builder.setMessage("You have not selected any of the checkboxes. These help developers " +
                "sort through issues quickly and resolve them. Please consider filling those that " +
                "are relevant.")
                .setTitle("No Checkbox Selected")
                .setCancelable(false)
                .setPositiveButton("Go Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("Send Report Anyway", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendLogFile(reportBundle);
                    }
                });
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.parseColor("#7DB7DC"));
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#0065A0"));
            }
        });
        alertDialog.show();
    }


    /**
     * creates a new {@link Intent#ACTION_SEND} with the report
     * bundle attached.
     *
     * @param reportBundle the file to attach
     */
    protected void sendLogFile(File reportBundle) {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO);
        emailIntent.setType("message/rfc822");
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[]{REPORTING_EMAIL});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                createSubject()+" enviroCar Log Report");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                createEmailContents());
        emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,
                Uri.fromFile(reportBundle));
        //emailIntent.setType("application/zip");

        startActivity(Intent.createChooser(emailIntent, "Send Log Report"));
        getFragmentManager().popBackStack();
    }

    /**
     * Gets all the appropriate version names, for the app, the Android version and API
     * Manufacturer and Model name of the phone
     * @return the string containing all the above
     */
    protected String getVersionNames(){
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Version Details\n");
        String versionName = Util.getVersionString(getBaseContext());

        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        int version = Build.VERSION.SDK_INT;
        String versionRelease = Build.VERSION.RELEASE;

        stringBuilder.append("enviroCar : ");
        stringBuilder.append(versionName);
        stringBuilder.append("\n Manufacturer: " + manufacturer);
        stringBuilder.append("\n Model: " + model);
        stringBuilder.append("\n Android API Level: " + version);
        stringBuilder.append("\n Version Release: " + versionRelease);
        stringBuilder.append("\n");
        return  stringBuilder.toString();

    }

    /**
     * gets the current car and bluetooth adapter name
     * @return the string with the data
     */
    protected String getCarBluetoothNames(){
        StringBuilder stringBuilder = new StringBuilder();
        Car car = mCarPrefHandler.getCar();
        stringBuilder.append("Car Details: ");
        if(car!=null)
            stringBuilder.append(car.getManufacturer() + " " + car.getModel());
        else
            stringBuilder.append("No Car Selected.");
        Pair<String, String> btNameAddress = ApplicationSettings.getSelectedBluetoothAdapterObservable(this).blockingFirst();
        stringBuilder.append("\nBluetooth Adapter: ");
        stringBuilder.append(btNameAddress.first);
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    /**
     * finds the checkboxes which have been ticked and their tag names
     * @return returns the string with the tags to be added to the subject line
     */
    protected String createSubject(){
        StringBuilder subject = new StringBuilder();
        for(int i=0;i<subjectTags.size();i++)
        {
            CheckBoxItem dto = checkBoxItems.get(i);
            if(dto.isChecked())
            {
                subject.append(subjectTags.get(i));
            }
        }
        return subject.toString();
    }

    /**
     * @return the string containing the body tags
     */
    protected String createBodyTags(){
        StringBuilder bodyT = new StringBuilder();
        bodyT.append("Tags: ");
        for(int i=0;i<bodyTags.size();i++)
        {
            CheckBoxItem dto = checkBoxItems.get(i+subjectTags.size());
            if(dto.isChecked())
            {
                bodyT.append(bodyTags.get(i));
            }
        }
        return bodyT.toString();
    }

    /**
     * read the user defined edit fields.
     *
     * @return a string acting as the contents of the email
     */
    private String createEmailContents() {
        StringBuilder sb = new StringBuilder();
        sb.append("A new Issue Report has been created:");
        sb.append(Util.NEW_LINE_CHAR);
        sb.append(Util.NEW_LINE_CHAR);
        sb.append(createBodyTags());
        sb.append(Util.NEW_LINE_CHAR);
        sb.append(Util.NEW_LINE_CHAR);
        sb.append(getVersionNames());
        sb.append(Util.NEW_LINE_CHAR);
        sb.append(getCarBluetoothNames());
        sb.append(Util.NEW_LINE_CHAR);
        sb.append("Additional comments:");
        sb.append(Util.NEW_LINE_CHAR);
        sb.append(createComments());
        sb.append(Util.NEW_LINE_CHAR);
        sb.append(Util.NEW_LINE_CHAR);
        sb.append("Estimated system time of occurrence: ");
        sb.append(createEstimatedTimeStamp());

        return sb.toString();
    }

    private String createComments() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Summary: ");
        stringBuilder.append(title.getText().toString());
        stringBuilder.append("\n");
        stringBuilder.append(comments.getText().toString());
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    private String createEstimatedTimeStamp() {
        long now = System.currentTimeMillis();
        String text;
        try {
            text = whenField.getText().toString();
        }catch (Exception e){
            e.printStackTrace();
            text = null;
        }

        int delta;
        if (text == null || text.isEmpty()) {
            delta = 0;
        } else {
            delta = Integer.parseInt(text);
        }

        Date date = new Date(now - delta * 1000 * 60);
        return SimpleDateFormat.getDateTimeInstance().format(date);
    }


    public File createVersionAndErrorDetailsFile() throws IOException{
        File otherFile = Util.createFileOnExternalStorage(OTHER_DETAILS_PREFIX
                + ".txt");
        StringBuilder text = new StringBuilder();
        text.append(createSubject());
        text.append("\n");
        text.append(createEmailContents());
        Util.saveContentsToFile(text.toString(),otherFile);

        return otherFile;
    }

    /**
     * creates a report bundle, containing all available log files
     *
     * @return the report bundle
     * @throws IOException
     */
    private File createReportBundle() throws IOException {
        File targetFile = Util.createFileOnExternalStorage(PREFIX
                + format.format(new Date()) + EXTENSION);

        Util.zip(findAllLogFiles(), targetFile.toURI().getPath());

        return targetFile;
    }

    private void removeOldReportBundles() throws IOException {
        File baseFolder = Util.resolveExternalStorageBaseFolder();

        final String todayPrefix = PREFIX.concat(dayFormat.format(new Date()));
        File[] oldFiles = baseFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isDirectory()) return false;

                return pathname.getName().startsWith(PREFIX) &&
                        !pathname.getName().startsWith(todayPrefix) &&
                        pathname.getName().endsWith(EXTENSION);
            }
        });

        if(oldFiles!=null){
            for (File file : oldFiles) {
                file.delete();
            }
        }

    }

    private List<File> findAllLogFiles() {
        File logFile = LocalFileHandler.effectiveFile;
        final String shortName = logFile.getName();

        File[] allFiles = logFile.getParentFile().listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.getName().startsWith(shortName)
                        && !pathname.getName().endsWith("lck");
            }
        });

        return Arrays.asList(allFiles);
    }

    public void hideKeyboard(View view) {
        if(view != null){
            InputMethodManager inputMethodManager =(InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * used to make sure the list View is not cut off in the parent ScrollView
     * @param listView
     */
    public static void setListViewHeightBasedOnChildren(ListView listView) {
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null)
            return;

        int desiredWidth = View.MeasureSpec.makeMeasureSpec(listView.getWidth(), View.MeasureSpec.UNSPECIFIED);
        int totalHeight = 0;
        View view = null;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            view = listAdapter.getView(i, view, listView);
            if (i == 0)
                view.setLayoutParams(new ViewGroup.LayoutParams(desiredWidth, ViewGroup.LayoutParams.WRAP_CONTENT));

            view.measure(desiredWidth, View.MeasureSpec.UNSPECIFIED);
            totalHeight += view.getMeasuredHeight();
        }
        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    /**
     * Gets the strings for the checkboxes and sets the adapter
     */
    public void set(){
        checkBoxItems = new ArrayList<>();
        extraInfo = new String();
        setCheckBoxes();
        CheckboxBaseAdapter checkboxBaseAdapter = new CheckboxBaseAdapter(SendLogFileActivity.this, checkBoxItems);
        checkboxBaseAdapter.notifyDataSetChanged();
        checkBoxListView.setAdapter(checkboxBaseAdapter);
        setListViewHeightBasedOnChildren(checkBoxListView);

        checkBoxListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int itemIndex, long l) {

                CheckBoxItem item = (CheckBoxItem) adapterView.getAdapter().getItem(itemIndex);
                CheckBox itemCheckbox = (CheckBox) view.findViewById(R.id.report_issue_checkbox_item);
                LOG.info("Checkbox " + itemIndex + " is " + itemCheckbox.isChecked());
                LOG.info("Checkbox List at " + itemIndex + " is " + checkBoxItems.get(itemIndex).isChecked());
            }
        });
    }

}
