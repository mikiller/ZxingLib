package com.mikiller.zxinglib;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.RelativeLayout;

import com.mikiller.zxinglib.camera.CameraManager;

import java.io.IOException;

import static android.app.Activity.RESULT_OK;

public class BarcodeScanView extends RelativeLayout {
    private SurfaceView sf_preview;
    private ViewfinderView scanView;
    private CameraManager cameraMgr;
    private CaptureActivityHandler captureHandler;
    public BarcodeScanView(Context context) {
        this(context, null, 0);
    }

    public BarcodeScanView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BarcodeScanView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs, defStyleAttr);
    }

    private void initView(Context context, AttributeSet attrs, int defStyleAttr){
        LayoutInflater.from(context).inflate(R.layout.layout_scan_view, this);
        sf_preview = findViewById(R.id.sf_preview);
        scanView = findViewById(R.id.scanView);
        sf_preview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                initCamera(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
        scanView.setCameraManager(cameraMgr= new CameraManager(getContext()));
    }

    private void initCamera(SurfaceHolder holder){
        try {
            cameraMgr.openDriver(holder);
            if(captureHandler == null){
                captureHandler = new CaptureActivityHandler(getContext(), Intents.Scan.QR_CODE_MODE, null, cameraMgr, new DecodeCallback() {
                    @Override
                    public void restartPreview() {
                        scanView.drawViewfinder();
                    }

                    @Override
                    public void decodeSuccessed(String rst, String codeFormat, Bitmap barcode, float scaleFactor) {
                        Log.e("main act", "rst: " + rst + ", " + codeFormat);
                    }

                    @Override
                    public void decodeFailed() {

                    }

                    @Override
                    public void returnScanResult(Intent intent) {
                        ((Activity)getContext()).setResult(RESULT_OK, intent);
                        ((Activity)getContext()).finish();
                    }
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
