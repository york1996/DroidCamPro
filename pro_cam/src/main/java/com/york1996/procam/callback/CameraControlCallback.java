package com.york1996.procam.callback;

import com.york1996.procam.model.CameraParams;

public interface CameraControlCallback {
    /**
     * 相机开始运行，可进行操作
     *
     * @param front 是否前置摄像头
     */
    void onCameraStarted(boolean front);

    /**
     * 相机结束运行，不可进行操作
     */
    void onCameraStopped();

    /**
     * 捕获照片数据回调
     *
     * @param photoBytes 照片数据
     */
    void onCameraCapture(byte[] photoBytes);

    /**
     * 相关参数改变
     *
     * @param params 参数
     */
    void onCameraParamsChanged(CameraParams params);

    /**
     * 错误异常
     *
     * @param errorCode 错误码
     */
    void onError(int errorCode);
}
