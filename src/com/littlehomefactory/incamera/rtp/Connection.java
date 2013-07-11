package com.littlehomefactory.incamera.rtp;

import java.io.IOException;

import java.net.Socket;
import java.net.InetAddress;

import android.util.Log;


public class Connection implements Runnable {
    private boolean connected = false;
    private Socket socket = null;

    private Thread thread = null;

    static private final String TAG = "com.littlehomefactory.incamera.rtp.Connection";

    static private String ip;
    static private int port;

    public Connection() {
	this.ip = "192.168.1.52";
	this.port = 8081;
    }
    
    public void start() {
	if (thread == null || thread.getState() == Thread.State.TERMINATED) {
	    thread = new Thread(this);
	    thread.start();
	}
    }

    public void stop() {
	if (thread != null)
	    thread.interrupt();
	setDisconnected();
	thread = null;
    }

    public synchronized void setDisconnected() {
	connected = false;
	if (socket != null) {
	    try {
		socket.close();
	    } catch (IOException e) {
		// pass
	    }
	    socket = null;
	}
    }

    public synchronized void setConnected() {
	connected = true;
    }

    public synchronized boolean isConnected() {
	return connected;
    }

    public Socket getSocket() {
	return socket;
    }

    public void run() {
	Log.d(TAG, "Try connect to server");
	setDisconnected();
	try {
	    InetAddress address = InetAddress.getByName(this.ip);
	    socket = new Socket(address, this.port);
	    setConnected();
	    Log.d(TAG, "Connected");
	} catch (Exception e) {
	    Log.d(TAG, "Connection failed");
	    // pass
	}
    }

}