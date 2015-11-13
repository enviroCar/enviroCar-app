package org.envirocar.app.view.preferences;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Handler;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;

import com.google.common.base.Preconditions;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;

import org.envirocar.app.R;
import org.envirocar.obd.events.SpeedUpdateEvent;
import org.envirocar.core.logging.Logger;
import org.envirocar.app.views.TypefaceEC;

import javax.inject.Inject;

/**
 * @author dewall
 */
public final class Tempomat extends FrameLayout {
    private static final Logger LOGGER = Logger.getLogger(Tempomat.class);

    // scale configuration
    private static final float SCALE_LOWEST_DEGREE = -135.f;
    private static final float SCALE_HIGHEST_DEGREE = 135.f;
    private static final float SCALE_MAX_DEGREE = 270.f;
    private static final float SCALE_MIN_DEGREE = 0.0f;
    private static final int SCALE_MIN_SPEED = 0;
    private static final int SCALE_MAX_SPEED = 200;
    private static final int SCALE_NOTCH_INTERVAL = 5;

    private static final int NUM_SPEED_NOTCHES = SCALE_MAX_SPEED / SCALE_NOTCH_INTERVAL;
    private static final float DEGREES_PER_NOTCH = SCALE_MAX_DEGREE / NUM_SPEED_NOTCHES;

    // Everything related to the animations.
    private boolean mCurrentlyAnimating;
    private RotationCandidate mNextAnimation;
    private RotationCandidate mPrevAnimation;

    // The bitmap for the static background and its painter.
    private Bitmap mBackground;
    private Paint mBackgroundPaint;

    // Drawing tools to draw the border
    private Paint mBorderPaint;
    private Paint mBorderCirclePaint;
    private RectF mBorderRect;
    private Paint mBorderShadowPaint;

    // Drawing tools to draw the background texture of the cruise control stuff.
    private RectF mFaceRect;
    private Paint mFacePaint;

    // Drawing tools to draw the circle ordered scale.
    private Paint mScaleTexturePaint;
    private Paint mScaleTextPaint;
    private RectF mScaleRect;

    // Drawing tools for the speed-related text
    private TextPaint mSpeedTextPaint;
    private TextPaint mUnitTextPaint;


    private float mCurrentDegree = -135.0f;
    private int mCurrentSpeed = 0;
    private SpeedIndicator mSpeedIndicatorView;


