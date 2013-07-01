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

package org.envirocar.app.views;

import org.envirocar.app.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;



public class CheckListItem extends LinearLayout {
	
	public static final int STATUS_ERROR = 0;
	public static final int STATUS_CLEAR = 1;
	public static final int STATUS_PROBLEM = 2;
	public static final int STATUS_PENDING = 3;

	private String strText = "";
	
	private TextView text;
	private ImageView img;
	private ProgressBar progress;
	private LayoutParams imgLayoutParams;
	private LayoutParams textLayoutParams;
	private ImageView imgActionRight;

	public CheckListItem(Context context) {
		super(context);
		init(context);
	}
	
	public CheckListItem(Context context, String itemText) {
		super(context);
		setText(itemText);
		init(context);
	}

	public CheckListItem(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CheckList);
		strText = a.getString(R.styleable.CheckList_itemText);
		a.recycle();
		init(context);
	}
	
	public void setText(String str){
		strText = str;
		text.setText(str);
	}
	
	public void setText(int id){
		strText = getResources().getString(id);
		text.setText(id);
	}
	
	
	public void setState(int state){
		switch(state){
			case STATUS_ERROR:
				progress.setVisibility(View.GONE);
				img.setVisibility(View.VISIBLE);				
				img.setImageResource(R.drawable.cross);
				imgActionRight.setVisibility(View.VISIBLE);
				break;
			case STATUS_CLEAR:
				progress.setVisibility(View.GONE);
				img.setVisibility(View.VISIBLE);				
				img.setImageResource(R.drawable.checkmark);
				imgActionRight.setVisibility(View.INVISIBLE);
				break;
			case STATUS_PROBLEM:
				progress.setVisibility(View.GONE);
				img.setVisibility(View.VISIBLE);
				img.setImageResource(R.drawable.problem);
				imgActionRight.setVisibility(View.VISIBLE);
				break;
			case STATUS_PENDING:
				img.setVisibility(View.GONE);
				progress.setVisibility(View.VISIBLE);
				break;
		}
	}
	

	private void init(Context context) {
		imgLayoutParams = new LayoutParams(getDP(R.dimen.checklist_textheight),getDP(R.dimen.checklist_textheight));
		textLayoutParams = new LayoutParams(getDP(R.dimen.checklist_textwidth),getDP(R.dimen.checklist_textheight));
		
		
		//init the internal views
		text = new TextView(context);
		text.setLayoutParams(textLayoutParams);
		text.setTextSize(TypedValue.COMPLEX_UNIT_PX,getResources().getDimensionPixelSize(R.dimen.checklist_textsize));
		text.setText(strText);
		text.setGravity(Gravity.CENTER_VERTICAL);
		text.setFocusable(false);

		
		
		img = new ImageView(context);
		img.setLayoutParams(imgLayoutParams);
		img.setImageResource(R.drawable.cross);
		img.setVisibility(View.GONE);
		img.setFocusable(false);

		
		progress = new ProgressBar(context);
		progress.setLayoutParams(imgLayoutParams);
		progress.setFocusable(false);

		
		imgActionRight = initImgActionRight(context);
		imgActionRight.setFocusable(false);

		
		this.addView(text);
		this.addView(img);
		this.addView(progress);
		this.addView(imgActionRight);
	}
	
	
	private int getDP(int res){
		return (int) getResources().getDimension(res);
		}

	private ImageView initImgActionRight(Context context){
		ImageView v = new ImageView(context);
		LayoutParams l = new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, getDP(R.dimen.checklist_textheight));
		v.setLayoutParams(l);
		v.setImageResource(R.drawable.action_right);
		return v;
	}

	


}
