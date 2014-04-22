package org.rtmplite.amf;

import org.rtmplite.utils.NumberUtils;

import com.sun.corba.se.impl.ior.ByteBuffer;

public class AMFObjectEncoder {
	
	private ByteBuffer buffer;
	
	public AMFObjectEncoder() {
		buffer = new ByteBuffer();
	}
	
	public synchronized void addString(String object) {
		buffer.append(AMFDataTypes.STRING);
		
		short shortLength = (short) object.length();
		byte[] shortLengthBytes = NumberUtils.shortToBytes(shortLength);
		
		for(int i=0; i<shortLengthBytes.length; i++) {
			buffer.append((byte)shortLengthBytes[i]);
		}
		
		byte[] chars = object.getBytes();
		
		for(int i=0; i<chars.length; i++) {
			buffer.append((byte)chars[i]);
		}
	}
	
	public synchronized void addBytes(byte[] data) {
		for(int i=0; i<data.length; i++) {
			buffer.append(data[i]);
		}
	}
	
	public synchronized void addNumber(Double number) {
		buffer.append(AMFDataTypes.NUMBER);
		
		byte[] doubleBytes = NumberUtils.doubleToBytes(number);
		
		for(int i=0; i<doubleBytes.length; i++) {
			buffer.append(doubleBytes[i]);
		}
	}
	
	public synchronized void addArray(AMFArrayEncoder array) {
		buffer.append(AMFDataTypes.OBJECT);
		
		byte[] arr = array.toArray();
		
		for(int i=0; i<arr.length; i++) {
			buffer.append(arr[i]);
		}
	}
	
	public synchronized void addNull() {
		buffer.append(AMFDataTypes.NULL);
	}
	
	public synchronized byte[] getRawBytes() {
		buffer.trimToSize();
		return buffer.toArray();
	}
}
