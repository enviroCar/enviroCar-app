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

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Typeface;

/**
 * Setting the fonts for the application
 * 
 */
public final class TypefaceEC {
	public static final Typeface Raleway(Context ctx) {
		Typeface typeface = Typeface.createFromAsset(ctx.getAssets(), "Raleway-Regular.ttf");
		return typeface;
	}

	public static final Typeface Newscycle(Context ctx) {
		Typeface typeface = Typeface.createFromAsset(ctx.getAssets(), "newscycle_regular.ttf");
		return typeface;
	}

	public static void applyCustomFont(ViewGroup list, Typeface customTypeface) {
		for (int i = 0; i < list.getChildCount(); i++) {
			View view = list.getChildAt(i);
			if (view instanceof ViewGroup) {
				applyCustomFont((ViewGroup) view, customTypeface);
			} else if (view instanceof TextView) {
				((TextView) view).setTypeface(customTypeface);
			}
		}
	}
}
