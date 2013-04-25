package de.ifgi.car.iomockup;

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
}
