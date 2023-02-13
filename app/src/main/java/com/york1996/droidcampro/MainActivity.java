package com.york1996.droidcampro;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.camera2.CaptureRequest;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Range;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
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
    private Button mBtnAEReset;
    private SeekBar mSeekBarCompensation;
    private SeekBar mSeekBarExposureTime;
    private SeekBar mSeekBarISO;
    private boolean mManualExposure;

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
            if (mCameraController == null) {
                return;
            }
            mCameraController.takePhoto();
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
        mBtnAEReset = findViewById(R.id.btn_ae);
        mBtnAEReset.setOnClickListener(v -> {
            if (mCameraController == null) {
                return;
            }
            mManualExposure = false;
            mCameraController.setAutoExposure(true);
        });
        mSeekBarCompensation = findViewById(R.id.seekbar_ae_compensation);
        mSeekBarExposureTime = findViewById(R.id.seekbar_exposure_time);
        mSeekBarISO = findViewById(R.id.seekbar_iso);

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
        if (mCameraController == null) {
            return;
        }
        mCameraController.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraController == null) {
            return;
        }
        mCameraController.stop();
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
        if (mCameraController == null) {
            return;
        }
        Range<Integer> aeCompensationRange = mCameraController.getAutoExposureCompensationRange();
        mSeekBarCompensation.setMax(aeCompensationRange.getUpper());
        mSeekBarCompensation.setMin(aeCompensationRange.getLower());
        mSeekBarCompensation.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mCameraController == null) {
                    return;
                }
                mCameraController.setAutoExposureCompensationStep(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                mManualExposure = false;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        Range<Integer> isoRange = mCameraController.getISORange();
        mSeekBarISO.setMax(isoRange.getUpper());
        mSeekBarISO.setMin(isoRange.getLower());
        mSeekBarISO.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mCameraController == null || !mManualExposure) {
                    return;
                }
                mCameraController.setISO(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onTrackManual();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        Range<Long> exposureTimeRange = mCameraController.getExposureTimeRange();
        mSeekBarExposureTime.setMin(exposureTimeRange.getLower().intValue());
        mSeekBarExposureTime.setMax(100000000);
        mSeekBarExposureTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mCameraController == null || !mManualExposure) {
                    return;
                }
                mCameraController.setExposureTime(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                onTrackManual();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    @Override
    public void onCameraStopped() {
        Log.d(TAG, "onCameraStopped");
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
    public void onCameraParamsChanged(CameraParams params) {
        Log.d(TAG, "onCameraParamsChanged");
    }

    @Override
    public void onError(int errorCode) {
        Log.e(TAG, "onError");
    }

    public void onAutoFocusClicked(View view) {
        if (mCameraController == null) {
            return;
        }
        mCameraController.setFocusMode(CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
    }

    public void onAutoFocusSingleClicked(View view) {
        if (mCameraController == null) {
            return;
        }
        mCameraController.setFocusMode(CaptureRequest.CONTROL_AF_MODE_AUTO);
    }

    public void onFocusManualClicked(View view) {
        if (mCameraController == null) {
            return;
        }
        mCameraController.setFocusMode(CaptureRequest.CONTROL_AF_MODE_OFF);
        SeekBar skb = findViewById(R.id.seekbar_focus_distance);
        float minFocusDistance = mCameraController.getMiniFocusDistance();
        float maxFocusDistance = mCameraController.getMaxFocusDistance();

        skb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float focusDistance = minFocusDistance + (maxFocusDistance - minFocusDistance) * progress / 100;
                mCameraController.setFocusDistance(focusDistance);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void onWBAutoClicked(View view) {
        if (mCameraController == null) {
            return;
        }
        mCameraController.setWhiteBalanceMode(CaptureRequest.CONTROL_AWB_MODE_AUTO);
    }

    public void onWBIncandescentClicked(View view) {
        if (mCameraController == null) {
            return;
        }
        mCameraController.setWhiteBalanceMode(CaptureRequest.CONTROL_AWB_MODE_INCANDESCENT);
    }

    public void onWBFluorescentClicked(View view) {
        if (mCameraController == null) {
            return;
        }
        mCameraController.setWhiteBalanceMode(CaptureRequest.CONTROL_AWB_MODE_FLUORESCENT);
    }

    public void onWBCloudClicked(View view) {
        if (mCameraController == null) {
            return;
        }
        mCameraController.setWhiteBalanceMode(CaptureRequest.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
    }

    public void onWBDayLightClicked(View view) {
        if (mCameraController == null) {
            return;
        }
        mCameraController.setWhiteBalanceMode(CaptureRequest.CONTROL_AWB_MODE_DAYLIGHT);
    }

    private void onTrackManual() {
        mManualExposure = true;
    }
}