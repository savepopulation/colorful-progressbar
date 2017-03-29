package com.raqun.colorfulprogressbar;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by test on 27/03/17.
 */

public class ColorfulProgressBar extends View {
    /**
     * ColorfulProgressBar should contain Max 4 Parts
     */
    private static final int MAX_COUNT_BAR_PART = 4;


    /**
     * ColorfulProgressBar should contain Min
     */
    private static final int MIN_COUNT_BAR_PART = 1;


    /**
     * Default value for No Resource
     */
    private static final int NO_RES = 0;


    /**
     * Default value for StrokeWidth
     */
    private static final int DEFAULT_STROKE_WIDTH = 8;


    /**
     * Default value for start angle
     */
    private static final float DEFAULT_START_ANGLE = -90;


    /**
     * Default View Color
     */
    private static final int DEFAULT_PRIMARY_COLOR = android.R.color.holo_blue_dark;

    /**
     * Default animation duration
     */
    private static final int DEFAULT_PROGRESS_ANIMATION_DURATION = 60000;


    /**
     * Default RectF
     * Gets initialized in initRect() method
     */
    @NonNull
    private RectF mRect;


    /**
     * Default Value Animator
     */
    private ValueAnimator mValueAnimator;


    /**
     * Current Angle of View
     */
    private float mAngle;


    /**
     * Angle For Each Part
     */
    private float mBreakPointAngle;

    /**
     *
     */
    private float mStartAngle;

    /**
     * Contains Bar Parts
     * Each Color Passed is a BarPart
     * Always contains at least 1 part
     */
    @NonNull
    private List<BarPart> mBarParts;


    /**
     * Index of the current drawing bar part.
     */
    private int mCurrentBarPartIndex;


    /**
     * Status Listener for Drawing Status.
     */
    @Nullable
    private DrawingStatusListener mListener;


    /**
     * If true Progress starts auto
     */
    private boolean mIsAutoAnimate;


    /**
     * Size of View
     */
    private int mSize;


    /**
     * Stroke Width of View
     */
    private int mStrokeWidth;


    /**
     * Total progress duration
     */
    private int mDuration;


    /**
     * Primary Color of Progress;
     */
    private int mPrimaryColor;


    /**
     * If true after Progress ends, View paints Primary Color
     */
    private boolean mIsFinalizeWithPrimaryColor;


    /**
     * Rect Width
     */
    private int mRectWidth = 400;


    /**
     * Rect Height
     */
    private int mRectHeight = 400;

    public ColorfulProgressBar(Context context) {
        super(context);
        initAttributes(null);
        initRect();
    }

