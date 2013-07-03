package com.littlehomefactory.incamera.sequentor;

import java.util.Arrays;
import java.io.InputStream;
import java.io.IOException;


public class Buffer {
    final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    private int buffer_size;
    private int payload_offset;
    private int payload_size;  // buffer_size - payload_offset
    private int data_end;
    private int frame_end;     // current frame end point
                               // frame_end >= payload_offset
                               // frame_end <= buffer_size
    // frame_size = frame_end - payload_offset;
    
    protected byte[] buffer;

    InputStream is;

    public Buffer(InputStream is, int buffer_size, int payload_offset) {
	this.is = is;
	if (payload_offset >= buffer_size)
	    throw new RuntimeException("Bad payload offset. Must be < buffer_size.");
	this.buffer_size = buffer_size;
	this.payload_offset = payload_offset;
	this.buffer = new byte[this.buffer_size];
	this.initPoints();
    }

    public Buffer(InputStream is, byte[] buffer, int payload_offset) {
	this.is = is;
	this.buffer = buffer;
	this.buffer_size = buffer.length;
	if (payload_offset >= buffer_size)
	    throw new RuntimeException("Bad payload offset. Must be < buffer_size.");
	this.payload_offset = payload_offset;
    }

    private void initPoints() {
	this.frame_end = payload_offset;
	this.data_end = payload_offset;  // no data
	this.calculatePayloadSize();
    }

    private void calculatePayloadSize() {
	payload_size = buffer_size - payload_offset;
    }

    private int getCurrentFrameSize() {
	return frame_end - payload_offset;
    }

    private int read(int fill_point, int length) throws IOException {
	int readed = 0;
	while (length > 0) {
	    int r = is.read(buffer, fill_point, length);
	    if (r < 0)
		return readed;
	    length -= r;
	    fill_point += r;
	    readed += r;
	}
	return readed;
    }

    public int fill() throws IOException {
	int fill_point = payload_offset;
	int tail_size = data_end - frame_end;
	if (tail_size != 0) {
	    System.arraycopy(buffer, frame_end,
			     buffer, fill_point,
			     tail_size);
	    fill_point += tail_size;
	}
	
	int to_read = buffer_size - fill_point;
	
	/*int available = is.available();
	if (available < to_read)
	to_read = available;*/

	int readed = read(fill_point, to_read);
	if (readed > 0) {
	    data_end = fill_point + readed;
	} else
	    data_end = fill_point;
	frame_end = data_end;
	return data_end - payload_offset;
    }

    public int getPayloadStart() {
	return payload_offset;
    }

    public int getDataSize() {
	return data_end;
    }

    public int getFrameSize() {
	return frame_end;
    }

    public void setFrameEnd(int frame_end) {
	if (frame_end > data_end)
	    throw new RuntimeException("frame_end > data_end");
	this.frame_end = frame_end;
    }

    public byte[] getData() {
	return buffer;
    }

    final public String toHex() {
	char[] hexChars = new char[buffer.length * 2];
	int v;
	for (int j=0; j<buffer.length; j++) {
	    v = (int) buffer[j] & 0xFF;
	    hexChars[j * 2] = hexArray[v >>> 4];
	    hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	}
	return new String(hexChars);
    }

    public byte[] copyFrame() {
	return Arrays.copyOfRange(buffer, payload_offset, frame_end);
    }
}