package com.littlehomefactory.incamera.sequentor;

import java.io.InputStream;


public abstract class Sequentor extends Buffer {
    private int splitter_pos = -1;
    private boolean is_first_last_frame;

    public Sequentor(InputStream is, int buffer_size, int payload_size) {
	super(is, buffer_size, payload_size);
    }

    public Sequentor(InputStream is, byte[] buffer, int payload_size) {
	super(is, buffer, payload_size);
    }

    abstract public int getSequenceLength();	
    abstract public boolean isStart(byte b);
    abstract public boolean isEnd(int point);

    public int getSplitterPos() {
	return splitter_pos;
    }
	
    protected boolean find(int start) {
	int i;
	splitter_pos = -1;
	is_first_last_frame = false;
	for (i = start; i < getDataSize(); i++) {
	    if (isStart(buffer[i])) {
		if ((getDataSize() - i) < getSequenceLength()) {
		    setFrameEnd(i);
		    return false;
		}
		if (isEnd(i+1)) {
		    if (i == getPayloadStart()) {
			// find end of frame
			is_first_last_frame = find(i + getSequenceLength());
		    } else
			setFrameEnd(i);
		    splitter_pos = i;
		    return true;
		}
	    }
	}
	return false;
    }

    public boolean find() {
	return find(getPayloadStart());
    }
}