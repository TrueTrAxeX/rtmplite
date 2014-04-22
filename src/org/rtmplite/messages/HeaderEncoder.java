package org.rtmplite.messages;

import org.rtmplite.utils.NumberUtils;


public class HeaderEncoder {
	
	private byte[] timestamp = { 0, 0, 0 }; // default init timestamp
	
	private byte channelId = (byte) 3; // default init encoding type
	
	private byte[] bodySize = { 0, 0, 0 }; // default init body size
	
	private byte[] streamId = { 0, 0, 0, 0 }; // default stream id
	
	private byte packetType = 0;
	
	public byte[] getEncodedTimestamp() {
		return timestamp;
	}
	
	public byte getEncodedEncryptType() {
		return channelId;
	}
	
	public byte[] getEncodedBodySize() {
		return bodySize;
	}
	
	public byte[] getEncodedStreamId() {
		return streamId;
	}
	
	public byte getPacketType() {
		return packetType;
	}
	
	/**
	 * Set packet timestamp
	 */
	public void setTimestamp(int timestamp) {
		this.timestamp = NumberUtils.intToThreeBytes(timestamp);
	}
	
	/**
	 * Set encrypt type
	 */
	public void setChannelId(byte encryptType) {
		this.channelId = encryptType;
	}
	
	/**
	 * Set body size
	 */
	public void setBodySize(int bodySize) {
		this.bodySize = NumberUtils.intToThreeBytes(bodySize);
		
		System.out.println("LENGTH: " + NumberUtils.threeBytesToInt(this.bodySize[0], this.bodySize[1], this.bodySize[2]));
	}
 	
	/**
	 * Set steram id
	 */
	public void setStreamId(int streamId) {
		this.streamId = NumberUtils.intToReverseBytes(streamId);
	}
	
	/**
	 * Set packet type
	 * Packet types contains in class PacketTypes
	 */
	public void setPacketType(byte type) {
		this.packetType = type;
	}
	
	private boolean withoutStreamId = false;
	
	public void withoutStreamId(boolean flag) {
		withoutStreamId = flag;
	}
	
	public byte[] toByteArray() {
		
		byte[] buf = new byte[12];
		
		int pos = 0;
		
		buf[pos++] = (byte) channelId;
		
		for(int i=0; i<timestamp.length; i++) {
			buf[pos++] = timestamp[i];
		}
		
		for(int i=0; i<bodySize.length; i++) {
			buf[pos++] = bodySize[i];
		}
		
		buf[pos++] = (byte) packetType;
		
		if(!withoutStreamId) {
			for(int i=0; i<streamId.length; i++) {
				buf[pos++] = streamId[i];
			}
		}
		
		return buf;
	}
	
	/**
	 * This class contain packet types
	 */
	public class PacketTypes {
		public static final byte AMF_COMMAND = (byte) 20;
	}
}
