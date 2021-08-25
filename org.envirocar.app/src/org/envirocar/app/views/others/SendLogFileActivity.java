/**
 * Copyright (C) 2013 - 2019 the enviroCar community
 * <p>
 * This file is part of the enviroCar app.
 * <p>
 * The enviroCar app is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * The enviroCar app is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along
 * with the enviroCar app. If not, see http://www.gnu.org/licenses/.
 */
package org.envirocar.app.views.others;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.afollestad.materialdialogs.MaterialDialog;

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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindArray;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

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

    @BindView(R.id.envirocar_toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.report_issue_time_since_crash)
    protected EditText whenField;
    @BindView(R.id.report_issue_desc)
    protected EditText comments;
    @BindView(R.id.report_issue_submit)
    protected View submitIssue;
    @BindView(R.id.report_issue_checkbox_list)
    protected ListView checkBoxListView;

    @BindArray(R.array.report_issue_subject_header)
    protected String[] subjectHeaders;
    @BindArray(R.array.report_issue_subject_tags)
    protected String[] subjectTags;

    @Inject
    protected CarPreferenceHandler mCarPrefHandler;
    @Inject
    protected BluetoothHandler mBluetoothHandler;

    protected List<CheckBoxItem> checkBoxItems;
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
        getSupportActionBar().setTitle("");

        setCheckboxes();

        try {
            removeOldReportBundles();
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    @OnClick(R.id.report_issue_submit)
    public void onClickSubmitButton(View v) {
        try {
            final File tmpBundle = createReportBundle();
            if (checkIfCheckboxSelected()) {
                sendLogFile(tmpBundle);
            } else {
                createNoCheckboxDialog(tmpBundle);
            }
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    /**
     * function to setCheckboxes the names for the checkboxes
     */
    public void setCheckBoxes() {
        for (String subjectHeader : subjectHeaders) {
            CheckBoxItem temp = new CheckBoxItem();
            temp.setChecked(false);
            temp.setItemText(subjectHeader);
            checkBoxItems.add(temp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) onBackPressed();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();
        hideKeyboard(getCurrentFocus());
    }


    /**
     * In case no checkbox has been ticked, a dialog is created urging the getUserStatistic to do so,
     * else continue
     *
     * @param reportBundle
     */
    public void createNoCheckboxDialog(File reportBundle) {
        new MaterialDialog.Builder(this)
                .title(R.string.report_issue_no_checkbox_selected_title)
                .content(R.string.report_issue_no_checkbox_selected_content)
                .positiveText(R.string.report_issue_no_checkbox_send_anyway)
                .negativeText(R.string.cancel)
                .cancelable(false)
                .onPositive((materialDialog, dialogAction) -> sendLogFile(reportBundle))
                .onNegative((materialDialog, dialogAction) -> LOG.info("Log report not send"))
                .show();
    }

    /**
     * creates a new {@link Intent#ACTION_SEND} with the report
     * bundle attached.
     *
     * @param reportBundle the file to attach
     */
    protected void sendLogFile(File reportBundle) {
        Intent emailIntent = new Intent(android.content.Intent.ACTION_SENDTO);
        //emailIntent.setType("message/rfc822");
        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[]{REPORTING_EMAIL});
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                createSubject() + " enviroCar Log Report");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                createEmailContents());
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,
                Uri.fromFile(reportBundle));
        //emailIntent.setType("application/zip");

        startActivity(Intent.createChooser(emailIntent, "Send Log Report"));
        getFragmentManager().popBackStack();
    }

    /**
     * Gets all the appropriate version names, for the app, the Android version and API
     * Manufacturer and Model name of the phone
     *
     * @return the string containing all the above
     */
    protected String getVersionNames() {
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
        return stringBuilder.toString();

    }

    /**
     * gets the current car and bluetooth adapter name
     *
     * @return the string with the data
     */
    protected String getCarBluetoothNames() {
        StringBuilder stringBuilder = new StringBuilder();
        Car car = mCarPrefHandler.getCar();
        stringBuilder.append("Car Details: ");
        if (car != null)
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
     *
     * @return returns the string with the tags to be added to the subject line
     */
    protected String createSubject() {
        StringBuilder subject = new StringBuilder();
        for (int i = 0; i < subjectTags.length; i++) {
            CheckBoxItem dto = checkBoxItems.get(i);
            if (dto.isChecked()) {
                subject.append(subjectTags[i]);
            }
        }
        return subject.toString();
    }

    /**
     * read the getUserStatistic defined edit fields.
     *
     * @return a string acting as the contents of the email
     */
    private String createEmailContents() {
        StringBuilder sb = new StringBuilder();
        sb.append("A new Issue Report has been created:");
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
        stringBuilder.append("Description: ");
        stringBuilder.append(comments.getText().toString());
        stringBuilder.append("\n");
        return stringBuilder.toString();
    }

    private String createEstimatedTimeStamp() {

        String text;
        try {
            text = whenField.getText().toString();
        } catch (Exception e) {
            e.printStackTrace();
            text = null;
        }

        String[] hoursAndMinutes = text.split(":");

        Calendar calendar = Calendar.getInstance();

        if(hoursAndMinutes.length > 1){
            try {
                calendar.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hoursAndMinutes[0]));
            } catch (Exception e) {
                LOG.info("Could not parse hour of day.");
            }

            try {
                calendar.set(Calendar.MINUTE, Integer.parseInt(hoursAndMinutes[1]));
            } catch (Exception e) {
                LOG.info("Could not parse minute.");
            }
        }
        return SimpleDateFormat.getDateTimeInstance().format(calendar.getTime());
    }


    public File createVersionAndErrorDetailsFile() throws IOException {
        File otherFile = Util.createFileOnInternalStorage(getCacheDir().getAbsolutePath(), OTHER_DETAILS_PREFIX + ".txt");
        StringBuilder text = new StringBuilder();
        text.append(createSubject());
        text.append("\n");
        text.append(createEmailContents());
        Util.saveContentsToFile(text.toString(), otherFile);

        return otherFile;
    }

    /**
     * creates a report bundle, containing all available log files
     *
     * @return the report bundle
     * @throws IOException
     */
    private File createReportBundle() throws IOException {
        File targetFile = Util.createFileOnInternalStorage(getExternalCacheDir().getAbsolutePath(),
                PREFIX + format.format(new Date()) + EXTENSION);

        Util.zip(findAllLogFiles(), targetFile.toURI().getPath());

        return targetFile;
    }

    private void removeOldReportBundles() throws IOException {
        File baseFolder = Util.resolveInternalStorageBaseFolder(getCacheDir().getAbsolutePath());

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

        if (oldFiles != null) {
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
        if (view != null) {
            InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * used to make sure the list View is not cut off in the parent ScrollView
     *
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
     * @return true if at least one checkbox is ticked
     */
    private boolean checkIfCheckboxSelected() {
        for (int i = 0; i < checkBoxItems.size(); i++) {
            CheckBoxItem dto = checkBoxItems.get(i);
            if (dto.isChecked()) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }


    /**
     * Gets the strings for the checkboxes and sets the adapter
     */
    private void setCheckboxes() {
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
