/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mikiller.zxinglib;

import com.google.zxing.ResultPoint;
import com.mikiller.zxinglib.camera.CameraManager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class ViewfinderView extends View {

    private static final int[] SCANNER_ALPHA = {0, 64, 128, 192, 255, 192, 128, 64};
    private static final long ANIMATION_DELAY = 30L;
    private static final int CURRENT_POINT_OPACITY = 0xA0;
    private static final int MAX_RESULT_POINTS = 20;
    private static final int POINT_SIZE = 10;
    private static final int LEFT_TOP = 0;
    private static final int RIGHT_TOP = 1;
    private static final int LEFT_BOTTOM = 2;
    private static final int RIGHT_BOTTOM = 3;
    private static final int CORNOR_WIDTH = 5;
    private static final int CORNOR_LENGHT = 60;

    private int LASER_SCANER = 0;
    private static boolean moveDown = true;

    private CameraManager cameraManager;
    Rect frame;
    Rect previewFrame;
    private final Paint paint;
    private TextPaint txtPaint;
    private Bitmap resultBitmap;
    private int maskColor;
    private int resultColor;
    private int laserColor;
    private List<Path> cornorPaths;
    private List<ResultPoint> possibleResultPoints;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        txtPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        txtPaint.setTextSize(32f);
        txtPaint.setColor(getResources().getColor(R.color.result_minor_text));
        txtPaint.setTextAlign(Paint.Align.CENTER);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);
        resultColor = resources.getColor(R.color.result_view);
        laserColor = resources.getColor(R.color.viewfinder_laser);
        possibleResultPoints = new ArrayList<>(5);
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
    frame = cameraManager.getFramingRect();
    previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        //画扫码框四周蒙版
        paint.setColor(resultBitmap != null ? resultColor : maskColor);
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);

        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(CURRENT_POINT_OPACITY);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            // Draw a red "laser scanner" line through the middle to show decoding is active
            //画扫码线
            paint.setColor(laserColor);
            drawLaser(canvas, frame);
            //画边框
            if(cornorPaths == null)
                initCornorPath();
            this.drawCorner(LEFT_TOP, canvas);
            this.drawCorner(RIGHT_TOP, canvas);
            this.drawCorner(RIGHT_BOTTOM, canvas);
            this.drawCorner(LEFT_BOTTOM, canvas);
            //画hint
            canvas.drawText("将二维码放入框内，即可自动扫描", canvas.getWidth() / 2, frame.bottom + 100, txtPaint);
            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            //设置扫描线更新频率
            postInvalidateDelayed(ANIMATION_DELAY,
                    frame.left + POINT_SIZE,
                    frame.top + POINT_SIZE,
                    frame.right - POINT_SIZE,
                    frame.bottom - POINT_SIZE);
        }
    }

    private void initCornorPath() {
        cornorPaths = new ArrayList<>();
        Path path = new Path();
        path.moveTo((float) (frame.left + CORNOR_WIDTH), (float) (frame.top + CORNOR_LENGHT));
        path.lineTo((float) (frame.left), (float) (frame.top + CORNOR_LENGHT));
        path.lineTo((float) (frame.left), (float) (frame.top));
        path.lineTo((float) (frame.left + CORNOR_LENGHT), (float) (frame.top));
        path.lineTo((float) (frame.left + CORNOR_LENGHT), (float) (frame.top + CORNOR_WIDTH));
        path.lineTo((float) (frame.left + CORNOR_WIDTH), (float) (frame.top + CORNOR_WIDTH));
        cornorPaths.add(path);
        path = new Path();
        path.moveTo((float) (frame.right - CORNOR_WIDTH), (float) (frame.top + CORNOR_LENGHT));
        path.lineTo((float) (frame.right), (float) (frame.top + CORNOR_LENGHT));
        path.lineTo((float) (frame.right), (float) (frame.top));
        path.lineTo((float) (frame.right - CORNOR_LENGHT), (float) (frame.top));
        path.lineTo((float) (frame.right - CORNOR_LENGHT), (float) (frame.top + CORNOR_WIDTH));
        path.lineTo((float) (frame.right - CORNOR_WIDTH), (float) (frame.top + CORNOR_WIDTH));
        cornorPaths.add(path);
        path = new Path();
        path.moveTo((float) (frame.left + CORNOR_WIDTH), (float) (frame.bottom - CORNOR_LENGHT));
        path.lineTo((float) (frame.left), (float) (frame.bottom - CORNOR_LENGHT));
        path.lineTo((float) (frame.left), (float) (frame.bottom));
        path.lineTo((float) (frame.left + CORNOR_LENGHT), (float) (frame.bottom));
        path.lineTo((float) (frame.left + CORNOR_LENGHT), (float) (frame.bottom - CORNOR_WIDTH));
        path.lineTo((float) (frame.left + CORNOR_WIDTH), (float) (frame.bottom - CORNOR_WIDTH));
        cornorPaths.add(path);
        path = new Path();
        path.moveTo((float) (frame.right - CORNOR_WIDTH), (float) (frame.bottom - CORNOR_LENGHT));
        path.lineTo((float) (frame.right), (float) (frame.bottom - CORNOR_LENGHT));
        path.lineTo((float) (frame.right), (float) (frame.bottom));
        path.lineTo((float) (frame.right - CORNOR_LENGHT), (float) (frame.bottom));
        path.lineTo((float) (frame.right - CORNOR_LENGHT), (float) (frame.bottom - CORNOR_WIDTH));
        path.lineTo((float) (frame.right - CORNOR_WIDTH), (float) (frame.bottom - CORNOR_WIDTH));
        cornorPaths.add(path);
    }

    private void drawCorner(int dir, Canvas canvas) {
        canvas.drawPath(cornorPaths.get(dir), this.paint);
    }

    private void drawLaser(Canvas canvas, Rect frame) {
        float top = (float) (this.getLaserScaner(frame.height() - 10) % (frame.height()) + frame.top);
        canvas.drawOval(new RectF((float) (frame.left + 10), top, (float) (frame.right - 10), top + 10), this.paint);
    }

    private int getLaserScaner(int max) {
        if (moveDown) {
            LASER_SCANER += 10;
        } else {
            LASER_SCANER -= 10;
        }

        if (LASER_SCANER > max) {
            moveDown = false;
        } else if (LASER_SCANER <= 0) {
            moveDown = true;
        }

        return LASER_SCANER;
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

}
