package com.google.snappy;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.Image;

import java.nio.ByteBuffer;

/**
 * Some Bitmap utility functions.
 */
public class BitmapUtility {

    public static Bitmap bitmapFromJpeg(byte[] data) {
        // 32K buffer.
        byte[] decodeBuffer = new byte[32 * 1024]; // 32K buffer.

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 16; // 3264 / 16 = 204.
        opts.inTempStorage = decodeBuffer;
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, opts);

        return rotatedBitmap(b);
    }

    public static Bitmap bitmapFromYuvImage(Image img) {
        int w = img.getWidth();
        int h = img.getHeight();
        ByteBuffer buf0 = img.getPlanes()[0].getBuffer();
        int len = buf0.capacity();
        int[] colors = new int[len];
        int alpha = 255 << 24;
        int green;
        for (int i = 0; i < len; i++) {
            green = ((int) buf0.get(i)) & 255;
            colors[i] = green << 16 | green << 8 | green | alpha;
        }
        Bitmap b = Bitmap.createBitmap(colors, w, h, Bitmap.Config.ARGB_8888);

        return rotatedBitmap(b);
    }

    /**
     * Returns parameter bitmap rotated 90 degrees
     */
    private static Bitmap rotatedBitmap(Bitmap b) {
        Matrix mat = new Matrix();
        mat.postRotate(90);
        Bitmap b2 = Bitmap.createBitmap(b, 0, 0,b.getWidth(),b.getHeight(), mat, true);
        return b2;
    }

}
