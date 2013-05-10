package car.io.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ClipDrawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import car.io.R;

/**
 * Taken and altered from
 * 
 * Created by IntelliJ IDEA. User: bryce Date: 4/15/11 Time: 10:11 AM
 * 
 * Usage example for the small progress bar:
 * <com.ifit.android.component.RoundProgress android:id="@+id/red_progress_bar"
 * android:layout_width="200dip" android:layout_height="35dip"
 * android:padding="3dip" ifit:max="100" ifit:progress="50"
 * ifit:progressDrawable="@drawable/red_progress_clip"
 * ifit:track="@drawable/progress_bar_fill_bg" />
 * 
 * or for the bigger blue progress bar <com.ifit.android.component.RoundProgress
 * android:id="@+id/blue_progress_bar" android:layout_width="200dip"
 * android:layout_height="52dip" android:padding="3dip" ifit:max="100"
 * ifit:progress="50" ifit:progressDrawable="@drawable/dark_blue_progress_clip"
 * ifit:track="@drawable/progress_bar_stripes_track" />
 * 
 */
public class RoundProgress extends RelativeLayout {
	private ImageView progressDrawableImageView;
	private ImageView trackDrawableImageView;
	private double max = 100;

	public int getMax() {
		Double d = Double.valueOf(max);
		return d.intValue();
	}

	public double getMaxDouble() {
		return max;
	}

	public void setMax(int max) {
		// Integer maxInt = new Integer(max);
		// Integer maxInt = Integer.valueOf(max);
		// maxInt.doubleValue();

		this.max = Double.valueOf(max);
	}

	public void setMax(double max) {
		this.max = max;
	}

	public RoundProgress(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater inflater = (LayoutInflater) context
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.round_progress, this);
		setup(context, attrs);
	}

	protected void setup(Context context, AttributeSet attrs) {
		TypedArray a = context.obtainStyledAttributes(attrs,
				R.styleable.RoundProgress);

		final String xmlns = "http://schemas.android.com/apk/res/car.io";
		int bgResource = attrs.getAttributeResourceValue(xmlns,
				"progressDrawable", 0);
		progressDrawableImageView = (ImageView) findViewById(R.id.progress_drawable_image_view);
		progressDrawableImageView.setBackgroundResource(bgResource);

		int trackResource = attrs.getAttributeResourceValue(xmlns, "track", 0);
		trackDrawableImageView = (ImageView) findViewById(R.id.track_image_view);
		trackDrawableImageView.setBackgroundResource(trackResource);

		int progress = attrs.getAttributeIntValue(xmlns, "progress", 0);
		setProgress(progress);
		int max = attrs.getAttributeIntValue(xmlns, "max", 100);
		setMax(max);

		// int numTicks = attrs.getAttributeIntValue(xmlns, "numTicks", 0);

		a.recycle();

		ProgressBarOutline outline = new ProgressBarOutline(context);
		addView(outline);
	}

	public void setProgress(Integer value) {
		setProgress((double) value);
	}

	public void setProgress(double value) {
		ClipDrawable drawable = (ClipDrawable) progressDrawableImageView
				.getBackground();
		double percent = (double) value / (double) max;
		int level = (int) Math.floor(percent * 10000);

		drawable.setLevel(level);
	}
}
