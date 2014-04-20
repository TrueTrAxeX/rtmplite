package org.rtmplite.utils;

public class RTMPUtils {
	/**
     * Get chunk marker
     * @param chunk marker
	 *
     * @param headerSize         Header size marker
     * @param channelId          Channel used
     */
	public static byte[] getChunkMarker(byte headerSize, int channelId) {
		if (channelId <= 63) {
			return new byte[] { ((byte) ((headerSize << 6) + channelId)) };
		} else if (channelId <= 320) {
			return new byte[] { (byte) (headerSize << 6), (byte) (channelId - 64) };
		} else {
			byte[] buf = new byte[3];
			buf[0] =(byte) ((headerSize << 6) | 1);
			channelId -= 64;
			buf[1] = (byte) (channelId & 0xff);
			buf[2] = (byte) (channelId >> 8);
			
			return buf;
		}
		
	}
}
