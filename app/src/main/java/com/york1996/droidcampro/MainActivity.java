package com.york1996.droidcampro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import com.york1996.droidcampro.callback.CameraControlCallback;
import com.york1996.droidcampro.controller.CameraController;
import com.york1996.droidcampro.controller.GalleryController;
import com.york1996.droidcampro.model.CameraParams;
import com.york1996.droidcampro.ui.AutoFitTextureView;

public class MainActivity extends AppCompatActivity implements CameraControlCallback {
    private static final String TAG = "MainActivity";

    private Button mBtnTakePhoto;
    private Button mBtnSwitchCam;
    private Button mBtnToGallery;
    private AutoFitTextureView mTextureViewPreview;
    private CameraController mCameraController;
    private GalleryController mGalleryController;
    private Uri mLastCapturePhoto;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        }

        mBtnTakePhoto = findViewById(R.id.btn_take_photo);
        mBtnTakePhoto.setOnClickListener(v -> {
            if (mCameraController != null) {
                mCameraController.takePhoto();
            }
        });
        mBtnSwitchCam = findViewById(R.id.btn_switch_cam);
        mBtnSwitchCam.setOnClickListener(v -> {
            if (mCameraController != null) {
                mCameraController.switchCamera();
            }
        });
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
        mTextureViewPreview = findViewById(R.id.texture_view_preview);
        mCameraController = new CameraController.Builder()
                .setContext(this)
                .setAutoFitTextureView(mTextureViewPreview)
                .setCameraControlCallback(this)
                .build();
        mGalleryController = new GalleryController(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mCameraController.start();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraController.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    mCameraController.start();
                }
            }
        }
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