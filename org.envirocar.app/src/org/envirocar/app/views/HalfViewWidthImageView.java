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
package org.envirocar.app.views;

import java.util.concurrent.atomic.AtomicBoolean;


import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class HalfViewWidthImageView extends ImageView implements OnParentDrawnListener {

	private static final String NAMESPACE = "http://envirocar.org";
	private static final String RELATED_VIEW_KEY = "widthRelatedPredecessingView";
	
	private AtomicBoolean firstRun = new AtomicBoolean(true);
	private int relatedLayoutId = Integer.MIN_VALUE;

	public HalfViewWidthImageView(Context context) {
		super(context);
	}
	
	public HalfViewWidthImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		parseAttributes(attrs);
	}
	
	public HalfViewWidthImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		parseAttributes(attrs);
	}

	private void parseAttributes(AttributeSet attrs) {
		relatedLayoutId = attrs.getAttributeResourceValue(NAMESPACE, RELATED_VIEW_KEY, Integer.MIN_VALUE);
	}
	
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
	}

	private void applyWidth() {
		if (!firstRun.getAndSet(false)) {
			return;
		}
		
		int newWidth;
		try {
			newWidth = resolveRootViewWidth() / 2;
		} catch (IllegalStateException e) {
			return;
		}
		
		int imgHeight = getDrawable().getIntrinsicHeight();
		int imgWidth = getDrawable().getIntrinsicWidth();
		
		int newHeight = imgHeight * newWidth / imgWidth;

		Log.i("", newWidth+" newWidth");
		
		//Use RelativeLayout.LayoutParams if your parent is a RelativeLayout
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
		    newWidth, newHeight);
		this.setLayoutParams(params);
		this.setScaleType(ImageView.ScaleType.CENTER_CROP);		
	}

	private int resolveRootViewWidth() {
		if (relatedLayoutId == Integer.MIN_VALUE) {
			throw new IllegalStateException("Related root layout not defined.");
		}
		
		int depth = 10;
		
		ViewParent tmp = getParent();
		
		View result = null;
		while (result == null && depth-- > 0 && tmp != null) {
			if (tmp instanceof View && ((View) tmp).getId() == relatedLayoutId) {
				result = (View) tmp;
			}
			
			tmp = tmp.getParent();
		}
		
		if (result == null) {
			throw new IllegalStateException("Related root layout not resolvable.");
		}
		
		return result.getWidth();
	}

	@Override
	public void onParentDrawn(View parentView) {
		applyWidth();
	}
	
}
