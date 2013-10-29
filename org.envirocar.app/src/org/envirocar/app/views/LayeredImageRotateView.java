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
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;

/**
 * This {@link View} extends from {@link RelativeLayout}.
 * It can be used to create layered {@link ImageView} components,
 * making one of these rotatable. The last child ImageView of this
 * View is drawed on top of all others.
 * <br/>
 * <br/>
 * Layout example:
 * <pre>
 * {@code
 *    <org.envirocar.app.views.LayeredImageRotateView android:layout_width="wrap_content"
 *        android:layout_height="wrap_content"
 *        android:id="@+id/speedometerView"
 *        enviroCar:minimumDegree="0.0"
 *        enviroCar:maximumDegree="360.0"
 *        enviroCar:minimumScaleValue="0.0"
 *        enviroCar:maximumScaleValue="250.0"
 *        enviroCar:rotatableImageViewid="@+id/arrowImage">
 *        <ImageView 
 *            android:layout_width="150dp"
 *        	  android:layout_height="150dp"
 *            android:contentDescription="@string/app_name"
 *            android:src="@drawable/speedometer"/>
 *        <ImageView
 *            android:layout_width="150dp"
 *        	  android:layout_height="150dp"
 *            android:contentDescription="@string/app_name" 
 *            android:id="@+id/arrowImage"
 *            android:src="@drawable/speedometer_arrow"/>
 *        
 *    </org.envirocar.co2meter.LayeredImageRotateView> 
 * }
 * </pre>
 * 
 * @author matthes rieke
 *
 */
public class LayeredImageRotateView extends RelativeLayout {

	private static final String NAMESPACE = "http://envirocar.org";
	private static final String MINIMUM_DEGREE_KEY = "minimumDegree";
	private static final String MAXIMUM_DEGREE_KEY = "maximumDegree";
	private static final String MINIMUM_SCALE_KEY = "minimumScaleValue";
	private static final String MAXIMUM_SCALE_KEY = "maximumScaleValue";
	private static final String ROTATABLE_RESOURCE_ID = "rotatableImageViewid";
	private ImageView rotatableView;
	private LocalAnimationListener animationListener;
	private RotationCandidate nextAnimation;
	private RotationCandidate previosAnimation;
	private float minimumDegree = 0f;
	private float maximumDegree = 360f;
	private int rotatableId = Integer.MIN_VALUE;
	private float minimumScaleValue = 0f;
	private float maximumScaleValue = 360f;
	private AtomicBoolean firstRun = new AtomicBoolean(true);

	public LayeredImageRotateView(Context context) {
		super(context);
	}
	
