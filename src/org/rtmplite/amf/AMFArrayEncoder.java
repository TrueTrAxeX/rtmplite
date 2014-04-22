package org.rtmplite.amf;

import org.rtmplite.utils.NumberUtils;

import com.sun.corba.se.impl.ior.ByteBuffer;

public class AMFArrayEncoder {
	
	private ByteBuffer buffer = new ByteBuffer();
	
	private synchronized void appendHeader(String key) {
		byte[] lengthInBytes = NumberUtils.shortToBytes((short) key.length());
		
		for(int i=0; i<lengthInBytes.length; i++) {
			buffer.append((byte)lengthInBytes[i]);
		}
		
		char[] chars = key.toCharArray();
		
		for(int i=0; i<chars.length; i++) {
			buffer.append((byte)chars[i]);
		}
	}
	
	public synchronized void append(String key, double value) {
		
		this.appendHeader(key);
		
		buffer.append(AMFDataTypes.NUMBER);
		
		byte[] doubleBytes = NumberUtils.doubleToBytes(value);
		
		for(int i=0; i<doubleBytes.length; i++) {
			buffer.append((byte)doubleBytes[i]);
		}
	}
	
	public synchronized void append(String key, String value) {
		
		this.appendHeader(key);
		
		buffer.append(AMFDataTypes.STRING);
		
		byte[] lengthInBytes = NumberUtils.shortToBytes((short) value.length());
		
		for(int i=0; i<lengthInBytes.length; i++) {
			buffer.append((byte)lengthInBytes[i]);
		}
		
		byte[] chars = value.getBytes();
		
		for(int i=0; i<chars.length; i++) {
			buffer.append((byte)chars[i]);
		}
	}
	
	public synchronized void append(String key, boolean value) {
		
		this.appendHeader(key);
		
		buffer.append(AMFDataTypes.BOOLEAN);
		
		if(value) {
			buffer.append((byte) 1);
		} else {
			buffer.append((byte) 0);
		}
	}
	
	public synchronized void appendNull(String key) {
		this.appendHeader(key);
		
		buffer.append(AMFDataTypes.NULL);
	}
	
	public synchronized byte[] toArray() {
		
		buffer.append((byte) 0);
		buffer.append((byte) 0);
		buffer.append((byte) 9);
		
		buffer.trimToSize();
		
		return buffer.toArray();
	}
}
