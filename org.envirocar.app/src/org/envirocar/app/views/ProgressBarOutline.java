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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * Taken and altered from:
 * 
 * Created by IntelliJ IDEA. User: bryce Date: 4/15/11 Time: 3:20 PM
 */
public class ProgressBarOutline extends View {

	private Paint paint;

	public ProgressBarOutline(Context context) {
		super(context);
		paint = new Paint();
	}

	@Override
	public void onDraw(Canvas canvas) {
		paint.setColor(Color.WHITE);
		paint.setStrokeWidth(2);
		paint.setAntiAlias(true);
		paint.setStyle(Paint.Style.STROKE);
		paint.setARGB(255, 230, 230, 230);
		RectF r = new RectF(1, 1, getWidth() - 2, getHeight() - 2);
		canvas.drawRoundRect(r, getHeight() / 2, getHeight() / 2, paint);
	}

}
