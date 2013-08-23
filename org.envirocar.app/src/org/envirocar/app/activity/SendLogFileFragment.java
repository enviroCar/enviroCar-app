/* 
 * enviroCar 2013
 * Copyright (C) 2013  
 * Martin Dueren, Jakob Moellers, Gerald Pape, Christopher Stephan
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 * 
 */
package org.envirocar.app.activity;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.envirocar.app.R;
import org.envirocar.app.logging.LocalFileHandler;
import org.envirocar.app.logging.Logger;
import org.envirocar.app.util.Util;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

/**
 * An activity for reporting issues.
 * 
 * @author matthes rieke
 *
 */
public class SendLogFileFragment extends SherlockFragment {

	private static final Logger logger = Logger
			.getLogger(SendLogFileFragment.class);
	private static final String REPORTING_EMAIL = "envirocar@52north.org";
	private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ss", Locale.getDefault());
	private EditText whenField;
	private EditText comments;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.send_log_layout, null);

		File reportBundle = null;
		try {
			final File tmpBundle = createReportBundle();
			reportBundle = tmpBundle;
			view.findViewById(R.id.send_log_button).setOnClickListener(
					new View.OnClickListener() {
						@Override
						public void onClick(View view) {
							sendLogFile(tmpBundle);
						}
					});
		} catch (IOException e) {
			logger.warn(e.getMessage(), e);
		}
		
		TextView locationText = (TextView) view
				.findViewById(R.id.textView_send_log_location);
		
		if (reportBundle != null) {
			locationText.setText(reportBundle.getAbsolutePath());
		}
		else {
			locationText.setError("Error allocating report bundle.");
			locationText.setText("An error occured while creating the report bundle. Please send in the logs available at "+
					LocalFileHandler.effectiveFile.getParentFile().getAbsolutePath());
		}
		
		resolveInputFields(view);

		return view;
	}

	private void resolveInputFields(View view) {
		this.whenField = (EditText) view.findViewById(R.id.send_log_when);
		this.comments = (EditText) view.findViewById(R.id.send_log_comments);
	}

	/**
	 * creates a new {@link Intent#ACTION_SEND} with the report
	 * bundle attached.
	 * 
	 * @param reportBundle the file to attach
	 */
	protected void sendLogFile(File reportBundle) {
		Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
		emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
				new String[] { REPORTING_EMAIL });
		emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
				"enviroCar Log Report");
		emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
				createEmailContents());
		emailIntent.putExtra(android.content.Intent.EXTRA_STREAM,
				Uri.fromFile(reportBundle));
		emailIntent.setType("application/zip");
		
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
		
		Date date = new Date(now - delta*1000*60);
		return SimpleDateFormat.getDateTimeInstance().format(date);
	}

	/**
	 * creates a report bundle, containing all available log files 
	 * 
	 * @return the report bundle
	 * @throws IOException
	 */
	private File createReportBundle() throws IOException {
		File targetFile = Util.createFileOnExternalStorage("report-"
				+ format.format(new Date()) + ".zip");

		Util.zip(findAllLogFiles(), targetFile.toURI().getPath());

		return targetFile;
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

}
