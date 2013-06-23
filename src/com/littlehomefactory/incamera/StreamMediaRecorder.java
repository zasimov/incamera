package com.littlehomefactory.incamera;


import java.io.IOException;

import android.hardware.Camera;

import android.media.CamcorderProfile;
import android.media.MediaRecorder;

import android.net.LocalServerSocket;
import android.net.LocalSocket;


public class StreamMediaRecorder extends MediaRecorder {
    private LocalServerSocket m_server = null;
    private LocalSocket m_send_gate = null;

    public StreamMediaRecorder(Camera camera,  // camera must be unlocked
			       LocalServerSocket server,
			       int send_buffer_size)
      throws IOException {
	super();

	// Open send gate
	m_server = server;
	m_send_gate = m_server.accept();
	m_send_gate.setSendBufferSize(send_buffer_size);

	setCamera(camera);

	// Sources
	setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
	setVideoSource(MediaRecorder.VideoSource.CAMERA);
	
	// Output format (video)
	// TODO(zasimov): move this settings to settings window
	setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
	//setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
	setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
	setVideoEncoder(MediaRecorder.VideoEncoder.H264);
	setVideoSize(640, 480);
	setVideoFrameRate(20);
	setVideoEncodingBitRate(1024*1024);
	// the bit rate should be adjusted silently if out of range
	setAudioEncodingBitRate(320000);
	setAudioSamplingRate(48000);

	// Quality
	// Use instead of setOutputFormat, setAudioEncoder and other
	// otherwise - invalid state 4
	//setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

	setOutputFile(m_send_gate.getFileDescriptor());
    }

    @Override
    public void release() {
	try { 
	    if (m_send_gate != null)
		m_send_gate.close();
	} catch (IOException e) {
	    // pass
	    m_send_gate = null;
	}
	super.release();
    }
}