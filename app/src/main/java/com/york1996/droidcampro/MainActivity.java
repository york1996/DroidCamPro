package com.york1996.droidcampro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.york1996.droidcampro.controller.CameraControllerImpl;
import com.york1996.droidcampro.ui.AutoFitTextureView;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button mBtnTakePhoto;
    private Button mBtnSwitchCam;
    private Button mBtnToGallery;
    private AutoFitTextureView mTextureViewPreview;
    private CameraControllerImpl mCameraController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
        }

        mBtnTakePhoto = findViewById(R.id.btn_take_photo);
        mBtnSwitchCam = findViewById(R.id.btn_switch_cam);
        mBtnSwitchCam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCameraController != null) {
                    mCameraController.switchCamera();
                }
            }
        });
        mBtnToGallery = findViewById(R.id.btn_to_gallery);
        mTextureViewPreview = findViewById(R.id.texture_view_preview);
        mCameraController = new CameraControllerImpl(this, mTextureViewPreview);
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
}