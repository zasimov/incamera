package com.littlehomefactory.incamera;


public class LittleEndianBuffer {
    protected byte[] buffer;
    private int size;

    final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    public LittleEndianBuffer(int size) {
	this.size = size;
	this.buffer = new byte[size];
    }

    public void setByte(int index, byte b) {
	if (index >= this.size)
	    throw new IndexOutOfBoundsException();
	buffer[index] = b;
    }

    public byte getByte(int index) {
	return this.buffer[index];
    }

    public void setLong(int begin, int end, long n) {
	end--;
	if (end >= this.size)
	    throw new IndexOutOfBoundsException();
	while (end >= begin) {
	    buffer[end] = (byte) (n % 256);
	    n >>= 8;
	    end--;
	}
    }

    final public int getSize() {
	return this.size;
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
}
