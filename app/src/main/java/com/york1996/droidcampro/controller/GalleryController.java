package com.york1996.droidcampro.controller;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

public class GalleryController {
    private final Context mContext;

    public GalleryController(Context context) {
        mContext = context;
    }

    /**
     * 保存图片
     *
     * @param imageBytes 图片字节数据
     * @return 保存后在相册的uri
     */
    public Uri savePictureToGallery(byte[] imageBytes) {
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
        String path = MediaStore.Images.Media.insertImage(mContext.getContentResolver(), bitmap, "IMG_" + System.currentTimeMillis(), null);
        if (!TextUtils.isEmpty(path)) {
            return Uri.parse(path);
        } else {
            return null;
        }
    }

    /**
     * 跳转系统相册浏览
     *
     * @param uri uri
     */
    public void jumpToViewPicture(Uri uri) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "image/*");
        mContext.startActivity(intent);
    }

    /**
     * 跳转系统相册
     */
    public void jumpToSystemGallery() {
        Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        mContext.startActivity(intent);
    }
}
