package com.google.snappy;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Log;
import android.util.Size;

/**
 * Caches (static) information about the first/main camera.
 * Convenience functions represent data from CameraCharacteristics.
 */

public class CameraInfoCache {
    private static final String TAG = "SNAPPY_CAMINFO";

    public static final boolean IS_NEXUS_5 = "hammerhead".equalsIgnoreCase(Build.DEVICE);
    public static final boolean IS_NEXUS_6 = "shamu".equalsIgnoreCase(Build.DEVICE);
    public static final boolean IS_NEXUS_9 = "flounder".equalsIgnoreCase(Build.DEVICE);
    public static final boolean IS_ANGLER = "angler".equalsIgnoreCase(Build.DEVICE);
    public static final boolean IS_BULLHEAD = "bullhead".equalsIgnoreCase(Build.DEVICE);
    public static final boolean IS_SAMSUNG_S6 = "zerofltevzw".equalsIgnoreCase(Build.DEVICE);
    public static final boolean IS_LG_G4 = "p1_lgu_kr".equalsIgnoreCase(Build.PRODUCT);

    public int[] noiseModes;
    public int[] edgeModes;

    private CameraCharacteristics mCameraCharacteristics;
    private String mCameraId;
    private Size mLargestYuvSize;
    private Size mLargestJpegSize;
    private Size mRawSize;
    private Rect mActiveArea;
    private Integer mSensorOrientation;
    private Integer mRawFormat;
    private int mBestFaceMode;
    private boolean mCamera2FullModeAvailable;

    /**
     * Constructor.
     */
    public CameraInfoCache(CameraManager cameraMgr, boolean useFrontCamera) {
        String[] cameralist;
        try {
            cameralist = cameraMgr.getCameraIdList();
            for (String id : cameralist) {
                mCameraCharacteristics = cameraMgr.getCameraCharacteristics(id);
                Integer facing = mCameraCharacteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing == (useFrontCamera ? CameraMetadata.LENS_FACING_FRONT : CameraMetadata.LENS_FACING_BACK)) {
                    mCameraId = id;
                    break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "ERROR: Could not get camera ID list / no camera information is available: " + e);
            return;
        }
        // Should have mCameraId as this point.
        if (mCameraId == null) {
            Log.e(TAG, "ERROR: Could not find a suitable rear or front camera.");
            return;
        }

        // Store YUV_420_888, JPEG, Raw info
        StreamConfigurationMap map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
        int[] formats = map.getOutputFormats();
        long lowestStall = Long.MAX_VALUE;
        for (int i = 0; i < formats.length; i++) {
            if (formats[i] == ImageFormat.YUV_420_888) {
                mLargestYuvSize = returnLargestSize(map.getOutputSizes(formats[i]));
            }
            if (formats[i] == ImageFormat.JPEG) {
                mLargestJpegSize = returnLargestSize(map.getOutputSizes(formats[i]));
            }
            if (formats[i] == ImageFormat.RAW10 || formats[i] == ImageFormat.RAW_SENSOR) { // TODO: Add RAW12
                Size size = returnLargestSize(map.getOutputSizes(formats[i]));
                long stall = map.getOutputStallDuration(formats[i], size);
                if (stall < lowestStall) {
                    mRawFormat = formats[i];
                    mRawSize = size;
                    lowestStall = stall;
                }
            }
        }

        mActiveArea = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);

        // Compute best face mode.
        int[] faceModes = mCameraCharacteristics.get(CameraCharacteristics.STATISTICS_INFO_AVAILABLE_FACE_DETECT_MODES);
        for (int i=0; i<faceModes.length; i++) {
            if (faceModes[i] > mBestFaceMode) {
                mBestFaceMode = faceModes[i];
            }
        }
        edgeModes = mCameraCharacteristics.get(CameraCharacteristics.EDGE_AVAILABLE_EDGE_MODES);
        noiseModes = mCameraCharacteristics.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);

        // Misc stuff.
        mCamera2FullModeAvailable = mCameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
                CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL;

        mSensorOrientation = mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
    }

    public int sensorOrientation() {
        return mSensorOrientation;
    }

    public boolean isCamera2FullModeAvailable() {
        return mCamera2FullModeAvailable;
    }

    public float getDiopterLow() {
        if (IS_NEXUS_6) {
            return 0f;
        }
        return 0f; // Infinity
    }

    public float getDiopterHi() {
        if (IS_NEXUS_6) {
            return 14.29f;
        }
        return 16f;
    }

    /**
     * Private utility function.
     */
    private Size returnLargestSize(Size[] sizes) {
        Size largestSize = null;
        int area = 0;
        for (int j = 0; j < sizes.length; j++) {
            if (sizes[j].getHeight() * sizes[j].getWidth() > area) {
                area = sizes[j].getHeight() * sizes[j].getWidth();
                largestSize = sizes[j];
            }
        }
        return largestSize;
    }

    public int bestFaceDetectionMode() {
        return mBestFaceMode;
    }

    public int faceOffsetX() {
        return (mActiveArea.width() - mLargestYuvSize.getWidth()) / 2;
    }

    public int faceOffsetY() {
        return (mActiveArea.height() - mLargestYuvSize.getHeight()) / 2;
    }

    public int activeAreaWidth() {
        return mActiveArea.width();
    }

    public int activeAreaHeight() {
        return mActiveArea.height();
    }

    public Rect getActiveAreaRect() {
        return mActiveArea;
    }

    public String getCameraId() {
        return mCameraId;
    }

    public Size getPreviewSize() {
        float aspect = mLargestYuvSize.getWidth() / mLargestYuvSize.getHeight();
        aspect = aspect > 1f ? aspect : 1f / aspect;
        if (aspect > 1.6) {
            return new Size(1920, 1080); // TODO: Check available resolutions.
        }
        if (IS_ANGLER || IS_BULLHEAD) {
            return new Size(1440, 1080);
        }
        return new Size(1280, 960); // TODO: Check available resolutions.
    }

    public Size getJpegStreamSize() {
        return mLargestJpegSize;
    }

    public Size getYuvStream1Size() {
        return mLargestYuvSize;
    }

    public Size getYuvStream2Size() {
        return new Size(320, 240);
    }

    public boolean rawAvailable() {
        return mRawSize != null;
    }
    public boolean reprocessingAvailable() {
        // TODO: Actually query capabilities list.
        return (IS_ANGLER || IS_BULLHEAD);
    }

    public Integer getRawFormat() {
        return mRawFormat;
    }

    public Size getRawStreamSize() {
        return mRawSize;
    }

}
