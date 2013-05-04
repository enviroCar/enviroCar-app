package car.io.views;

import android.content.Context;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public final class TYPEFACE {
    public static final Typeface Raleway(Context ctx){
        Typeface typeface = Typeface.createFromAsset(ctx.getAssets(), "Raleway-Regular.ttf");
        return typeface;
    }
    public static final Typeface Newscycle(Context ctx){
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