    private Animation.AnimationListener mAnimationListener = new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
            // Nothing to do..
        }

        @Override
        public void onAnimationEnd(Animation animation) {
            synchronized (Tempomat.this) {
                mCurrentlyAnimating = false;
                executeQueuedAnimation();
            }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
            // Nothing to do..
        }
    };


    private Handler mHandler = new Handler();

    @Inject
    protected Bus mBus;

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     */
    public Tempomat(Context context) {
        super(context);
        init(context);
    }

    /**
     * Constructor.
     *
     * @param context the context of the current scope.
     * @param attrs
     */
    public Tempomat(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    /**
     * Constructor.
     *
     * @param context      the context of the current scope.
     * @param attrs
     * @param defStyleAttr
     */
    public Tempomat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        // This FrameLayout view draws something on its own. Therefore, this flag has
        // to be set to true.
        this.setWillNotDraw(false);

        // Initializes the drawing tools.
        setupDrawingTools();

        // Initialize the SpeedIndicatorView and add it to this framelayout. The
        // SpeedIndicatorView will be used to be rotated on the cruise control.
        mSpeedIndicatorView = new SpeedIndicator(getContext());
        mSpeedIndicatorView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        addView(mSpeedIndicatorView);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Register on the Bus;
        //        bus.register(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // Unregister on the bus.
        //        bus.unregister(this);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Preconditions.checkNotNull(mBackground, "mBackground has not beed initialized before.");

        // First, draw the background.
        canvas.drawBitmap(mBackground, 0, 0, mBackgroundPaint);

        // then draw the current speed in the center.
        drawText(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        LOGGER.info(String.format("onMeasure(%s,%s)", "" + widthMeasureSpec, "" +
                heightMeasureSpec));

        // Get the size and mode of width and height.
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        // Choose the dimension, width, and height to select.
        int chosenWidth = chooseDimension(widthMode, widthSize);
        int chosenHeight = chooseDimension(heightMode, heightSize);
        int chosenDimension = Math.min(chosenWidth, chosenHeight);

        // Store the measured width and measured height.
        setMeasuredDimension(chosenDimension, chosenDimension);

        // Do the same with the indicator view.
        mSpeedIndicatorView.measure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        LOGGER.info("onSizeChanged()");
        createBackground();
    }

    /**
     * Receiver method for the speed update event.
     *
     * @param event the SpeedUpdateEvent to receive over the bus.
     */
    @Subscribe
    public void onReceiveSpeedUpdateEvent(SpeedUpdateEvent event) {
        float degree = 0.0f;
        if (event.mSpeed <= SCALE_MIN_SPEED) {
            degree = SCALE_MIN_DEGREE;
        } else if (event.mSpeed >= SCALE_MAX_SPEED) {
            degree = SCALE_MAX_DEGREE;
        } else {
            degree = (((float) event.mSpeed / SCALE_MAX_SPEED) * SCALE_MAX_DEGREE);
        }

        mCurrentDegree = degree;
        mCurrentSpeed = event.mSpeed;

        // Schedule the next rotation to degree.
        mHandler.post(() -> submitRotationToDegree(mCurrentDegree));
    }

    public void setSpeed(int speed) {
        float degree = 0.0f;
        if (speed <= SCALE_MIN_SPEED) {
            degree = SCALE_MIN_DEGREE;
        } else if (speed >= SCALE_MAX_SPEED) {
            degree = SCALE_MAX_DEGREE;
        } else {
            degree = (((float) speed / SCALE_MAX_SPEED) * SCALE_MAX_DEGREE);
        }

        mCurrentDegree = degree;
        mCurrentSpeed = speed;

        // Schedule the next rotation to degree.
        mHandler.post(() -> submitRotationToDegree(mCurrentDegree));
    }

    private void setupDrawingTools() {
        mBorderRect = new RectF(0.1f, 0.1f, 0.9f, 0.9f);

        mBackgroundPaint = new Paint();
        mBackgroundPaint.setFilterBitmap(true);

        // the linear gradient is a bit skewed for realism
        mBorderPaint = new Paint();
        mBorderPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setShader(new LinearGradient(0.40f, 0.0f, 0.60f, 1.0f,
                Color.rgb(0x00, 0x65, 0xA0),
                Color.rgb(0x20, 0x20, 0x20),
                Shader.TileMode.CLAMP));

        mBorderCirclePaint = new Paint();
        mBorderCirclePaint.setAntiAlias(true);
        mBorderCirclePaint.setStyle(Paint.Style.STROKE);
        mBorderCirclePaint.setColor(Color.argb(0x4f, 0x33, 0x36, 0x33));
        mBorderCirclePaint.setStrokeWidth(0.005f);

        float rimSize = 0.02f;
        mFaceRect = new RectF();
        mFaceRect.set(
                mBorderRect.left + rimSize,
                mBorderRect.top + rimSize,
                mBorderRect.right - rimSize,
                mBorderRect.bottom - rimSize);

        mFacePaint = new Paint();
        mFacePaint.setFilterBitmap(true);
        mFacePaint.setStyle(Paint.Style.FILL);
        mFacePaint.setColor(getResources().getColor(R.color.material_blue_grey_950));

        mBorderShadowPaint = new Paint();
        mBorderShadowPaint.setShader(
                new RadialGradient(0.5f, 0.5f,
                        mFaceRect.width() / 2.0f,
                        new int[]{0x00000000, 0x00000500, 0x50000500},
                        new float[]{0.96f, 0.96f, 0.99f},
                        Shader.TileMode.MIRROR));
        mBorderShadowPaint.setStyle(Paint.Style.FILL);

        mScaleTexturePaint = new Paint();
        mScaleTexturePaint.setStyle(Paint.Style.STROKE);
        mScaleTexturePaint.setColor(0x9f048ABF);
        mScaleTexturePaint.setStrokeWidth(0.005f);
        mScaleTexturePaint.setAntiAlias(true);

        mScaleTextPaint = new Paint();
        mScaleTextPaint.setStyle(Paint.Style.FILL);
        mScaleTextPaint.setColor(0x9f048ABF);
        mScaleTextPaint.setStrokeWidth(0.005f);
        mScaleTextPaint.setAntiAlias(true);

        mScaleTextPaint.setTextSize(1.0f);
        mScaleTextPaint.setTypeface(TypefaceEC.Raleway(getContext()));
        mScaleTextPaint.setTextScaleX(0.8f);
        mScaleTextPaint.setTextAlign(Paint.Align.CENTER);
        mScaleTextPaint.setLinearText(true);

        float scalePosition = 0.10f;
        mScaleRect = new RectF();
        mScaleRect.set(
                mFaceRect.left + scalePosition,
                mFaceRect.top + scalePosition,
                mFaceRect.right - scalePosition,
                mFaceRect.bottom - scalePosition);

        // Unit text painter that holds the settings for the unit of measurement paiting (e.g. km/h)
        mUnitTextPaint = new TextPaint();
        mUnitTextPaint.setAntiAlias(true);
        mUnitTextPaint.setColor(Color.WHITE);
        mUnitTextPaint.setStyle(Paint.Style.FILL);
        mUnitTextPaint.setTextAlign(Paint.Align.CENTER);
        mUnitTextPaint.setTextSize(5f);
        mUnitTextPaint.setLinearText(true);
        mUnitTextPaint.setTypeface(TypefaceEC.Newscycle(getContext()));

        // Text painter for the speed text.
        mSpeedTextPaint = new TextPaint();
        mSpeedTextPaint.setAntiAlias(true);
        mSpeedTextPaint.setColor(Color.WHITE);
        mSpeedTextPaint.setStyle(Paint.Style.FILL);
        mSpeedTextPaint.setTextSize(20f);
        mSpeedTextPaint.setLinearText(true);
        mSpeedTextPaint.setTypeface(TypefaceEC.Raleway(getContext()));
        mSpeedTextPaint.setShadowLayer(20.f, 0.0f, 0.0f,
                getResources().getColor(R.color.blue_light_cario));
    }

    /**
     * @param mode
     * @param size
     * @return
     */
    private int chooseDimension(int mode, int size) {
        if (mode == MeasureSpec.AT_MOST || mode == MeasureSpec.EXACTLY) {
            return size;
        } else {
            // in case there has not been any size specified.
            return 300;
        }
    }

    private void createBackground() {
        LOGGER.info("createBackground()");

        if (mBackground != null) {
            mBackground.recycle();
        }

        // Create a bitmap for the background we want to draw on.
        mBackground = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mBackground);

        // Scale the image to the field we focus on.
        float scale = (float) getWidth();
        canvas.save();
        canvas.scale(scale, scale);

        // Draw the basic border texture
        drawStaticBorderTexture(canvas);

        // Draw the general face texture over the border texture
        drawStaticFaceTexture(canvas);

        // Draw the scaling
        drawStaticScaleTexture(canvas);
        canvas.restore();

        // Other scale required. Some Android versions are not able to deal with scales below 1.0.
        canvas.save();
        canvas.scale(scale / 100, scale / 100);
        drawStaticText(canvas);
        canvas.restore();
    }

    private void drawStaticBorderTexture(final Canvas canvas) {
        canvas.drawOval(mBorderRect, mBorderPaint);
        canvas.drawOval(mBorderRect, mBorderCirclePaint);
    }

    private void drawStaticFaceTexture(final Canvas canvas) {
        canvas.drawOval(mFaceRect, mFacePaint);
        canvas.drawOval(mFaceRect, mBorderCirclePaint);
        canvas.drawOval(mFaceRect, mBorderShadowPaint);
    }

    private void drawStaticScaleTexture(final Canvas canvas) {
        canvas.drawOval(mScaleRect, mScaleTexturePaint);

        canvas.save(Canvas.MATRIX_SAVE_FLAG);
        canvas.rotate(SCALE_LOWEST_DEGREE, 0.5f, 0.5f);
        for (int i = 0; i <= NUM_SPEED_NOTCHES; ++i) {
            if (i % 4 == 0) {
                float y1 = mScaleRect.top;
                float y2 = y1 - 0.035f;

                canvas.drawLine(0.5f, y1, 0.5f, y2, mScaleTexturePaint);

                canvas.save();
                canvas.scale(0.045f, 0.045f);
                canvas.drawText(Integer.toString(i * 5),
                        0.5f * 22f, (y2 - 0.010f) * 22f,
                        mScaleTextPaint);
                canvas.restore();
            } else {
                float y1 = mScaleRect.top;
                float y2 = y1 - 0.015f;

                canvas.drawLine(0.5f, y1, 0.5f, y2, mScaleTexturePaint);
            }

            canvas.rotate(DEGREES_PER_NOTCH, 0.5f, 0.5f);
        }
        canvas.restore();
    }

    private void drawStaticText(final Canvas canvas) {
        LOGGER.info("drawStaticText()");
        canvas.drawText("km/h", mScaleRect.centerX() * 100,
                mScaleRect.bottom * 100 - 2.8f, mUnitTextPaint);
    }

    private void drawText(final Canvas canvas) {
        float scale = (float) canvas.getWidth() / 100;

        canvas.save();
        canvas.scale(scale, scale);

        Rect textBounds = new Rect();
        String valueString = Integer.toString(mCurrentSpeed);
        mSpeedTextPaint.getTextBounds(valueString, 0, valueString.length(), textBounds);

        canvas.drawText(valueString, 50.0f - textBounds.exactCenterX(),
                50.0f - textBounds.centerY(), mSpeedTextPaint);

        canvas.restore();
    }

    /**
     * submit a new rotation target degree.
     *
     * @param degree the target degree
     */
    public void submitRotationToDegree(float degree) {
        RotationCandidate candidate = new RotationCandidate(degree);
        queueAnimation(candidate);
    }

    /**
     * Queues the next animation.
     *
     * @param anim the rotation candidate to queue.
     */
    private synchronized void queueAnimation(RotationCandidate anim) {
        mNextAnimation = anim;
        if (!mCurrentlyAnimating) {
            executeQueuedAnimation();
        }
    }

    /**
     * Executes the current queued animation.
     */
    private synchronized void executeQueuedAnimation() {
        if (mNextAnimation == null)
            return;

        // Create a new animation from the next rotation holder class.
        final Animation anim = createAnimation(mNextAnimation);

        if (anim == null)
            return;

        // Set the animating flag.
        mCurrentlyAnimating = true;

        // Starts the animation.
        mSpeedIndicatorView.startAnimation(anim);

        mPrevAnimation = mNextAnimation;
        mNextAnimation = null;
    }

    /**
     * Creates a Android-based {@link Animation} from a {@link org.envirocar.app.view.preferences
     * .Tempomat.RotationCandidate}.
     *
     * @param candidate the holder class for the next animation.
     * @return a Android animation.
     */
    private Animation createAnimation(RotationCandidate candidate) {
        float start;
        if (mPrevAnimation != null) {
            start = mPrevAnimation.finalDegree;
        } else {
            start = SCALE_MIN_DEGREE;
        }

        if (start == candidate.finalDegree) {
            return null;
        }

        RotateAnimation anim = new RotateAnimation(start, candidate.finalDegree, Animation
                .RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        setInterpolatorAndDuration(start, candidate.finalDegree, anim);
        anim.setFillAfter(true);

        anim.setAnimationListener(mAnimationListener);
        return anim;
    }

    private void setInterpolatorAndDuration(float startDegree, float endDegree, RotateAnimation
            anim) {
        if (Math.abs(startDegree - endDegree) > 10) {
            anim.setInterpolator(new AccelerateDecelerateInterpolator());
            anim.setDuration(500);
        } else {
            anim.setInterpolator(new LinearInterpolator());
            anim.setDuration(50);
        }
    }


    /**
     * This view class represents the inidcator for speed. The indicator is only drawn once in a
     * view
     */
    private class SpeedIndicator extends View {

        private Bitmap mSpeedIndicatorImage;

        private Paint mHandPaint;
        private Path mHandPath;

        /**
         * Constructor.
         *
         * @param context the context of the current scope.
         */
        public SpeedIndicator(Context context) {
            super(context);
            init();
        }

        /**
         * Constructor.
         *
         * @param context
         * @param attrs
         */
        public SpeedIndicator(Context context, AttributeSet attrs) {
            super(context, attrs);
            init();
        }

        public SpeedIndicator(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            init();
        }

        private void init() {
            mHandPaint = new Paint();
            mHandPaint.setAntiAlias(true);
            mHandPaint.setColor(getResources().getColor(R.color.green_dark_cario));
            mHandPaint.setShadowLayer(1.01f, -5.005f, -5.005f, 0x7f000000);
            mHandPaint.setStyle(Paint.Style.FILL);
            mHandPaint.setStrokeWidth(3);

            mHandPath = new Path();
            mHandPath.moveTo(0.5f, 0.5f - 0.2f);
            mHandPath.lineTo(0.5f - 0.010f, 0.5f - 0.2f - 0.007f);
            mHandPath.lineTo(0.5f - 0.002f, 0.5f - 0.32f);
            mHandPath.lineTo(0.5f + 0.002f, 0.5f - 0.32f);
            mHandPath.lineTo(0.5f + 0.010f, 0.5f - 0.2f - 0.007f);
            mHandPath.lineTo(0.5f, 0.5f - 0.2f);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            LOGGER.info(String.format("SpeedIndicator.onMeasure(%s,%s)", "" + widthMeasureSpec, "" +
                    heightMeasureSpec));

            int widthMode = MeasureSpec.getMode(widthMeasureSpec);
            int widthSize = MeasureSpec.getSize(widthMeasureSpec);

            int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);

            int chosenWidth = chooseDimension(widthMode, widthSize);
            int chosenHeight = chooseDimension(heightMode, heightSize);

            int chosenDimension = Math.min(chosenWidth, chosenHeight);

            setMeasuredDimension(chosenDimension, chosenDimension);
        }

        @Override
        protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            LOGGER.info("SpeedIndicator.onSizeChanged()");
            createSpeedIndicator();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            Preconditions.checkNotNull(mSpeedIndicatorImage, "mSpeedIndicator cannot be null.");
            canvas.drawBitmap(mSpeedIndicatorImage, 0, 0, mBackgroundPaint);
        }

        private void createSpeedIndicator() {
            LOGGER.info("SpeedIndicator.createSpeedIndicator()");

            // Recycle the previous speed indicator.
            if (mSpeedIndicatorImage != null) {
                mSpeedIndicatorImage.recycle();
            }

            mSpeedIndicatorImage = Bitmap.createBitmap(getWidth(), getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mSpeedIndicatorImage);
            canvas.save();
            float scale = (float) getWidth();
            canvas.scale(scale, scale);
            canvas.rotate(SCALE_LOWEST_DEGREE, 0.5f, 0.5f);
            canvas.drawPath(mHandPath, mHandPaint);
            canvas.restore();
        }
    }

    /**
     * Holder class that holds candidate for the next rotation animation.
     */
    private class RotationCandidate {
        private float finalDegree;

        public RotationCandidate(float degree) {
            if (degree < SCALE_LOWEST_DEGREE) {
                this.finalDegree = SCALE_MIN_DEGREE;
            } else if (degree > SCALE_MAX_DEGREE) {
                this.finalDegree = SCALE_MAX_DEGREE;
            } else {
                this.finalDegree = degree;
            }
        }
    }
}
