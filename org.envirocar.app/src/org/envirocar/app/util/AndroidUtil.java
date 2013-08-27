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
package org.envirocar.app.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

/**
 * singleton class for providing some android specific functions,
 * such as Toast, SharedPreferences, ...
 * 
 * @author matthes rieke
 *
 */
public class AndroidUtil {

	private static AndroidUtil instance;
	private Context applicationContext;

	private AndroidUtil(Context appCont) {
		this.applicationContext = appCont;
	}
	
	public static synchronized void init(Context ac) {
		instance = new AndroidUtil(ac);
	}
	
	public static synchronized AndroidUtil getInstance() {
		return instance;
	}
	
	/**
	 * @param text the text to toast
	 * @param size use {@link Toast#LENGTH_SHORT} and {@link Toast#LENGTH_LONG}
	 */
	public void makeTextToast(String text, int size) {
		Toast.makeText(applicationContext, text,
				size).show();
	}

	public SharedPreferences getDefaultSharedPreferences() {
		return PreferenceManager.getDefaultSharedPreferences(applicationContext);
	}
	
}
