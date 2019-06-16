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
package org.envirocar.app.views;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.app.views.reportissue.CheckBoxItem;
import org.envirocar.app.views.reportissue.CheckboxBaseAdapter;
import org.envirocar.core.logging.LocalFileHandler;
import org.envirocar.core.logging.Logger;
import org.envirocar.core.util.Util;

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

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An activity for reporting issues.
 *
 * @author matthes rieke
 */
public class SendLogFileActivity extends AppCompatActivity {

    private static final Logger LOG = Logger
            .getLogger(SendLogFileActivity.class);
    private static final String REPORTING_EMAIL = "envirocar@52north.org";
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.getDefault());
    private static final DateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final String PREFIX = "report-";
    private static final String EXTENSION = ".zip";

    @BindView(R.id.report_issue_header)
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

    protected List<CheckBoxItem> checkBoxItems;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_log_layout_new);
        ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Report an Issue");

        checkBoxItems = new ArrayList<>();
        setCheckBoxes();
        CheckboxBaseAdapter checkboxBaseAdapter = new CheckboxBaseAdapter(getApplicationContext(), checkBoxItems);
        checkboxBaseAdapter.notifyDataSetChanged();
        checkBoxListView.setAdapter(checkboxBaseAdapter);
        checkBoxListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int itemIndex, long l) {

                Object itemObject = adapterView.getAdapter().getItem(itemIndex);

                CheckBoxItem item = (CheckBoxItem) itemObject;

                CheckBox itemCheckbox = view.findViewById(R.id.report_issue_checkbox_item);

                if(item.isChecked())
                {
                    itemCheckbox.setChecked(false);
                    item.setChecked(false);
                }else
                {
                    itemCheckbox.setChecked(true);
                    item.setChecked(true);
                }
            }
        });

        File reportBundle = null;
        try {
            removeOldReportBundles();

            final File tmpBundle = createReportBundle();
            reportBundle = tmpBundle;
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
            submitIssue.setOnClickListener(
                    view -> sendLogFile(tmpBundle));
        } catch (IOException e) {
            LOG.warn(e.getMessage(), e);
        }

        if (reportBundle != null) {
            locationText.setText(reportBundle.getAbsolutePath());
        } else {
            locationText.setError("Error allocating report bundle.");
            locationText.setText("An error occured while creating the report bundle. Please send in the logs available at " +
                    LocalFileHandler.effectiveFile.getParentFile().getAbsolutePath());
        }

    }

    public void setCheckBoxes(){
        List<String> items = Arrays.asList(getResources().getStringArray(R.array.checkbox_text_items));
        for (int i = 0; i < items.size(); i++) {
            CheckBoxItem temp = new CheckBoxItem();
            temp.setChecked(false);
            temp.setItemText(items.get(i));
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

    public boolean checkIfCheckboxSelected(){

        for(int i=0;i<checkBoxItems.size();i++)
        {
            CheckBoxItem dto = checkBoxItems.get(i);
            if(dto.isChecked())
            {
                return Boolean.TRUE;
            }
        }
        LOG.info("No checkboxes ticked.");
        return Boolean.FALSE;
    }

    public void createDialog(File reportBundle){
        AlertDialog.Builder builder = new AlertDialog.Builder(getApplicationContext());
        builder.setMessage("You have not selected any of the checkboxes. These help developers " +
                "sort through issues quickly and resolve them. Please consider filling those that " +
                "are relevant.")
                .setTitle("No Checkbox Selected")
                .setCancelable(false)
                .setPositiveButton("Go Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Send Report Anyway", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendLogFile(reportBundle);
                    }
                });
        AlertDialog alertDialog = builder.create();
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
                "enviroCar Log Report");
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                createEmailContents());
        emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,
                Uri.fromFile(reportBundle));
        //emailIntent.setType("application/zip");

        startActivity(Intent.createChooser(emailIntent, "Send Log Report"));
        getFragmentManager().popBackStack();
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
        sb.append("Estimated system time of occurrence: ");
        sb.append(createEstimatedTimeStamp());
        sb.append(Util.NEW_LINE_CHAR);
        sb.append(Util.NEW_LINE_CHAR);
        sb.append("Additional comments:");
        sb.append(Util.NEW_LINE_CHAR);
        sb.append(createAdditionalComments());
        return sb.toString();
    }

    private String createAdditionalComments() {
        return this.comments.getText().toString();
    }

    private String createEstimatedTimeStamp() {
        long now = System.currentTimeMillis();
        String text = this.whenField.getText().toString();

        int delta;
        if (text == null || text.isEmpty()) {
            delta = 0;
        } else {
            delta = Integer.parseInt(text);
        }

        Date date = new Date(now - delta * 1000 * 60);
        return SimpleDateFormat.getDateTimeInstance().format(date);
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

}
