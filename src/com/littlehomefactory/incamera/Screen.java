package com.littlehomefactory.incamera;

import android.view.Window;
import android.view.WindowManager;


public class Screen {
    public static void keepOn(Window window) {
	window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
}