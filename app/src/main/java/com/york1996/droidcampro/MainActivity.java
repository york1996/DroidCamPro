package com.york1996.droidcampro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.TextureView;
import android.widget.Button;

import com.york1996.droidcampro.controller.CameraController;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button mBtnTakePhoto;
    private TextureView mTextureViewPreview;
    private CameraController mCameraController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnTakePhoto = findViewById(R.id.btn_take_photo);
        mTextureViewPreview = findViewById(R.id.texture_view_preview);
        mCameraController = new CameraController(this, mTextureViewPreview);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 1);
            return;
        }
        mCameraController.start();
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
                } else {
                    finish();
                }
            }
        }
    }
}