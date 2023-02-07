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
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.text.TextUtils;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class CameraController {
    private static final String TAG = "CameraController";

    private final Context mContext;
    private final CameraManager mCameraManager;
    private final TextureView mTextureViewPreview;

    private HandlerThread mCameraThread;
    private Handler mCameraHandler;
    // 当前使用的摄像头ID
    private String mCameraId;
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

    public CameraController(Context context, TextureView previewTextureView) {
        mContext = context;
        mTextureViewPreview = previewTextureView;
        mCameraManager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
    }

    public void start() {
        // 相机线程及其handler
        mCameraThread = new HandlerThread("CameraThread");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        // 注册
        if (mTextureViewPreview.isAvailable()) {
            Log.i(TAG, "preview already available");
            return;
        }
        mTextureViewPreview.setSurfaceTextureListener(mTextureListener);
    }

    public void stop() {
        mCameraThread.quitSafely();
    }

    private void initCamera(int width, int height) {
        try {
            for (String cameraId : mCameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT && mCameraId == null) {
                    continue;
                }
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                if (map == null) {
                    Log.e(TAG, "can't find SCALER_STREAM_CONFIGURATION_MAP, cameraId = " + cameraId);
                    continue;
                }
                mPreviewSize = getOptimalSize(map.getOutputSizes(SurfaceTexture.class), width, height);
                mCaptureSize = Collections.max(Arrays.asList(map.getOutputSizes(ImageFormat.JPEG)),
                        (lhs, rhs) -> Long.signum((long) lhs.getWidth() * lhs.getHeight() - (long) rhs.getHeight() * rhs.getWidth()));
                mImageReader = ImageReader.newInstance(mCaptureSize.getWidth(), mCaptureSize.getHeight(),
                        ImageFormat.JPEG, 2);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mCameraHandler);
                mCameraId = cameraId;
                break;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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

    private final ImageReader.OnImageAvailableListener mOnImageAvailableListener = new ImageReader.OnImageAvailableListener() {
        @Override
        public void onImageAvailable(ImageReader reader) {

        }
    };

    private void openCamera() {
        try {
            if (ActivityCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "can't open camera, no permission");
                return;
            }
            mCameraManager.openCamera(mCameraId, mStateCallback, mCameraHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
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
                    try {
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
            initCamera(width, height);
            openCamera();
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
            Log.d(TAG, "onSurfaceTextureUpdated");
        }
    };
}
