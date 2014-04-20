package org.rtmplite.utils;

import java.nio.ByteBuffer;

public class NumberUtils {
	public static int threeBytesToInt(byte b1, byte b2, byte b3) {
		int r = (b3 & 0xFF) | ((b2 & 0xFF) << 8) | ((b1 & 0x0F) << 16);
        return r;
    }
	
	public static byte[] intToThreeBytes(int number) {
		byte b1,b2,b3;
		b3 = (byte)(number & 0xFF);
		b2 = (byte)((number >> 8) & 0xFF);
		b1 = (byte)((number >> 16) & 0xFF);
		
		return new byte[] { b1, b2, b3 };
	}
	
	public static byte[] intToBytes(int value) {
	    return new byte[] {
	        (byte) value,
	        (byte) (value >> 8),
	        (byte) (value >> 16),
	        (byte) (value >> 24)
	    };
	}
	
	public static int bytesToInt(byte[] bytes) {
	     return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
	}
	
	public static byte[] shortToBytes(short number) {
		
		return new byte[] { (byte)(number >> 8), (byte)(number)};
	}
	
	public static byte[] longToBytes(long number) {
		byte[] vInt = new byte[] {
				 (byte)(number >>> 56), 
				 (byte)(number >>> 48), 
				 (byte)(number >>> 40), 
				 (byte)(number >>> 32), 
				 (byte)(number >>> 24),
				 (byte)(number >>> 16),
				 (byte)(number >>> 8),
				 (byte)number};
		return vInt;
	}
	
	public static byte[] doubleToBytes(double d) {
	    long l = Double.doubleToRawLongBits(d);
	    return new byte[] {
	        (byte)((l >> 56) & 0xff),
	        (byte)((l >> 48) & 0xff),
	        (byte)((l >> 40) & 0xff),
	        (byte)((l >> 32) & 0xff),
	        (byte)((l >> 24) & 0xff),
	        (byte)((l >> 16) & 0xff),
	        (byte)((l >> 8) & 0xff),
	        (byte)((l >> 0) & 0xff),
	    };
	}

}
