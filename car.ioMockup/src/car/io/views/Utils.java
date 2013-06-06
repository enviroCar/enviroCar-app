package car.io.views;

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

	public static String MD5(String md5) {
		try {
			java.security.MessageDigest md = java.security.MessageDigest
					.getInstance("MD5");
			byte[] array = md.digest(md5.getBytes());
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < array.length; ++i) {
				sb.append(Integer.toHexString((array[i] & 0xFF) | 0x100)
						.substring(1, 3));
			}
			return sb.toString();
		} catch (java.security.NoSuchAlgorithmException e) {
		}
		return null;
	}
}
