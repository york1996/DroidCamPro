package com.york1996.droidcampro;

import android.content.Context;
import android.widget.Toast;

/**
 * Author:meizu on 2023/2/8 19:28
 * Email:zhangxinyu1@meizu.com
 * Description：TODO
 */
public class Utils {
    public static void showToast(Context context, String msg) {
        Toast.makeText(context.getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
}
