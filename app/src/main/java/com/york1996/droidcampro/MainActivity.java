package com.york1996.droidcampro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.york1996.procam.callback.CameraControlCallback;
import com.york1996.procam.CameraController;
import com.york1996.procam.controller.GalleryController;
import com.york1996.procam.model.CameraParams;
import com.york1996.procam.ui.AutoFitTextureView;


public class MainActivity extends AppCompatActivity implements CameraControlCallback {
    private static final String TAG = "MainActivity";

    private Button mBtnTakePhoto;
    private Button mBtnToGallery;
    private Button mBtnAELock;
    private boolean mCurrentLockAE;
    private Button mBtnAWBLock;
    private boolean mCurrentLockAWB;
    private AutoFitTextureView mTextureViewPreview;
    private CameraController mCameraController;
    private GalleryController mGalleryController;
    private Uri mLastCapturePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        } else {
            init();
        }
    }

    private void init() {
        mTextureViewPreview = findViewById(R.id.texture_view_preview);
        mCameraController = new CameraController.Builder()
                .setContext(this)
                .setAutoFitTextureView(mTextureViewPreview)
                .setCameraControlCallback(this)
                .build();
        mBtnTakePhoto = findViewById(R.id.btn_take_photo);
        mBtnTakePhoto.setOnClickListener(v -> {
            if (mCameraController != null) {
                mCameraController.takePhoto();
            }
        });
        mBtnAWBLock = findViewById(R.id.btn_awb_lock);
        mBtnAWBLock.setOnClickListener(v -> {
            if (mCameraController == null) {
                return;
            }
            mCameraController.setAutoWhiteBalanceLock(!mCurrentLockAWB);
            mCurrentLockAWB = !mCurrentLockAWB;
            mBtnAWBLock.setText(mCurrentLockAWB ? "awb: lock" : "awb: unlock");
        });
        mBtnAELock = findViewById(R.id.btn_ae_lock);
        mBtnAELock.setOnClickListener(v -> {
            if (mCameraController == null) {
                return;
            }
            mCameraController.setAutoExposureLock(!mCurrentLockAE);
            mCurrentLockAE = !mCurrentLockAE;
            mBtnAELock.setText(mCurrentLockAE ? "曝光锁定：开" : "曝光锁定：关");
        });

        mGalleryController = new GalleryController(this);
        mBtnToGallery = findViewById(R.id.btn_to_gallery);
        mBtnToGallery.setOnClickListener(v -> {
            if (mGalleryController == null) {
                return;
            }
            if (mLastCapturePhoto != null) {
                mGalleryController.jumpToViewPicture(mLastCapturePhoto);
            } else {
                mGalleryController.jumpToSystemGallery();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mCameraController != null) {
            mCameraController.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraController != null) {
            mCameraController.stop();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    init();
                }
            }
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "onConfigurationChanged");
    }

    @Override
    public void onCameraStarted(boolean front) {
        Log.d(TAG, "onCameraStarted = " + front);
    }

    @Override
    public void onCameraStopped() {
        Log.d(TAG, "onCameraStopped");
    }

    @Override
    public void onCameraParamsChanged(CameraParams params) {
        Log.d(TAG, "onCameraParamsChanged");
    }

    @Override
    public void onCameraCapture(byte[] photoBytes) {
        Log.d(TAG, "onCameraCapture");
        if (mGalleryController == null) {
            return;
        }
        mLastCapturePhoto = mGalleryController.savePictureToGallery(photoBytes);
        Toast.makeText(this, "相片已保存", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(int errorCode) {
        Log.e(TAG, "onError");
    }
}