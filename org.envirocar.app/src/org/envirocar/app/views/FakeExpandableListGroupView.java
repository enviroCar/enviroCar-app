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
	private static final String INITIAL_STATE = "expanded";

	private View linkedView;
	private int linkedViewId;
	private int indicatorId;
	protected ImageView indicatorView;
	private boolean initialState;
	private boolean firstRun = true;

	public FakeExpandableListGroupView(Context context) {
		super(context);

		init();
	}

	private void init() {
		this.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					linkedView = resolveLinkedView(linkedViewId, linkedView);
				} catch (IllegalStateException e) {
					return;
				}

				try {
					indicatorView = (ImageView) resolveLinkedView(indicatorId, indicatorView);
				} catch (IllegalStateException e) {
					return;
				}

				changeState(linkedView.getVisibility());
			}
		});
	}

	protected void changeState(int visibility) {
		switch (visibility) {
		case View.GONE:
			linkedView.setVisibility(View.VISIBLE);
			indicatorView.setImageState(new int[] {android.R.attr.state_expanded}, false);
			break;
		case View.VISIBLE:
			linkedView.setVisibility(View.GONE);
			indicatorView.setImageState(new int[] {}, false);
			break;
		case View.INVISIBLE:
			linkedView.setVisibility(View.VISIBLE);
			indicatorView.setImageState(new int[] {android.R.attr.state_expanded}, false);
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
		initialState = attrs.getAttributeBooleanValue(NAMESPACE, INITIAL_STATE, false);
	}
	
	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		
		synchronized (this) {
			if (firstRun) {
				try {
					linkedView = resolveLinkedView(linkedViewId, linkedView);
					indicatorView = (ImageView) resolveLinkedView(indicatorId, indicatorView);
					
					if (initialState) {
						linkedView.setVisibility(View.VISIBLE);
						indicatorView.setImageState(new int[] {android.R.attr.state_expanded}, false);
					}
					else {
						linkedView.setVisibility(View.GONE);
						indicatorView.setImageState(new int[] {}, false);
					}
					
					firstRun = false;
				} catch (IllegalStateException e) {
				}		
			}
		}
		
	}

	private View resolveLinkedView(int targetViewId, View target) {
		if (target != null) return target;
		
		View resultView = null;
		if (targetViewId != Integer.MIN_VALUE) {
			int depth = 10;
			ViewParent tmp = getParent();
			
			while (tmp != null && resultView == null && depth-- > 0) {
				if (tmp instanceof View) {
					resultView = ((View) tmp).findViewById(targetViewId);
				}

				tmp = tmp.getParent();
			}
		}
		
		if (resultView == null) {
			throw new IllegalStateException("Could not resolve related view.");
		}

		return resultView;
	}

}
