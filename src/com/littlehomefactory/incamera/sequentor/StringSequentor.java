package com.littlehomefactory.incamera.sequentor;

import java.io.InputStream;


public class StringSequentor extends Sequentor {
    private String sequence;

    public class BadBufferSize extends RuntimeException {
    }

    public StringSequentor(InputStream is, int buffer_size, int payload_offset,
			   String sequence) {
	super(is, buffer_size, payload_offset);
	if (buffer_size - payload_offset < sequence.length())
	    throw new BadBufferSize();
	this.sequence = sequence;
    }

    public StringSequentor(InputStream is, byte[] buffer, int payload_offset,
			   String sequence) {
	super(is, buffer, payload_offset);
	if (buffer.length - payload_offset < sequence.length())
	    throw new BadBufferSize();
	this.sequence = sequence;
    }

    public int getSequenceLength() {
	return sequence.length();
    }

    public boolean isStart(byte b) {
	return (b == (byte) sequence.charAt(0));
    }

    public boolean isEnd(int point) {
	int j = 1;
	for (int i = point; i < point + getSequenceLength() - 1; i++) {
	    if (buffer[i] != (byte) sequence.charAt(j))
		return false;
	    j++;
	}
	return true;
    }
}