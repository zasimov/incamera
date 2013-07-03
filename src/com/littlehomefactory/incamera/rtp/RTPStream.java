package com.littlehomefactory.incamera.rtp;

import java.io.IOException;
import java.io.InputStream;

import java.net.Socket;
import java.net.InetAddress;

import android.os.SystemClock;
import android.util.Log;

import com.littlehomefactory.incamera.LittleEndianBuffer;
import com.littlehomefactory.incamera.sequentor.Sequentor;


public class RTPStream extends LittleEndianBuffer implements Runnable {
    public static final int RTP_HDR_START = 4;
    public static final int RTP_HDR_LEN = 12;
    public static final int H263_HDR_LEN = 2;
    public static final int H263_HDR_START = RTP_HDR_START + RTP_HDR_LEN;
    public static final int MAX_PACKET_SIZE = 1500; // local memory size
    public static final int PAYLOAD_START = H263_HDR_START + H263_HDR_LEN;

    private int ssrc = 1;
    private int sequence_n = 0;

    private InputStream is = null;
    private Sequentor sequentor;
    private byte[] previous_frame;
    private boolean previous_frame_exist = false;
    private int previous_frame_size;

    private Thread thread = null;

    public Connection connection = null;

    public final static String TAG = "com.littlehomefactory.incamera.rtp.RTPStream";

    public RTPStream(InputStream is) {
	super(MAX_PACKET_SIZE);
	if (is == null)
	    throw new RuntimeException("null is");
	previous_frame = new byte[getSize()];
	this.is = is;
	this.sequentor = new Sequentor(this.is, this.buffer, PAYLOAD_START) {
		public int getSequenceLength() { return 3; }
		public boolean isStart(byte b) { return b == 0x00; }
		public boolean isEnd(int point) {
		    return (buffer[point] == 0x00 && (buffer[point+1] & 0xFC) == 0x80);
		}
	    };
	this.connection = new Connection();
	connection.start();
    }

    public void setSsrc(long ssrc) {
	this.setLong(RTP_HDR_START + 8, RTP_HDR_START + 12, ssrc);
    }

    public void setTimestamp(long timestamp) {
	this.setLong(RTP_HDR_START + 4, RTP_HDR_START + 8, timestamp);
    }

    public void markPacket() {
	this.setLong(RTP_HDR_START + 2, RTP_HDR_START + 4, ++this.sequence_n);
    }

    public void start() throws IOException {
	if (thread == null || thread.getState() == Thread.State.TERMINATED) {
	    thread = new Thread(this);
	    thread.start();
	}
    }

    public void stop() {
	thread.interrupt();
	connection.stop();
	thread = null;
    }

    public void run() {
	try {
	    Log.d(TAG, "Skip MP4 header...");
	    skip_mp4_header();
	} catch (IOException e) {
	    Log.e(TAG, "End of mp4 header not found.");
	    return;
	}

	try {
	    sendStream();
	} catch (IOException e) {
	    // ignore IOExceptions within main loop
	}
    }

    private void sendStream() throws IOException {
	long time = 0;
	long duration = 0;
	boolean stream_found = false;
	boolean new_frame_found = false;

	this.setByte(RTP_HDR_START, (byte) 0x80);  // v=2
	this.setByte(RTP_HDR_START+1, (byte) 96);    // dynamic payload
	this.setSsrc(0xF1F2F3F4);

	// H.263 header
	// two byte long header (RFC 4629, section 5.1)
	buffer[H263_HDR_START] = 0;
	buffer[H263_HDR_START + 1] = 0;

	while (!Thread.interrupted()) {
	    time = System.nanoTime();

	    if (sequentor.fill() <= 0) {
		//Log.d(TAG, "Input buffer is empty");
		continue;
	    }

	    duration += System.nanoTime() - time;

	    new_frame_found = sequentor.find();

	    if (stream_found || new_frame_found) {
		stream_found = true;

		if (new_frame_found) {
		    if (previous_frame_exist) {
			// Send previous frame
			previous_frame[RTP_HDR_START + 1] += 0x80; // mark last packet;
			sendPreviousFrame();
		    } else {
			buffer[H263_HDR_START] = 4;
		    }
		} else {
		    buffer[H263_HDR_START] = 0;
		    if (previous_frame_exist)
			sendPreviousFrame();
		}

		previous_frame_exist = true;
		previous_frame_size = sequentor.getFrameSize();
		setLong(0, 2, previous_frame_size);
		buffer[2] = (byte) 0x93;
		buffer[3] = (byte) 0x94;
		System.arraycopy(buffer, 0,
				 previous_frame, 0,
				 sequentor.getFrameSize());

	    }
	}

	Log.d(TAG, "RTP Stream is stopped");
    }

    private void sendPreviousFrame() {
	if (connection.isConnected()) {
	    try {
		connection.getSocket().getOutputStream().write(previous_frame, 0, previous_frame_size);
	    } catch (IOException e) {
		Log.d(TAG, "sendPreviousFrame failed");
		connection.setDisconnected();
	    }
	} else
	    connection.start();
    }

    private void skip_mp4_header() throws IOException {
	while (true) {
	    while (this.is.read() != 'm');
	    this.is.read(buffer, RTP_HDR_LEN, 3);
	    if (buffer[RTP_HDR_LEN] == 'd' && buffer[RTP_HDR_LEN+1] == 'a' && 
		buffer[RTP_HDR_LEN+2] == 't')
		break;
	}
    }
}