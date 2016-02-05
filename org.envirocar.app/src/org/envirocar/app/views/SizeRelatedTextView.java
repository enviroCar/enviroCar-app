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
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.TextView;

public class SizeRelatedTextView extends TextView implements OnParentDrawnListener {

	private static final String NAMESPACE = "http://envirocar.org";
	private static final String RELATED_VIEW_KEY = "sizeRelatedPredecessingView";
	private static final String TARGET_TEXT_STRING = "targetTextString";
	
	private int relatedLayoutId = Integer.MIN_VALUE;
	private String targetTextString;
	private AtomicBoolean firstRun = new AtomicBoolean(true);
	
	public SizeRelatedTextView(Context context) {
		super(context);
	}
	
	public SizeRelatedTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		parseAttributes(attrs);
	}
	
	public SizeRelatedTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		parseAttributes(attrs);
	}

	private void parseAttributes(AttributeSet attrs) {
		relatedLayoutId = attrs.getAttributeResourceValue(NAMESPACE, RELATED_VIEW_KEY, Integer.MIN_VALUE);
		targetTextString = attrs.getAttributeValue(NAMESPACE, TARGET_TEXT_STRING);
		
		if (targetTextString == null || targetTextString.isEmpty()) {
			targetTextString = "0123456789";
		}
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		applyTextSize();
	}
	

	private void applyTextSize() {
		if (relatedLayoutId == Integer.MIN_VALUE || !firstRun.getAndSet(false))
			return;
		
		try {
			int relatedWidth = resolveRelatedWidth();
			float resolvedSize = computeSize(relatedWidth);
			setTextSize(resolvedSize);
		} catch (IllegalStateException e) {
		}
		
	}

	private int resolveRelatedWidth() {
		int depth = 10;
		
		ViewParent tmp = getParent();
		View resultView = null;
		while (tmp != null && resultView == null && depth-- > 0) {
			if (tmp instanceof View) {
				resultView = ((View) tmp).findViewById(relatedLayoutId);
			}
			
			tmp = tmp.getParent();
		}
		
		if (resultView == null) {
			throw new IllegalStateException("Could not resolve related view.");
		}
		
		if (resultView instanceof ImageView) {
			ImageView iv = (ImageView) resultView;
			if (iv.getLayoutParams() != null && iv.getLayoutParams().width != 0) {
				return iv.getLayoutParams().width;
			}
			return iv.getMeasuredWidth();
		}
		return resultView.getWidth();
	}

	private float computeSize(int relatedWidth) {
		if (relatedWidth <= 0)
			return getTextSize();
		
		TextPaint tp = new TextPaint(getPaint());
		float textSize = getTextSize();
		float calculatedWidth = 0.0f;
		
		float densityMultiplier = getContext().getResources().getDisplayMetrics().density;
		
		while (calculatedWidth < relatedWidth) {
			tp.setTextSize((textSize+=4) * densityMultiplier);
			calculatedWidth = tp.measureText(targetTextString);
		}
		
		return textSize;
	}

	@Override
	public void onParentDrawn(View parentView) {
		applyTextSize();
	}

}
