package car.io.views;

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