    public ColorfulProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ColorfulProgressBar);
        initAttributes(attributes);
        initRect();
    }

    public ColorfulProgressBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        final TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.ColorfulProgressBar, defStyle, 0);
        initAttributes(attributes);
        initRect();
    }

    private void initAttributes(@Nullable final TypedArray attributes) {
        if (attributes != null) {
            try {
                mStrokeWidth = (int) attributes.getDimension(R.styleable.ColorfulProgressBar_strokeWidth, DEFAULT_STROKE_WIDTH);
                mIsAutoAnimate = attributes.getBoolean(R.styleable.ColorfulProgressBar_autoPlay, false);
                mStartAngle = attributes.getFloat(R.styleable.ColorfulProgressBar_startAngle, DEFAULT_START_ANGLE);
                mDuration = attributes.getInt(R.styleable.ColorfulProgressBar_duration, DEFAULT_PROGRESS_ANIMATION_DURATION);
                mPrimaryColor = attributes.getColor(R.styleable.ColorfulProgressBar_primaryColor, getDefaultColor());
                mIsFinalizeWithPrimaryColor = attributes.getBoolean(R.styleable.ColorfulProgressBar_finalizeWithColorPrimary, false);
                initBars(attributes.getResourceId(R.styleable.ColorfulProgressBar_colors, NO_RES));
            } finally {
                attributes.recycle();
            }
        } else {
            // Set Default Values
            mStrokeWidth = DEFAULT_STROKE_WIDTH;
            mIsAutoAnimate = false;
            mStartAngle = DEFAULT_START_ANGLE;
            mDuration = DEFAULT_PROGRESS_ANIMATION_DURATION;
            mPrimaryColor = getDefaultColor();
            mIsFinalizeWithPrimaryColor = false;
            initBars(NO_RES);
        }
    }

    private void initBars(int resId) {
        int[] colors;
        if (resId == NO_RES) {
            colors = new int[1];
            colors[0] = DEFAULT_PRIMARY_COLOR;
        } else {
            colors = getResources().getIntArray(resId);
            if (colors.length < MIN_COUNT_BAR_PART || colors.length > MAX_COUNT_BAR_PART) {
                throw new IllegalArgumentException("Max color size is 4");
            }
        }

        setBreakPointAngle(360 / colors.length);
        initBarParts(colors);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        this.mRectHeight = h;
        this.mRectWidth = w;
        //initRect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final BarPart barPart = mBarParts.get(mCurrentBarPartIndex);
        if (mAngle < barPart.mEndPoint) {
            if (mCurrentBarPartIndex > 0) {
                for (int i = 0; i < mCurrentBarPartIndex; i++) {
                    final BarPart prevBarPart = mBarParts.get(i);
                    canvas.drawArc(mRect, prevBarPart.mStartPoint, mBreakPointAngle, false, prevBarPart.mPaint);
                }
            }
            canvas.drawArc(mRect, barPart.mStartPoint, mAngle - barPart.mLimit, false, barPart.mPaint);
        } else {
            if (mCurrentBarPartIndex < mBarParts.size() - 1) {
                ++mCurrentBarPartIndex;
                draw(canvas);
            } else {
                canvas.drawArc(mRect, mStartAngle, 360, false, barPart.mPaint);
                finishDrawing();
            }
        }
    }

    private void initRect() {
        mRect = new RectF(mStrokeWidth, mStrokeWidth, mRectHeight + mStrokeWidth, mRectWidth + mStrokeWidth);
        if (mIsAutoAnimate) {
            animate(mDuration);
        }
    }

    private void initBarParts(int[] colors) {
        if (mBarParts == null) {
            mBarParts = new ArrayList<>();
        }

        for (int i = 0; i < colors.length; i++) {
            if (colors[i] != NO_RES) {
                mBarParts.add(new BarPart(colors[i], i));
            }
        }
    }

    private void setBreakPointAngle(float breakPointAngle) {
        this.mBreakPointAngle = breakPointAngle;
    }

    private int getDefaultColor() {
        return DEFAULT_PRIMARY_COLOR;
    }

    public final void animate(long duration) {
        cancelAnimation();
        final float oldAngle = mAngle;
        mValueAnimator = ValueAnimator.ofFloat(oldAngle, 360);
        mValueAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                // Empty Method
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                finishDrawing();
                if (mListener != null) {
                    mListener.onDrawFinished();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                // Empty Method
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
                // Empty Method
            }
        });
        mValueAnimator.setDuration(duration);
        mValueAnimator.setInterpolator(new LinearInterpolator());
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mAngle = (float) animation.getAnimatedValue();
                postInvalidate();
            }
        });
        mValueAnimator.start();
    }

    public void cancelAnimation() {
        if (mValueAnimator != null) {
            mValueAnimator.removeAllListeners();
            mValueAnimator.cancel();
            mValueAnimator = null;
        }
    }

    public void finishDrawing() {
        if (mIsFinalizeWithPrimaryColor) {
            for (BarPart barPart : mBarParts) {
                barPart.mPaint.setColor(mPrimaryColor);
            }
        }
    }

    // BAR PART
    private final class BarPart {
        private final Paint mPaint;
        private final float mStartPoint;
        private final float mEndPoint;
        private final float mLimit;

        BarPart(int color, int order) {
            this.mPaint = initPaint(color, mStrokeWidth);
            this.mStartPoint = mStartAngle + (order * mBreakPointAngle);
            this.mEndPoint = (order + 1) * mBreakPointAngle;
            this.mLimit = order * mBreakPointAngle;
        }

        @NonNull
        private Paint initPaint(int color, int strokeWidth) {
            final Paint paint = new Paint();
            paint.setAntiAlias(true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(strokeWidth);
            paint.setColor(color);
            paint.setStrokeJoin(Paint.Join.ROUND);
            paint.setStrokeCap(Paint.Cap.ROUND);
            return paint;
        }
    }

    // STATUS LISTENER
    public interface DrawingStatusListener {
        void onDrawFinished();
    }
}
