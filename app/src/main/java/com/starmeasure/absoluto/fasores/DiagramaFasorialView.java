package com.starmeasure.absoluto.fasores;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class DiagramaFasorialView extends View {
    private static final String TAG = DiagramaFasorialView.class.getSimpleName();
    private Paint mPaint;
    private Paint mAxisPaint;
    private Paint mPhasorPaint;
    private Paint mTextPaint;
    private PointF mCenter = new PointF(0, 0);
    private float mRadius = 0;
    private List<Fasor> fasors = new ArrayList<>();
    private PointF mPhasor = new PointF();

    public static class Fasor {
        public static enum HeadType {
            ROUND,
            ARROW
        }
        public Fasor(float amplitude, float phase, int color, String label, HeadType headType) {
            this.amplitude = amplitude;
            this.phase = phase;
            this.color = color;
            this.label = label;
            this.headType = headType;
        }

        final float amplitude;
        final float phase;
        final int color;
        final String label;
        final HeadType headType;
    }

    public DiagramaFasorialView(Context context) {
        super(context);
        Log.d(TAG, "PhasorView(C)");
        init();
    }

    public DiagramaFasorialView(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.d(TAG, "PhasorView(CA)");
        init();
    }

    void init() {
        Log.d(TAG, "init");
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(Convert.dpToPx(getContext(), 1));
        mPaint.setPathEffect(new DashPathEffect(new float[]{10, 10}, 0));
        mPaint.setColor(0xff909090);

        mAxisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAxisPaint.setStyle(Paint.Style.STROKE);
        mAxisPaint.setStrokeWidth(Convert.dpToPx(getContext(), 1));
        mAxisPaint.setColor(0xff565656);

        mPhasorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPhasorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mPhasorPaint.setStrokeCap(Paint.Cap.ROUND);
        mPhasorPaint.setStrokeWidth(Convert.dpToPx(getContext(), 2));
        mPhasorPaint.setColor(0xffff3333);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setStyle(Paint.Style.STROKE);
        mTextPaint.setColor(0xff000000);
        mTextPaint.setTextSize(Convert.spToPx(getContext(), 16));
    }

    public void setFasors(List<Fasor> fasors) {
        this.fasors = fasors;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //Log.d(TAG, "onDraw");
        super.onDraw(canvas);

        canvas.drawCircle(mCenter.x, mCenter.y, mRadius, mPaint);
        canvas.drawCircle(mCenter.x, mCenter.y, mRadius*2/3, mPaint);
        canvas.drawLine(mCenter.x, mCenter.y - mRadius,
                mCenter.x, mCenter.y + mRadius,
                mAxisPaint);
        canvas.drawLine(mCenter.x - mRadius, mCenter.y,
                mCenter.x + mRadius, mCenter.y,
                mAxisPaint);
        for (int i = fasors.size()-1; i >= 0; i--) {
            float v = mRadius * Math.min(1, fasors.get(i).amplitude);
            double rad = fasors.get(i).phase * Math.PI / 180;
            mPhasor.x = (float) (v * Math.cos(rad));
            mPhasor.y = (float) (v * -Math.sin(rad));
            mPhasor.offset(mCenter.x, mCenter.y);
            mTextPaint.setColor(fasors.get(i).color);
            Paint.FontMetrics fontMetrics = mTextPaint.getFontMetrics();
            float height = fontMetrics.bottom - fontMetrics.top;
            float dp10 = Convert.dpToPx(getContext(), 10);
            float dp3 = Convert.dpToPx(getContext(), 3);
            canvas.drawText(fasors.get(i).label, dp10, (i+1)*(dp3 + height), mTextPaint);
            mPhasorPaint.setColor(fasors.get(i).color);
            if (v > 0.02)
                drawArrow(canvas, mCenter.x, mCenter.y, mPhasor.x, mPhasor.y,
                        fasors.get(i).headType, mPhasorPaint);
        }
    }

    private void drawArrow(Canvas canvas, float startX, float startY, float stopX, float stopY, Fasor.HeadType headType, Paint paint) {
        canvas.drawLine(startX, startY, stopX, stopY, paint);
        switch (headType) {
            case ROUND:
                canvas.drawCircle(stopX, stopY, Convert.dpToPx(getContext(), 1), paint);
                break;

            case ARROW:
                Path path = new Path();
                float dp3 = Convert.dpToPx(getContext(), 3);
                path.setFillType(Path.FillType.EVEN_ODD);
                path.moveTo(stopX, stopY);
                float angulo = (float) (Math.atan2(stopY - startY, stopX - startX) + 5*Math.PI/6);
                path.rLineTo(
                        dp3 * (float) Math.cos(angulo),
                        dp3 * (float) Math.sin(angulo));
                path.rLineTo(
                        dp3 * (float) Math.cos(angulo + 2*Math.PI/3),
                        dp3 * (float) Math.sin(angulo + 2*Math.PI/3));
                path.close();
                canvas.drawPath(path, paint);
                break;
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        //Log.d(TAG, "*************************** onSizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int minw = getPaddingLeft() + getPaddingRight() + getSuggestedMinimumWidth();
        int w = resolveSizeAndState(minw, widthMeasureSpec, 1);
        int h = resolveSizeAndState(MeasureSpec.getSize(w), heightMeasureSpec, 0);
        setMeasuredDimension(w, h);
        mCenter.set(w/2, h/2);
        mRadius = (getMeasuredWidth() - getPaddingRight() - getPaddingLeft()) / 2 - Convert.dpToPx(getContext(), 5);
    }
}

