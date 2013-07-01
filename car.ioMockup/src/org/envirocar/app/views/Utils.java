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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.location.LocationManager;
import android.os.Build;

public class Utils {

	
	public static int getActionBarId(){
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                return Class.forName("com.actionbarsherlock.R$id").getField("abs__action_bar_title").getInt(null);
            }
            else {
                // Use reflection to get the actionbar title TextView and set the custom font. May break in updates.
                return Class.forName("com.android.internal.R$id").getField("action_bar_title").getInt(null);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
	}
	
	/*
	 * Util functions for the Checklist
	 */
	public static boolean isGPSEnabled(Context context){
		return ((LocationManager) context.getSystemService(android.content.Context.LOCATION_SERVICE) != null) && ((LocationManager) context.getSystemService(android.content.Context.LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER);
	}
	
	public static boolean isBluetoothEnabled(Context context){
		return BluetoothAdapter.getDefaultAdapter() != null && BluetoothAdapter.getDefaultAdapter().isEnabled();
	}
	
	



	    /** Transform ISO 8601 string to Calendar. */
	    public static long isoDateToLong(final String iso8601string) throws ParseException {
	        String s = iso8601string.replace("Z", "+00:00");
	        try {
	            s = s.substring(0, 22) + s.substring(23);
	        } catch (IndexOutOfBoundsException e) {
	            throw new ParseException("Invalid length", 0);
	        }
	        Date date = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse(s);

	        return date.getTime();
	    }


//	public static String MD5(String md5) {
//		try {
//			java.security.MessageDigest md = java.security.MessageDigest
//					.getInstance("MD5");
//			byte[] array = md.digest(md5.getBytes());
//			StringBuffer sb = new StringBuffer();
//			for (int i = 0; i < array.length; ++i) {
//				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
//						.substring(1, 3));
//			}
//			return sb.toString();
//		} catch (java.security.NoSuchAlgorithmException e) {
//		}
//		return null;
//	}
}
