package com.york1996.procam;

import android.content.Context;
import android.hardware.camera2.params.MeteringRectangle;
import android.util.Range;

import com.york1996.procam.callback.CameraControlCallback;
import com.york1996.procam.controller.CameraControllerImpl;
import com.york1996.procam.ui.AutoFitTextureView;

public abstract class CameraController {

    private static CameraController createController(Builder builder) {
        if (builder.context == null
                || builder.autoFitTextureView == null
                || builder.cameraControlCallback == null) {
            throw new IllegalArgumentException("all params could not be null.");
        }
        return new CameraControllerImpl(builder.context,
                builder.autoFitTextureView,
                builder.cameraControlCallback);
    }

    public static class Builder {
        private Context context;
        private AutoFitTextureView autoFitTextureView;
        private CameraControlCallback cameraControlCallback;

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public Builder setAutoFitTextureView(AutoFitTextureView textureView) {
            this.autoFitTextureView = textureView;
            return this;
        }

        public Builder setCameraControlCallback(CameraControlCallback callback) {
            this.cameraControlCallback = callback;
            return this;
        }

        public CameraController build() {
            return createController(this);
        }
    }

    /***
     * 开始摄影
     */
    public abstract void start();

    /**
     * 结束摄影
     */
    public abstract void stop();

    public abstract void takePhoto();

    /**
     * 切换摄像头
     */
    public abstract void switchCamera();

    /**
     * 是否支持切换
     *
     * @return 结果
     */
    public abstract boolean supportSwitchCamera();

    /**
     * 获取闪光灯模式
     *
     * @return 闪光灯模式
     */
    public abstract int[] getFlashMode();

    /**
     * 设置闪光灯模式
     * @param mode 模式
     */
    public abstract void setFlashMode(int mode);

    /**
     * 开启自动测光
     *
     * @param autoExposure 是否开启
     */
    public abstract void setAutoExposure(boolean autoExposure);

    /**
     * 获取曝光补偿范围
     *
     * @return 范围
     */
    public abstract Range<Integer> getAutoExposureCompensationRange();

    /**
     * 设置曝光补偿
     *
     * @param value 补偿值
     */
    public abstract void setAutoExposureCompensationStep(int value);

    /**
     * 设置曝光锁定
     *
     * @param lock 是否锁定
     */
    public abstract void setAutoExposureLock(boolean lock);

    /**
     * 设置测光区域
     *
     * @param rectangles 测光区域，可以设置多个实现矩阵测光
     */
    public abstract void setAutoExposureArea(MeteringRectangle[] rectangles);

    /**
     * 获取快门时间范围
     *
     * @return 范围
     */
    public abstract Range<Long> getExposureTimeRange();

    /**
     * 设置快门时间
     *
     * @param value 时间
     */
    public abstract void setExposureTime(long value);

    /**
     * 获取镜头可调整光圈
     *
     * @return 光圈
     */
    public abstract float[] getAvailableLensAperture();

    /**
     * 设置光圈
     *
     * @param value 光圈
     */
    public abstract void setLensAperture(float value);

    /**
     * 获取白平衡模式
     *
     * @return 模式
     */
    public abstract int[] getAvailableWhiteBalanceMode();

    /**
     * 设置白平衡模式
     *
     * @param mode 白平衡模式
     */
    public abstract void setWhiteBalanceMode(int mode);

    /**
     * 自动白平衡锁定
     *
     * @param lock 是否锁定
     */
    public abstract void setAutoWhiteBalanceLock(boolean lock);

    /**
     * ISO值范围
     *
     * @return 范围
     */
    public abstract Range<Integer> getISORange();

    /**
     * 设置ISO
     *
     * @param value ISO值
     */
    public abstract void setISO(int value);

    /**
     * 获取自动对焦模式
     *
     * @return 模式值
     */
    public abstract int[] getFocusModes();

    /**
     * 设置对焦模式
     *
     * @param mode 模式值
     */
    public abstract void setFocusMode(int mode);

    /**
     * 设置对焦区域
     *
     * @param rectangle 区域
     */
    public abstract void setFocusArea(MeteringRectangle rectangle);

    /**
     * 获取最小对焦距离
     *
     * @return 最小距离
     */
    public abstract float getMiniFocusDistance();

    /**
     * 设置对焦距离
     *
     * @param distance 距离
     */
    public abstract void setFocusDistance(float distance);
}
