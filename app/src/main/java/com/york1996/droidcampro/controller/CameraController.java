package com.york1996.droidcampro.controller;

import android.hardware.camera2.params.MeteringRectangle;
import android.util.Range;

public abstract class CameraController {

    /***
     * 开始摄影
     */
    public abstract void start();

    /**
     * 结束摄影
     */
    public abstract void stop();

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
    public abstract Range<Integer> getExposureCompensationRange();

    /**
     * 设置曝光补偿
     *
     * @param value 补偿值
     */
    public abstract void setExposureCompensationStep(int value);

    /**
     * 设置曝光锁定
     *
     * @param lock 是否锁定
     */
    public abstract void setExposureLock(boolean lock);

    /**
     * 设置曝光区域
     *
     * @param rectangles 区域
     */
    public abstract void setExposureArea(MeteringRectangle[] rectangles);

    /**
     * 获取快门时间范围
     *
     * @return 范围
     */
    public abstract Range<Integer> getExposureTimeRange();

    /**
     * 设置快门时间
     *
     * @param value 时间
     */
    public abstract void setExposureTime(int value);

    /**
     * 获取白平衡值范围
     *
     * @return 范围
     */
    public abstract Range<Integer> getWhiteBalanceRegionRange();

    /**
     * 设置白平衡值
     *
     * @param value 白平衡值
     */
    public abstract void setWhiteBalanceRegion(int value);

    /**
     * 自动白平衡
     *
     * @param auto 是否自动
     */
    public abstract void setAutoWhitBalance(boolean auto);

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
     * 设置对焦锁定
     *
     * @param lock 是否锁定
     */
    public abstract void setFocusLock(boolean lock);

    /**
     * 获取对焦距离范围
     *
     * @return 距离范围
     */
    public abstract Range<Integer> getFocusDistanceRange();

    /**
     * 设置对焦距离
     *
     * @param distance 距离
     */
    public abstract void setFocusDistance(int distance);
}
