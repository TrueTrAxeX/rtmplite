package org.rtmplite.utils;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.messages.Constants;

import com.sun.corba.se.impl.ior.ByteBuffer;

/**
 * Chunks utils
 * @author blade-x
 *
 */
public class ChunksUtils {
	
	public static byte[] splitOnChunks(int chunkSize, IoBuffer source, byte channelId) {
		int numChunks = (int) Math.ceil(source.limit() / (float) chunkSize);
		byte[] marker = RTMPUtils.getChunkMarker((byte)Constants.HEADER_CONTINUE, channelId);
		ByteBuffer buffer = new ByteBuffer();
		
		source.rewind();
		
		int dataLen = source.limit();
		
		//int extendedTimestamp = header.getExtendedTimestamp();
		for (int i = 0; i < numChunks - 1; i++) {
			//BufferUtils.put(out, data, chunkSize);
			
			for(int a=0; a<chunkSize; a++) {
				buffer.append(source.get());
			}
			
			for(int a=0; a<marker.length; a++) {
				buffer.append(marker[a]);
			}
			
			dataLen -= chunkSize;
		}
		
		for(int a=0; a<dataLen; a++) {
			buffer.append(source.get());
		}
		
		buffer.trimToSize();
		
		return buffer.toArray();
	}
	
}
