package com.mikiller.zxinglib;

import android.content.Intent;
import android.graphics.Bitmap;

public interface DecodeCallback {
    void restartPreview();
    void decodeSuccessed(String rst, String codeFormat, Bitmap barcode, float scaleFactor);
    void decodeFailed();
    void returnScanResult(Intent intent);
}
