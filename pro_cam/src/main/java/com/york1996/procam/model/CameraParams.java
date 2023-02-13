package com.york1996.procam.model;

import java.util.Objects;

public class CameraParams {
    // 快门时间
    private long mExposureTime;
    // ISO
    private int mISO;
    // 光圈
    private float mLensAperture;
    // 曝光补偿
    private int mAutoExposureCompensation;

    public long getExposureTime() {
        return mExposureTime;
    }

    public void setExposureTime(long exposureTime) {
        mExposureTime = exposureTime;
    }

    public int getISO() {
        return mISO;
    }

    public void setISO(int ISO) {
        mISO = ISO;
    }

    public float getLensAperture() {
        return mLensAperture;
    }

    public void setLensAperture(float lensAperture) {
        mLensAperture = lensAperture;
    }

    public int getAutoExposureCompensation() {
        return mAutoExposureCompensation;
    }

    public void setAutoExposureCompensation(int autoExposureCompensation) {
        mAutoExposureCompensation = autoExposureCompensation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CameraParams that = (CameraParams) o;
        return mExposureTime == that.mExposureTime && mISO == that.mISO && Float.compare(that.mLensAperture, mLensAperture) == 0 && mAutoExposureCompensation == that.mAutoExposureCompensation;
    }

    @Override
    public String toString() {
        return "CameraParams{" +
                "mExposureTime=" + mExposureTime +
                ", mISO=" + mISO +
                ", mLensAperture=" + mLensAperture +
                ", mAutoExposureCompensation=" + mAutoExposureCompensation +
                '}';
    }
}
