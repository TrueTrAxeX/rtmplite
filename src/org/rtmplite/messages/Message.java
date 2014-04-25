package org.rtmplite.messages;

import org.rtmplite.amf.AMFObjectEncoder;
import org.rtmplite.utils.RTMPUtils;

import com.sun.corba.se.impl.ior.ByteBuffer;

public class Message {
	
	public static final int HEADER_CONTINUE = 7;
	public static final int HEADER_NEW = 0x0;
	
	private int chunkSize = 128;
	
	private HeaderEncoder header;
	private AMFObjectEncoder object;
	
	private int headerLength;
	private int bodyLength;
	
	public Message(HeaderEncoder header, AMFObjectEncoder object) {
		this.header = header;
		this.object = object;
	}
	
	public int getHeaderLength() {
		return headerLength;
	}
	
	public int getBodyLength() {
		return bodyLength;
	}
	
	/**
	 * Split data on chunks and encode this to bytes
	 */
	public byte[] splitOnChunks(byte[] sourceBytes) {
		
		int numChunks = (int) Math.ceil(sourceBytes.length / (float) chunkSize);
		int dataLen = sourceBytes.length;
		
		if (numChunks == 1) {
			return sourceBytes;
		} else {
			ByteBuffer buffer = new ByteBuffer();
			byte[] marker = RTMPUtils.getChunkMarker((byte)HEADER_CONTINUE, header.getChannelId());
			
			int pos = 0;
			
			//int extendedTimestamp = header.getExtendedTimestamp();
			for (int i = 0; i < numChunks - 1; i++) {
				//BufferUtils.put(out, data, chunkSize);
				
				for(int a=0; a<chunkSize; a++) {
					buffer.append(sourceBytes[pos++]);
				}
				
				for(int a=0; a<marker.length; a++) {
					buffer.append(marker[a]);
				}
				
				dataLen -= chunkSize;
				//RTMPUtils.encodeHeaderByte(out, HEADER_CONTINUE, channelId);
				//if (extendedTimestamp != 0) {
				//	out.putInt(extendedTimestamp);
				//}
			}
			
			for(int a=0; a<dataLen; a++) {
				buffer.append(sourceBytes[pos++]);
			}
			
			buffer.trimToSize();
		
			return buffer.toArray();
		}
	}
	
	public byte[] getRawBytes() {
		
		byte[] amfObjectBytes = object.getRawBytes();

		bodyLength = amfObjectBytes.length;
		header.setBodySize(amfObjectBytes.length);
		
		amfObjectBytes = splitOnChunks(amfObjectBytes);
		
		byte[] headerBytes = header.toByteArray();
		
		headerLength = headerBytes.length;
		
		int totalLength = amfObjectBytes.length + headerBytes.length;
		
		byte[] finalBytes = new byte[totalLength];
		
		int pos = 0;
		
		for(int i=0; i<headerBytes.length; i++) {
			finalBytes[pos++] = headerBytes[i];
		}
		
		for(int i=0; i<amfObjectBytes.length; i++) {
			finalBytes[pos++] = amfObjectBytes[i];
		}
		
		return finalBytes;
	}
}
