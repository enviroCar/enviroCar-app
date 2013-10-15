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

import java.util.concurrent.atomic.AtomicBoolean;

import android.content.Context;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.TextView;

public class SizeRelatedTextView extends TextView {

	private static final String NAMESPACE = "http://envirocar.org";
	private static final String RELATED_VIEW_KEY = "sizeRelatedPredecessingView";
	private static final String TARGET_TEXT_STRING = "targetTextString";
	
	private AtomicBoolean firstRun = new AtomicBoolean(true);
	private int relatedLayoutId = Integer.MIN_VALUE;
	private String targetTextString;
	
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
		applyTextSize();
		super.onDraw(canvas);
	}

	private void applyTextSize() {
		if (!firstRun.getAndSet(false)) {
			return;
		}
		
		if (relatedLayoutId == Integer.MIN_VALUE)
			return;
		
		try {
			int relatedWidth = resolveRelatedWidth();
			setTextSize(computeSize(relatedWidth));
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
		
		return resultView.getWidth();
	}

	private float computeSize(int relatedWidth) {
		if (relatedWidth <= 0)
			return getTextSize();
		
		TextPaint tp = new TextPaint(getPaint());
		float textSize = 8f;
		float calculatedWidth = 0.0f;
		while (calculatedWidth < relatedWidth) {
			tp.setTextSize(textSize++);
			calculatedWidth = tp.measureText(targetTextString);
		}
		
		return textSize;
	}

}
