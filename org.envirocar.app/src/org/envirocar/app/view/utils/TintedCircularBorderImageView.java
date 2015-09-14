package org.envirocar.app.view.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.util.AttributeSet;

import org.envirocar.app.R;

/**
 * @author dewall
 */
public class TintedCircularBorderImageView extends CircularBorderedImageView {

    /**
     * @param context
     */
    public TintedCircularBorderImageView(Context context) {
        super(context);
    }

    /**
     * @param context
     * @param attrs
     */
    public TintedCircularBorderImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /**
     * @param context
     * @param attrs
     * @param defStyle
     */
    public TintedCircularBorderImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

    }


    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        if (drawable == null)
            return;

//        drawable = DrawableCompat.wrap(drawable);
//        DrawableCompat.setTint(drawable, Color.RED);
//        setColorFilter(Color.RED);



        if (drawable instanceof BitmapDrawable) {
            mBitmap = ((BitmapDrawable) drawable).getBitmap();
            return;
        }

        Bitmap bitmap;

        if (drawable instanceof ColorDrawable) {
            bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
//        mBitmap = bitmap;
    }

}
