package com.mikiller.zxinglib;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.security.spec.EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Mikiller on 2016/9/6.
 */
public class ZxingUtil {

    public static Bitmap createQRCode(String content, Bitmap.Config config, BarcodeFormat fomat, int width, int height){
        if(TextUtils.isEmpty(content))
            return null;

        Object pixes;
        Map<EncodeHintType, Object> hint = new HashMap<>();
        hint.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hint.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hint.put(EncodeHintType.MARGIN, 1);
        try {
            Bitmap bmp = Bitmap.createBitmap(width, height, config);
            BitMatrix matrix = new MultiFormatWriter().encode(content, fomat, width, height, hint);
            if(config == Bitmap.Config.ALPHA_8)
                pixes = ByteBuffer.allocate(width * height);
            else
                pixes = new int[width * height];
            for(int x = 0; x < width; ++x) {
                for(int y = 0; y < height; ++y) {
                    if(config == Bitmap.Config.ALPHA_8)
                        ((ByteBuffer)pixes).put(y * width + x, (byte)(matrix.get(x, y)?0:255));
                    else
                        ((int[])pixes)[y * width + x] = matrix.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            if(config == Bitmap.Config.ALPHA_8)
                bmp.copyPixelsFromBuffer((ByteBuffer) pixes);
            else
                bmp.setPixels((int[])pixes, 0, width, 0, 0, width, height);
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Bitmap createQRCode(String content, Bitmap.Config config, int width, int height, Bitmap logo){
        Bitmap bmp = createQRCode(content, config, BarcodeFormat.QR_CODE, width, height);
        if(bmp != null)
            bmp = addLogo(bmp ,logo);
            return bmp;
    }

    public static Bitmap addLogo(Bitmap src, Bitmap logo){
        if(src == null || logo == null)
            return src;

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int logoWidth = logo.getWidth();
        int logoHeight = logo.getHeight();

        if(srcWidth == 0 || srcHeight == 0 || logoWidth == 0 || logoHeight == 0)
            return src;

        float scaleSize = srcWidth / 4.0f / logoWidth;

        Bitmap bmp = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.RGB_565);
        try{
            Canvas canvas = new Canvas(bmp);
            canvas.drawBitmap(src, 0,0,null);
            canvas.scale(scaleSize, scaleSize, srcWidth / 2, srcHeight / 2);
            canvas.drawBitmap(logo, (srcWidth - logoWidth) / 2, (srcHeight - logoHeight) / 2, null);
            canvas.save();
            canvas.restore();
        }catch (Exception e){
            bmp = null;
        }
        return bmp;
    }

    public static void showQRCode(final Activity context, final int size, final ImageView imageView, final String content, final Bitmap.Config config, final Bitmap bitmap){
        Executors.newFixedThreadPool(4).execute(new Runnable() {
            @Override
            public void run() {
//                BitmapDrawable drawable = (BitmapDrawable) mContext.getResources().getDrawable(R.mipmap.ic_launcher);

//                int size = DisplayUtil.getScreenWidth(mContext) / 2 - DisplayUtil.dip2px(mContext, 5);
                final Bitmap bmp = ZxingUtil.createQRCode(content, config, size, size, bitmap);
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bmp);
                    }
                });

            }
        });
    }

    public static void showQRCode(final Activity context, final ImageView imageView, final Bitmap bitmap, final Bitmap logo){
        Executors.newFixedThreadPool(4).execute(new Runnable() {
            @Override
            public void run() {
//                BitmapDrawable drawable = (BitmapDrawable) mContext.getResources().getDrawable(R.mipmap.ic_launcher);

//                int size = DisplayUtil.getScreenWidth(mContext) / 2 - DisplayUtil.dip2px(mContext, 5);
                final Bitmap bmp = ZxingUtil.addLogo(bitmap, logo);
                context.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bmp);
                    }
                });

            }
        });
    }
}
