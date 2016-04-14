package com.google.snappy;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by andyhuibers on 9/18/14.
 */

public class PreviewOverlay extends View {
    private static final String TAG = "SNAPPY_FACE";

    private boolean mShow3AInfo;
    private boolean mShowGyroGrid;
    private int mColor;
    private int mColor2;
    private Paint mPaint;
    private Paint mPaint2;

    // Rendered data:
    private NormalizedFace[] mFaces;
    private float mExposure;
    private float mLens;
    private int mAfState;
    private float mFovLargeDegrees;
    private float mFovSmallDegrees;
    float[] mAngles = new float[2];

    public PreviewOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        Resources res = getResources();
        mColor = res.getColor(R.color.face_color);
        mPaint = new Paint();
        mPaint.setColor(mColor);
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(res.getDimension(R.dimen.face_circle_stroke));

        mColor2 = res.getColor(R.color.hud_color);
        mPaint2 = new Paint();
        mPaint2.setAntiAlias(true);
        mPaint2.setStyle(Paint.Style.STROKE);
        mPaint2.setStrokeWidth(res.getDimension(R.dimen.hud_stroke));
    }

    public void setFrameData(NormalizedFace[] faces, float normExposure, float normLens, int afState) {
        mFaces = faces;
        mExposure = normExposure;
        mLens = normLens;
        mAfState = afState;
        this.setVisibility(VISIBLE);
        invalidate();
    }

    public void show3AInfo(boolean show) {
        mShow3AInfo = show;
        this.setVisibility(VISIBLE);
        invalidate();
    }

    public void setGyroAngles(float[] angles) {
        mAngles = angles;
    }

    public void setFieldOfView(float fovLargeDegrees, float fovSmallDegrees) {
        mFovLargeDegrees = fovLargeDegrees;
        mFovSmallDegrees = fovSmallDegrees;
    }

    public void showGyroGrid(boolean show) {
        mShowGyroGrid = show;
        this.setVisibility(VISIBLE);
        invalidate();
    }

    private static double SHORT_LOG_EXPOSURE = Math.log10(1000000000 / 10000); // 1/10000 second
    private static double LONG_LOG_EXPOSURE = Math.log10(1000000000 / 10); // 1/10 second
    float[] yGridValues = new float[] {
            (float) ((Math.log10(1000000000 / 30) - SHORT_LOG_EXPOSURE) / (LONG_LOG_EXPOSURE - SHORT_LOG_EXPOSURE)),
            (float) ((Math.log10(1000000000 / 100) - SHORT_LOG_EXPOSURE) / (LONG_LOG_EXPOSURE - SHORT_LOG_EXPOSURE)),
            (float) ((Math.log10(1000000000 / 1000) - SHORT_LOG_EXPOSURE) / (LONG_LOG_EXPOSURE - SHORT_LOG_EXPOSURE))};

    /** Focus states
     CONTROL_AF_STATE_INACTIVE 0
     CONTROL_AF_STATE_PASSIVE_SCAN 1
     CONTROL_AF_STATE_PASSIVE_FOCUSED 2
     CONTROL_AF_STATE_ACTIVE_SCAN 3
     CONTROL_AF_STATE_FOCUSED_LOCKED 4
     CONTROL_AF_STATE_NOT_FOCUSED_LOCKED 5
     CONTROL_AF_STATE_PASSIVE_UNFOCUSED 6
     */

    @Override
    protected void onDraw(Canvas canvas) {
        if (mFaces == null) {
            return;
        }
        float previewW = this.getWidth();
        float previewH = this.getHeight();

        // 3A visualizatoins
        if (mShow3AInfo) {

            // Draw 3A ball on a rail
            if (false) {
                mPaint2.setStyle(Paint.Style.FILL_AND_STROKE);
                mPaint2.setColor(0x33FFFFFF);
                canvas.drawRect(0.04f * previewW, 0.03f * previewH, 0.96f * previewW, 0.05f * previewH, mPaint2);

                mPaint2.setStyle(Paint.Style.FILL_AND_STROKE);
                float x1 = (0.92f * mLens + 0.04f) * previewW;
                float y1 = (0.04f) * previewH;
                mPaint2.setColor(0xFF000000);
                canvas.drawCircle(x1, y1, 20, mPaint2);
                mPaint2.setColor(0xFFDDDDDD);
                canvas.drawCircle(x1, y1, 18, mPaint2);
            }

            // Draw AF center thing
            mPaint2.setStyle(Paint.Style.FILL_AND_STROKE);
            float x2 = 0.5f * previewW;
            float y2 = 0.5f * previewH;
            mPaint2.setColor(0x990000FF);
            String text = "NOT IN CAF";
            if (mAfState == 1) { // passive scan RED
                mPaint2.setColor(0x99FF0000);
                text = "CAF SCAN";
            }
            if (mAfState == 2) { // passive good
                mPaint2.setColor(0x9999FF99);
                text = "CAF FOCUSED";
            }
            if (mAfState == 6) { // passive bad
                mPaint2.setColor(0x99FFFFFF);
                text = "CAF UNFOCUSED";
            }
            canvas.drawCircle(x2, y2, mLens * 0.25f * previewW, mPaint2);
            mPaint.setColor(0xFFFFFFFF);
            mPaint.setTextSize(36f);
            canvas.drawText(text, x2, y2 - mLens * 0.25f * previewW - 7f, mPaint);
        }

        // Draw Faces
        for (NormalizedFace face : mFaces) {
            RectF r1 = face.bounds;
            float newY = r1.centerX() * previewH;
            float newX = (1 - r1.centerY()) * previewW;
            float dY = r1.width() * previewH;
            float dX = r1.height() * previewW;
            float dP = (dX + dY) * 0.045f;
            RectF newR1 = new RectF(newX - dX * 0.5f, newY - dY * 0.5f, newX + dX * 0.5f, newY + dY * 0.5f);
            canvas.drawRoundRect(newR1, dP, dP, mPaint);

            PointF[] p = new PointF[3];
            p[0] = face.leftEye;
            p[1] = face.rightEye;
            p[2] = face.mouth;

            for (int j = 0; j < 3; j++) {
                if (p[j] == null) {
                    continue;
                }
                newY = p[j].x * previewH;
                newX = (1 - p[j].y) * previewW;
                canvas.drawCircle(newX, newY, dP, mPaint);
            }
        }

        // Draw Gyro grid.
        if (mShowGyroGrid) {
            float x1, x2, y1, y2;

            //
            //                    screen/sensor
            //                          |
            // screen/2 = FL tan(FOV/2) |
            //                          |                             lens
            //                          |<––––––––––––– FL –––––––––––>()–––––––––> scene @ infinity
            //                          |
            //                          |
            //                          |
            //

            float focalLengthH = 0.5f * previewH / (float) Math.tan(Math.toRadians(mFovLargeDegrees) * 0.5);
            float focalLengthW = 0.5f * previewW / (float) Math.tan(Math.toRadians(mFovSmallDegrees) * 0.5);
            final double ANGLE_STEP = (float) Math.toRadians(10f);

            // Draw horizontal lines, with 10 degree spacing.
            double phase1 = mAngles[0] % ANGLE_STEP;
            for (double i = -5 * ANGLE_STEP + phase1; i < 5 * ANGLE_STEP; i += ANGLE_STEP) {
                x1 = 0;
                x2 = previewW;
                y1 = y2 = previewH / 2 + focalLengthH * (float) Math.tan(i);
                canvas.drawLine(x1, y1, x2, y2, mPaint);
            }
            // Draw vertical lines, with 10 degree spacing.
            double phase2 = mAngles[1] % ANGLE_STEP;
            for (double i = -5 * ANGLE_STEP + phase2; i < 5 * ANGLE_STEP; i += ANGLE_STEP) {
                x1 = x2 = previewW / 2 + focalLengthW * (float) Math.tan(i);
                y1 = 0;
                y2 = previewH;
                canvas.drawLine(x1, y1, x2, y2, mPaint);
            }
        }

        super.onDraw(canvas);
    }
}


