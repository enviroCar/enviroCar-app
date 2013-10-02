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

import org.envirocar.app.R;
import org.envirocar.app.model.TermsOfUseInstance;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;

public class DialogUtil {

	public static void createSingleChoiceItemsDialog(String title, String[] items,
			DialogCallback callback, Activity activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle(title);
		
		builder.setSingleChoiceItems(items, -1, callback);

		builder.setOnCancelListener(callback);
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	public static void createTitleMessageDialog(int titleIde, int messageId,
			DialogCallback callback, Activity activity) {
		createTitleMessageDialog(
				activity.getString(titleIde),
				activity.getString(messageId),
				callback, activity);
	}
	
	public static void createTitleMessageDialog(String title, String message,
			DialogCallback callback, Activity activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle(title);
		builder.setMessage(message);
		
		builder.setPositiveButton(R.string.yes, callback);
		builder.setNegativeButton(R.string.no, callback);
		
		builder.setOnCancelListener(callback);
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	
	public abstract static class DialogCallback implements OnClickListener, OnCancelListener, OnDismissListener {

		@Override
		public void onCancel(DialogInterface dialog) {
			cancelled();
		}
		
		@Override
		public void onDismiss(DialogInterface dialog) {
			
		}

		@Override
		public void onClick(DialogInterface dialog, int which) {
			dialog.dismiss();
			itemSelected(which);
		}
	
		public abstract void itemSelected(int which);

		public abstract void cancelled();
		
	}


	public abstract static class PositiveNegativeCallback extends DialogCallback {
		
		@Override
		public void itemSelected(int which) {
			if (which == DialogInterface.BUTTON_POSITIVE) {
				positive();
			}
			else if (which == DialogInterface.BUTTON_NEGATIVE) {
				cancelled();
			}
			else if (which == DialogInterface.BUTTON_NEUTRAL) {
				cancelled();
			}
		}
		
		@Override
		public void cancelled() {
			negative();
		}

		public abstract void negative();
		
		public abstract void positive();
	}


	public static void createTermsOfUseDialog(TermsOfUseInstance current,
			boolean firstTime, DialogCallback callback,
			Activity activity) {
		createTitleMessageDialog(activity.getResources().getString(R.string.terms_of_use_title),
				createTermsOfUseMarkup(current, firstTime, activity), callback, activity);
	}

	private static String createTermsOfUseMarkup(TermsOfUseInstance current,
			boolean firstTime, Activity activity) {
		StringBuilder sb = new StringBuilder();
		
		String linesep = System.getProperty("line.separator");
		if (!firstTime) {
			sb.append(activity.getString(R.string.terms_of_use_sorry));
		}
		else {
			sb.append(activity.getString(R.string.terms_of_use_info));
		}
		sb.append(linesep);
		sb.append(linesep);
		
		sb.append(current.getContents());
		
		return sb.toString();
	}
	
	
}
