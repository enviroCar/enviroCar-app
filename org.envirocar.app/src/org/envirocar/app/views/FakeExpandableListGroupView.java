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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class FakeExpandableListGroupView extends LinearLayout {

	private static final String NAMESPACE = "http://envirocar.org";
	private static final String LINKED_VIEW_KEY = "linkedView";
	private static final String INDICATOR_KEY = "indicatorImageView";

	private View linkedView;
	private int linkedViewId;
	private int indicatorId;
	protected ImageView indicatorView;

	public FakeExpandableListGroupView(Context context) {
		super(context);

		init();
	}

	private void init() {
		this.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (linkedView == null) {
					if (linkedViewId != Integer.MIN_VALUE) {
						try {
							linkedView = resolveLinkedView(linkedViewId);
						} catch (IllegalStateException e) {
							return;
						}
					}
				}

				if (indicatorView == null) {
					try {
						indicatorView = (ImageView) resolveLinkedView(indicatorId);
					} catch (IllegalStateException e) {
						return;
					}
				}


				changeState(linkedView.getVisibility());
			}
		});
	}

	protected void changeState(int visibility) {
		switch (visibility) {
		case View.GONE:
			linkedView.setVisibility(View.VISIBLE);
			indicatorView.getDrawable().setState(new int[] {android.R.attr.state_expanded});
			break;
		case View.VISIBLE:
			linkedView.setVisibility(View.GONE);
			indicatorView.getDrawable().setState(new int[] {-android.R.attr.state_expanded});
			break;
		case View.INVISIBLE:
			linkedView.setVisibility(View.VISIBLE);
			indicatorView.getDrawable().setState(new int[] {android.R.attr.state_expanded});
			break;
		default:
			break;
		}
		
		createColorAnimation();		
	}

	@SuppressWarnings("deprecation")
	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	protected void createColorAnimation() {
		Drawable target = resolveBackground();

		Drawable[] color = { new ColorDrawable(Color.parseColor("#43a3d4")),
				target };
		TransitionDrawable trans = new TransitionDrawable(color);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			setBackground(trans);
		} else {
			setBackgroundDrawable(trans);
		}
		trans.startTransition(200);
	}

	private Drawable resolveBackground() {
		Drawable bg = getBackground();
		int depth = 10;
		ViewParent tmp = getParent();
		while (bg == null && depth-- > 0) {
			if (tmp != null) {
				if (tmp instanceof View) {
					bg = ((View) tmp).getBackground();
				}
				tmp = tmp.getParent();
			}
		}

		if (bg == null) {
			bg = new ColorDrawable(Color.TRANSPARENT);
		}

		return bg;
	}

	public FakeExpandableListGroupView(Context context, AttributeSet attrs) {
		super(context, attrs);

		parseAttributes(attrs);

		init();
	}

	@SuppressLint("NewApi")
	public FakeExpandableListGroupView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);

		parseAttributes(attrs);

		init();
	}

	private void parseAttributes(AttributeSet attrs) {
		linkedViewId = attrs.getAttributeResourceValue(NAMESPACE,
				LINKED_VIEW_KEY, Integer.MIN_VALUE);
		indicatorId = attrs.getAttributeResourceValue(NAMESPACE, INDICATOR_KEY,
				Integer.MIN_VALUE);
	}

	private View resolveLinkedView(int linkedViewId) {
		int depth = 10;
		ViewParent tmp = getParent();
		View resultView = null;
		while (tmp != null && resultView == null && depth-- > 0) {
			if (tmp instanceof View) {
				resultView = ((View) tmp).findViewById(linkedViewId);
			}

			tmp = tmp.getParent();
		}

		if (resultView == null) {
			throw new IllegalStateException("Could not resolve related view.");
		}

		return resultView;
	}

}
