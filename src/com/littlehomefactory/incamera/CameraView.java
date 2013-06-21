package com.littlehomefactory.incamera;


import java.io.IOException;

import android.content.Context;

import android.hardware.Camera;

import android.util.Log;

import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;


public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
    private SurfaceHolder mHolder;
    private Camera mCamera;

    private String TAG = "com.littlehomefactory.incamera.CameraView";

    public static interface CameraReadyCallback { 
        public void onCameraReady(SurfaceView preview); 
    }

    private CameraReadyCallback m_on_camera_ready = null;

    public CameraView(Context context, Camera camera, 
		      CameraReadyCallback on_camera_ready) {
        super(context);

        mCamera = camera;

	m_on_camera_ready = on_camera_ready;

        // Install a SurfaceHolder.Callback so we get notified when
        // the underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior
        // to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to
        // draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
	    if (m_on_camera_ready != null);
	        m_on_camera_ready.onCameraReady(this);
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your
        // activity.
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // If your preview can change or rotate, take care of those
        // events here.  Make sure to stop the preview before resizing
        // or reformatting it.

        if (mHolder.getSurface() == null){
          // preview surface does not exist
          return;
        }

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }
}