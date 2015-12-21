/**
 * Copyright (C) 2013 - 2015 the enviroCar community
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
package org.envirocar.app.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import org.envirocar.app.R;
import org.envirocar.core.entity.TermsOfUse;

public class DialogUtil {

	public static void createSingleChoiceItemsDialog(String title, String[] items,
			DialogCallback callback, Context activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle(title);
		
		builder.setSingleChoiceItems(items, -1, callback);

		builder.setOnCancelListener(callback);
		
		AlertDialog dialog = builder.create();
		dialog.show();
	}
	
	public static void createTitleMessageDialog(int titleIde, int messageId,
			DialogCallback callback, Context activity) {
		createTitleMessageDialog(
				activity.getString(titleIde),
				new SpannableString(activity.getString(messageId)),
				callback, activity);
	}
	
	public static void createTitleMessageDialog(String title, Spanned message,
			DialogCallback callback, Context activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle(title);
		builder.setMessage(message);
		
		builder.setPositiveButton(R.string.yes, callback);
		builder.setNegativeButton(R.string.no, callback);
		
		builder.setOnCancelListener(callback);
		
		AlertDialog dialog = builder.create();
		
		dialog.show();
	}
	
	public static void createTitleMessageInfoDialog(String title, Spanned message,
			DialogCallback callback, Context activity) {
		AlertDialog.Builder builder = new AlertDialog.Builder(activity);

		builder.setTitle(title);
		builder.setMessage(message);
		
		builder.setPositiveButton("Ok", callback);
		
		AlertDialog dialog = builder.create();
		
		dialog.show();
	}
	
	public static void createTitleMessageInfoDialog(String title, Spanned message,
			boolean doNotShowAgainField,
			DialogCallback callback, Context activity) {
		if (!doNotShowAgainField) {
			createTitleMessageInfoDialog(title, message, callback, activity);
			return;
		}
		
		DoNotShowAgainAlertDialog dialog = new DoNotShowAgainAlertDialog(
				new ContextThemeWrapper(activity, R.style.EnviroCarAlertDialog));
		dialog.setMessage(message);
		dialog.setTitle(title);
		dialog.setCallback(callback);
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


	public static void createTermsOfUseDialog(TermsOfUse current,
			boolean firstTime, DialogCallback callback,
			Context context) {
		createTitleMessageDialog(context.getResources().getString(R.string.terms_of_use_title),
				createTermsOfUseMarkup(current, firstTime, context), callback, context);
	}


	public static Spanned createTermsOfUseMarkup(TermsOfUse current,
			boolean firstTime, Context context) {
		StringBuilder sb = new StringBuilder();
		
		sb.append("<p>");
		if (!firstTime) {
			sb.append(context.getString(R.string.terms_of_use_sorry));
		}
		else {
			sb.append(context.getString(R.string.terms_of_use_info));
		}
		sb.append(":</p>");
		sb.append(current.getContents().replace("</li>", "<br/></li>"));
		
		return Html.fromHtml(sb.toString());
	}
	
	private static class DoNotShowAgainAlertDialog extends AlertDialog {

		private TextView messageView;
		private CheckBox checkboxView;
		private CharSequence message;
		private TextView titleView;
		private CharSequence title;
		private Button okButton;
		private DialogCallback callback;

		protected DoNotShowAgainAlertDialog(Context context) {
			super(context);
		}
		
		public void setCallback(DialogCallback callback) {
			this.callback = callback;
		}

		protected DoNotShowAgainAlertDialog(Context context,
				boolean cancelable, OnCancelListener cancelListener) {
			super(context, cancelable, cancelListener);
		}

		protected DoNotShowAgainAlertDialog(Context context, int theme) {
			super(context, theme);
		}
		
		@Override
		protected void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
			
			View view = View.inflate(getContext(), R.layout.do_not_show_again_dialog, null);
			setContentView(view);
			
			messageView = (TextView) view.findViewById(R.id.do_not_show_again_message);
			if (messageView != null) {
				messageView.setText(message);
			}
			
			titleView = (TextView) view.findViewById(R.id.do_not_show_again_title);
			if (titleView != null) {
				titleView.setText(title);
			}
			
			checkboxView = (CheckBox) view.findViewById(R.id.do_not_show_again_checkbox);
			
			okButton = (Button) view.findViewById(R.id.do_not_show_again_ok);
			okButton.setOnClickListener(new View.OnClickListener() {
				
				@Override
				public void onClick(View v) {
					if (checkboxView.isChecked()) {
						callback.itemSelected(AlertDialog.BUTTON_POSITIVE);
					} else {
						callback.cancelled();
					}
					
					dismiss();
				}
			});
		}
		
		@Override
		public void setMessage(CharSequence message) {
			this.message = message;
		}
		
		public void setTitle(CharSequence titl) {
			this.title = titl;
		}
		
	}
	
}
