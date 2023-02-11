package com.york1996.droidcampro.controller;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.york1996.droidcampro.callback.CameraControlCallback;
import com.york1996.droidcampro.controller.CameraController;
import com.york1996.droidcampro.ui.AutoFitTextureView;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CameraControllerImpl extends CameraController {
    private static final String TAG = "CameraController";

    private static final SparseIntArray ORIENTATION = new SparseIntArray();

    static {
        ORIENTATION.append(Surface.ROTATION_0, 90);
        ORIENTATION.append(Surface.ROTATION_90, 0);
        ORIENTATION.append(Surface.ROTATION_180, 270);
        ORIENTATION.append(Surface.ROTATION_270, 180);
    }

    private final Context mContext;
    private final CameraManager mCameraManager;
    private final AutoFitTextureView mTextureViewPreview;
    private final CameraControlCallback mCallback;
    private final Handler mMainHandler;

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    private int mTextureWidth;
    private int mTextureHeight;

    // 当前使用的摄像头ID
    private String mCameraId;
    // 当前方向
    private boolean mFrontCam;
    // 预览大小
    private Size mPreviewSize;
    // 照片大小
    private Size mCaptureSize;
    // 获取预览数据
    private ImageReader mImageReader;
    // 相机控制
    private CameraDevice mCameraDevice;
    // 拍照参数
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CaptureRequest mCaptureRequest;
    private CameraCaptureSession mCameraCaptureSession;

    CameraControllerImpl(Context context, AutoFitTextureView previewTextureView, CameraControlCallback callback) {
        mContext = context;
        mMainHandler = new Handler(Looper.getMainLooper());
        mTextureViewPreview = previewTextureView;
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        mCallback = callback;
    }

    @Override
    public void start() {
        Log.d(TAG, "start");
        // 相机线程及其handler
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        // 注册
        if (mTextureViewPreview.isAvailable()) {
            Log.i(TAG, "preview already available");
            openCameraAndPreview(mTextureWidth, mTextureHeight, mFrontCam);
            return;
        }
        mTextureViewPreview.setSurfaceTextureListener(mTextureListener);
    }

    @Override
    public void stop() {
        stopCameraAndPreview();
        if (mCameraThread != null) {
            mCameraThread.quitSafely();
            mCameraThread = null;
        }
    }

    @Override
    public void takePhoto() {
        try {
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            int rotation = mContext.getResources().getConfiguration().orientation;
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION, ORIENTATION.get(rotation));
            mCameraCaptureSession.capture(mCaptureRequestBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    mCaptureRequestBuilder.removeTarget(mImageReader.getSurface());
                    Log.d(TAG, "takePhoto: onCaptureCompleted");
                }
            }, null);
        } catch (CameraAccessException e) {
            Log.e(TAG, "takePhoto: " + e);
        }
    }

    @Override
    public void switchCamera() {
        stopCameraAndPreview();
        openCameraAndPreview(mTextureWidth, mTextureHeight, !mFrontCam);
    }

    @Override
    public boolean supportSwitchCamera() {
        try {
            return mCameraManager.getCameraIdList().length > 1;
        } catch (CameraAccessException e) {
            Log.e(TAG, "supportSwitchCamera: " + e);
        }
        return false;
    }

    @Override
    public void setAutoExposure(boolean autoExposure) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
        try {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            mCaptureRequest = mCaptureRequestBuilder.build();
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setAutoExposure: ", e);
        }
    }

    @Override
    public Range<Integer> getAutoExposureCompensationRange() {
        try {
            CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            return characteristics.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getAutoExposureCompensationRange: ", e);
        }
        return null;
    }

    @Override
    public void setAutoExposureCompensationStep(int value) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
        try {
            Range<Integer> exposureCompensationRange = getAutoExposureCompensationRange();
            value = Math.max(exposureCompensationRange.getLower(), Math.min(exposureCompensationRange.getUpper(), value));
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, value);
            mCaptureRequest = mCaptureRequestBuilder.build();
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setAutoExposureCompensationStep: ", e);
        }
    }

    @Override
    public void setAutoExposureLock(boolean lock) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
        try {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, lock);
            mCaptureRequest = mCaptureRequestBuilder.build();
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setAutoExposureLock: ", e);
        }
    }

    @Override
    public void setAutoExposureArea(MeteringRectangle[] rectangles) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
        try {
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, rectangles);
            mCaptureRequest = mCaptureRequestBuilder.build();
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setAutoExposureArea: ", e);
        }
    }

    @Override
    public Range<Long> getExposureTimeRange() {
        try {
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            return cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getExposureTimeRange: ", e);
        }
        return null;
    }

    @Override
    public void setExposureTime(long value) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
        try {
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            int[] availableModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
            boolean isManualModeSupported = false;
            for (int mode : availableModes) {
                if (mode == CameraMetadata.CONTROL_AE_MODE_OFF) {
                    isManualModeSupported = true;
                    break;
                }
            }
            if (!isManualModeSupported) {
                Log.e(TAG, "Manual mode not supported");
                return;
            }
            Range<Long> exposureTimeRange = getExposureTimeRange();
            value = Math.max(exposureTimeRange.getLower(), Math.min(exposureTimeRange.getUpper(), value));
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
            mCaptureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, value);
            mCaptureRequest = mCaptureRequestBuilder.build();
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setExposureTime: ", e);
        }
    }

    @Override
    public Range<Integer> getISORange() {
        try {
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            return cameraCharacteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
        } catch (CameraAccessException e) {
            Log.e(TAG, "getISORange: ", e);
        }
        return null;
    }

    @Override
    public void setISO(int value) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
        try {
            CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics(mCameraId);
            int[] availableModes = cameraCharacteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_MODES);
            boolean isManualModeSupported = false;
            for (int mode : availableModes) {
                if (mode == CameraMetadata.CONTROL_AE_MODE_OFF) {
                    isManualModeSupported = true;
                    break;
                }
            }
            if (!isManualModeSupported) {
                Log.e(TAG, "Manual mode not supported");
                return;
            }
            Range<Integer> range = getISORange();
            value = Math.max(range.getLower(), Math.min(range.getUpper(), value));
            mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF);
            mCaptureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, value);
            mCaptureRequest = mCaptureRequestBuilder.build();
            mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "setISO: ", e);
        }
    }

    @Override
    public Range<Integer> getWhiteBalanceRegionRange() {
        return null;
    }

    @Override
    public void setWhiteBalanceRegion(int value) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
    }

    @Override
    public void setAutoWhitBalance(boolean auto) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
    }

    @Override
    public void setAutoWhiteBalanceLock(boolean lock) {

    }

    @Override
    public int[] getFocusModes() {
        return new int[0];
    }

    @Override
    public void setFocusMode(int mode) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
    }

    @Override
    public void setFocusArea(MeteringRectangle rectangle) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
    }

    @Override
    public Range<Integer> getFocusDistanceRange() {
        return null;
    }

    @Override
    public void setFocusDistance(int distance) {
        if (mCaptureRequestBuilder == null || mCameraCaptureSession == null) {
            return;
        }
    }

    private void openCameraAndPreview(int width, int height, boolean front) {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK && front) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    Log.e(TAG, "can't find SCALER_STREAM_CONFIGURATION_MAP, cameraId = " + cameraId);
                    continue;
                }
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                if (mPreviewSize.getHeight() < mPreviewSize.getWidth()) {
                    mTextureViewPreview.setAspectRatio(mPreviewSize.getHeight(), mPreviewSize.getWidth());
                } else {
                    mTextureViewPreview.setAspectRatio(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                }
                mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        (lhs, rhs) -> Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getHeight() * rhs.getWidth()));
                mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(),
                        ImageFormat.JPEG, 2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);
                mCameraId = cameraId;
                mFrontCam = front;
                break;
            }
            if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "can't open camera, no permission");
                return;
            }
            mCameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void stopCameraAndPreview() {
        mCameraId = null;
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
            mCameraCaptureSession = null;
        }
        if (mCameraDevice != null) {
            mCameraDevice.close();
            mCameraDevice = null;
        }
        if (mImageReader != null) {
            mImageReader.close();
            mImageReader = null;
        }
        mCaptureRequestBuilder = null;
        mCaptureRequest = null;
        mCallback.onCameraStopped();
    }

    private Size getOptimalSize(Size[] sizeMap, int width, int height) {
        List<Size> sizeList = new ArrayList<>();
        for (Size option : sizeMap) {
            if (width > height) {
                if (option.getWidth() > width && option.getHeight() > height) {
                    sizeList.add(option);
                }
            } else {
                if (option.getWidth() > height && option.getHeight() > width) {
                    sizeList.add(option);
                }
            }
        }
        if (sizeList.size() > 0) {
            return Collections.min(sizeList,
                    (lhs, rhs) -> Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getWidth() * rhs.getHeight()));
        }
        return sizeMap[0];
    }

    private void startPreview() {
        SurfaceTexture mSurfaceTexture = mTextureViewPreview.getSurfaceTexture();
        mSurfaceTexture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface previewSurface = new Surface(mSurfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);
            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(CameraCaptureSession session) {
                    Log.d(TAG, "CaptureSession: onConfigured");
                    try {
                        mCallback.onCameraStarted(mFrontCam);
                        mCaptureRequest = mCaptureRequestBuilder.build();
                        mCameraCaptureSession = session;
                        mCameraCaptureSession.setRepeatingRequest(mCaptureRequest, null, mCameraHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(CameraCaptureSession session) {

                }
            }, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {
            Image image = reader.acquireLatestImage();
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.capacity()];
            buffer.get(bytes);
            mMainHandler.post(() -> mCallback.onCameraCapture(bytes));
            image.close();
        }
    };

    private final CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(CameraDevice camera) {
            Log.d(TAG, "mStateCallback: onOpened");
            mCameraDevice = camera;
            startPreview();
        }

        @Override
        public void onDisconnected(CameraDevice camera) {
            Log.d(TAG, "mStateCallback: onDisconnected");
            camera.close();
            mCameraDevice = null;
        }

        @Override
        public void onError(CameraDevice camera, int error) {
            Log.e(TAG, "mStateCallback: onError");
            camera.close();
            mCameraDevice = null;
        }
    };

    private final TextureView.SurfaceTextureListener mTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureAvailable");
            mTextureWidth = width;
            mTextureHeight = height;
            openCameraAndPreview(width, height, false);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
            Log.d(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            Log.d(TAG, "onSurfaceTextureDestroyed");
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };
}