	public LayeredImageRotateView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		parseAttributes(attrs);
	}

	public LayeredImageRotateView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		
		parseAttributes(attrs);
	}
	
	private void parseAttributes(AttributeSet attrs) {
		minimumDegree = attrs.getAttributeFloatValue(NAMESPACE, MINIMUM_DEGREE_KEY, 0.0f);
		maximumDegree = attrs.getAttributeFloatValue(NAMESPACE, MAXIMUM_DEGREE_KEY, 360.0f);
		minimumScaleValue = attrs.getAttributeFloatValue(NAMESPACE, MINIMUM_SCALE_KEY, 0.0f);
		maximumScaleValue = attrs.getAttributeFloatValue(NAMESPACE, MAXIMUM_SCALE_KEY, 360.0f);
		rotatableId = attrs.getAttributeResourceValue(NAMESPACE, ROTATABLE_RESOURCE_ID, Integer.MIN_VALUE);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		if (rotatableId != Integer.MIN_VALUE) {
			try {
				setRotatableImageView(rotatableId);
			} catch (IllegalArgumentException e) {
				Log.w("enviroCar", e.getMessage());
			}
		}
		
		setWillNotDraw(false);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (firstRun.getAndSet(false)) {
			for (int i = 0; i < getChildCount(); i++) {
				View child = getChildAt(i);
				if (child instanceof OnParentDrawnListener) {
					((OnParentDrawnListener) child).onParentDrawn(this);
					child.invalidate();
				}
			}
			setWillNotDraw(true);
		}
	}
	
	/**
	 * Set the {@link ImageView} which should be rotated through {@link #submitScaleValue(float)}
	 * or {@link #submitScaleValue(float)}.
	 * 
	 * @param imageViewRes the resource id of the {@link ImageView} to rotate
	 */
	public void setRotatableImageView(int imageViewRes) {
		View v = findViewById(imageViewRes);
		
		if (v == null || !(v instanceof ImageView)) {
			throw new IllegalArgumentException("View with ID "+imageViewRes+" is not valid.");
		}
		
		rotatableView = (ImageView) v;
		animationListener = new LocalAnimationListener();
	}
	
	/**
	 * submit a new rotation target degree. see {@link #setMaximumDegree(float)}
	 * and {@link #setMinimumDegree(float)}.
	 * 
	 * @param degree the target degree
	 */
	public void submitRotationToDegree(float degree) {
		RotationCandidate candidate = new RotationCandidate(degree);
		queueAnimation(candidate);
	}
	
	/**
	 * submit a new target scale value. see {@link #setMaximumScaleValue(float)}
	 * and {@link #setMinimumScaleValue(float)}.
	 * 
	 * @param value the target scale value
	 */
	public void submitScaleValue(float value) {
		submitRotationToDegree(convertScaleValueToDegree(value));
	}
	
	/**
	 * Use this to set the max of a ranged scale value in
	 * phenomenon space (e.g. speed)
	 * 
	 * @param max the maximum scale value
	 */
	public void setMaximumScaleValue(float max) {
		this.maximumScaleValue = max;
	}
	
	/**
	 * Use this to set the min of a ranged scale value in
	 * phenomenon space (e.g. speed)
	 * 
	 * @param min the minimum scale value
	 */
	public void setMinimumScaleValue(float min) {
		this.minimumScaleValue = min;
	}
	
	/**
	 * The minimum value of target rotation degrees
	 * 
	 * @param minimumDegree the min degree
	 */
	public void setMinimumDegree(float minimumDegree) {
		this.minimumDegree = minimumDegree;
	}

	/**
	 * The maximum value of target rotation degrees
	 * 
	 * @param maximumDegree the max degree
	 */
	public void setMaximumDegree(float maximumDegree) {
		this.maximumDegree = maximumDegree;
	}

	private float convertScaleValueToDegree(float value) {
		float inRange;
		if (value < this.minimumScaleValue) {
			inRange = minimumScaleValue;
		}
		else if (value > this.maximumScaleValue) {
			inRange = maximumScaleValue;
		}
		else {
			inRange = value;
		}
		
		float range = maximumScaleValue-minimumScaleValue;
		
		float degree = minimumDegree + (maximumDegree * ((inRange-minimumScaleValue) / range));
		return degree;
	}

	private synchronized void queueAnimation(RotationCandidate anim) {
		nextAnimation = anim;
		if (!animationListener.currentlyAnimating) {
			executeQueuedAnimation();
		}
	}

	private synchronized void executeQueuedAnimation() {
		if (nextAnimation == null) return;
		
		Animation anim = createAnimation(nextAnimation);
		
		if (anim == null) return;
		
		rotatableView.startAnimation(anim);
		previosAnimation = nextAnimation;
		nextAnimation = null;
		animationListener.currentlyAnimating = true;
	}

	private Animation createAnimation(RotationCandidate candidate) {
		float start;
		if (previosAnimation != null) {
			start = previosAnimation.finalDegree;
		} else {
			start = minimumDegree;
		}
		
		if (start == candidate.finalDegree) {
			return null;
		}
		
		RotateAnimation anim = new RotateAnimation(start, candidate.finalDegree, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
		setInterpolatorAndDuration(start, candidate.finalDegree, anim);
		anim.setFillAfter(true);
		
		anim.setAnimationListener(animationListener);
		return anim;
	}

	private void setInterpolatorAndDuration(float startDegree, float endDegree, RotateAnimation anim) {
		if (Math.abs(startDegree - endDegree) > 10) {
			anim.setInterpolator(new AccelerateDecelerateInterpolator());
			anim.setDuration(500);
		}
		else {
			anim.setInterpolator(new LinearInterpolator());
			anim.setDuration(50);
		}
	}

	private class LocalAnimationListener implements AnimationListener {

		boolean currentlyAnimating = false;
		
		@Override
		public void onAnimationEnd(Animation animation) {
			synchronized (LayeredImageRotateView.this) {
				currentlyAnimating = false;
				executeQueuedAnimation();
			}
		}

		@Override
		public void onAnimationRepeat(Animation animation) {
		}

		@Override
		public void onAnimationStart(Animation animation) {
		}
		
	}
	
	private class RotationCandidate {

		private float finalDegree;

		public RotationCandidate(float degree) {
			if (degree < minimumDegree) {
				this.finalDegree = minimumDegree;
			}
			else if (degree > maximumDegree) {
				this.finalDegree = maximumDegree;
			}
			else {
				this.finalDegree = degree;
			}
		}
		
	}
}
