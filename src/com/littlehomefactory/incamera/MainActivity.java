package com.littlehomefactory.incamera;


import java.io.IOException;

import android.app.Activity;

import android.content.Context;
import android.content.pm.PackageManager;

import android.hardware.Camera;

import android.media.MediaRecorder;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import java.net.Socket;
import java.net.InetAddress;

import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import android.util.Log;

import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.View;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.littlehomefactory.incamera.rtp.RTPStream;


public class MainActivity extends Activity 
  implements CameraView.CameraReadyCallback {
    private Camera m_camera = null;

    private StreamMediaRecorder m_recorder = null;
    private RTPStream rtp_stream = null;
    private boolean m_recording = false;

    private FrameLayout m_camera_preview = null;
    private TextView m_camera_message = null;

    private LocalServerSocket m_server = null;
    private LocalSocket m_recv_gate;

    // Constants
    private final String SERVER_ADDRESS = "incamera_socket";
    private final int BUFFER_SIZE = 50000;

    public final String TAG = "com.littlehomefactory.incamera.MainActivity";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

	try {
	    m_server = new LocalServerSocket(SERVER_ADDRESS);
	} catch (IOException e) {
	    // TODO: log message
	    return;
	}

	try {
	    m_recv_gate = new LocalSocket();
	    m_recv_gate.connect(m_server.getLocalSocketAddress());
	    m_recv_gate.setReceiveBufferSize(BUFFER_SIZE);
	    rtp_stream = new RTPStream(m_recv_gate.getInputStream());
	} catch (IOException e) {
	    // TODO(zasimov): log message
	    try {
		m_server.close();
	    } catch (IOException e1) {
		// pass
	    }
	    m_server = null;
	    return;
	}

	if (! hardwareCameraIsExist(this)) {
	    // TODO(zasimov): check hardware camera feature
	}
	
	// Disable title bar
	requestWindowFeature(Window.FEATURE_NO_TITLE);

	Screen.keepOn(getWindow());

	m_camera = getCameraInstance();

	// NullException have obtained if setContentView after addView
        setContentView(R.layout.main);

	// Add camera preview widget
        FrameLayout m_camera_preview = (FrameLayout) findViewById(R.id.camera_preview);
	CameraView camera_view = new CameraView(this, m_camera, this);
	m_camera_preview.addView(camera_view);

	// Setup exit button click handler
	Button exit_button = (Button) findViewById(R.id.exit_button);
	exit_button.setOnClickListener(m_exit_action);

	// Setup first message for user
	m_camera_message = (TextView) findViewById(R.id.camera_message);
	m_camera_message.setText("Connecting...");
    }

    public void onCameraReady(SurfaceView preview) {
	m_camera.unlock();
	// TODO(zasimov): check m_camera for null

	try {
	    m_recorder = new StreamMediaRecorder(m_camera, m_server, BUFFER_SIZE);
	} catch (IOException e) {
	    // TODO(zasimov): log message
	    releaseCamera();
	    return;
	}

	m_recorder.setPreviewDisplay(preview.getHolder().getSurface());

	if (! prepareMediaRecorder()) {
	    // TODO(zasimov): log message
	    releaseCamera();
	    return;
	}

	m_recorder.start();
	try {
	    rtp_stream.start();
	} catch (IOException e) {
	    // pass
	    Log.d(TAG, "RTP Stream start failed");
	}
	m_recording = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
	if (m_recorder != null && m_recording)
	    m_recorder.stop();

        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();

	rtp_stream.stop();

	try {
	    if (m_recv_gate != null)
		m_recv_gate.close();
	} catch (IOException e) {
	    // pass
	}
	m_recv_gate = null;

	try {
	    if (m_server != null)
		m_server.close();
	} catch (IOException e) {
	    // pass
	}
	m_server = null;
    }

    private boolean hardwareCameraIsExist(Context context) 
    {
	if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
	    return true;
	} else {
	    return false;
	}
    }

    public static Camera getCameraInstance() {
	Camera c = null;
	try {
	    c = Camera.open(); // attempt to get a Camera instance
	}
	catch (Exception e){
	    // Camera is not available (in use or does not exist)
	}
	return c; // returns null if camera is unavailable
    }

    private void releaseCamera() {
        if (m_camera != null) {
            m_camera.release();
            m_camera = null;
        }
    }

    private OnClickListener m_exit_action = new OnClickListener() {
        @Override
        public void onClick(View view) {
            finish();
        }   
    };

    private boolean prepareMediaRecorder() {
	if (m_recorder == null || m_camera == null)
	    // TODO(zasimov): log message
	    return false;

	//recorder.setPreviewDisplay(mPreview.getHolder().getSurface());

	try {
	    m_recorder.prepare();
	} catch (IllegalStateException e) {
	    Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
	    releaseMediaRecorder();
	    return false;
	} catch (IOException e) {
	    Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
	    releaseMediaRecorder();
	    return false;
	}
	return true;
    }

    private void releaseMediaRecorder(){
        if (m_recorder != null) {
            m_recorder.reset();   // clear recorder configuration
            m_recorder.release(); // release the recorder object
            m_recorder = null;
            m_camera.lock();           // lock camera for later use
        }
    }

}
