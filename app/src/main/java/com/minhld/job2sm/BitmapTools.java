package com.minhld.job2sm;

import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by minhld on 01/28/2016.
 */
public class BitmapTools {
    public static byte[] getBytesFromBitmap(Bitmap bmp) {
        ByteArrayOutputStream bos =  null;

        try {
            // assign the binary data
            bos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 0, bos);
            return bos.toByteArray();
        } catch (Exception e) {
            // error goes here
            // return an empty binary array
            return new byte[0];
        } finally {
            try {
                bos.close();
            } catch (IOException e2) { }
        }
    }

    public static void writeBitmapToFile(String outputBitmapPath, Bitmap bmp,
                                         boolean releaseBitmap) throws IOException {
        File file = new File(outputBitmapPath);
        FileOutputStream fOut = new FileOutputStream(file);

        bmp.compress(Bitmap.CompressFormat.PNG, 100, fOut);
        fOut.flush();
        fOut.close();

        if (releaseBitmap) {
            bmp.recycle();
            System.gc();
        }
    }

    /**
     * create a scaled down bitmap with new width & height
     * but maintain the image ratio
     *
     * @param src
     * @param width
     * @return
     */
    public static Bitmap createScaleImage(Bitmap src, int width) {
        int height = (width * src.getHeight()) / src.getWidth();
        return Bitmap.createScaledBitmap(src, width, height, true);
    }

    /**
     * calculate the sample size
     *
     * @param bmp
     * @param resizedWidth
     * @return
     */
    public static int calculateInSampleSize(Bitmap bmp, int resizedWidth) {
        final int width = bmp.getWidth();
        final int height = bmp.getHeight();

        final int resizedHeight = (resizedWidth * height) / width;
        int inSampleSize = 1;

        if (height > resizedHeight || width > resizedWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;


            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > resizedHeight &&
                    (halfWidth / inSampleSize) > resizedWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
