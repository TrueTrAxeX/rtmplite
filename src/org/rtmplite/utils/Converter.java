package org.rtmplite.utils;

import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;

import org.apache.mina.core.buffer.IoBuffer;

public class Converter {
	
	public static byte[] onMetaDataToSetDataFrame(IoBuffer sourceData) {
		sourceData.position(4);
		
		int packetLength = sourceData.getMediumInt();
		
		sourceData.position(13);
		
		short length = sourceData.getShort();
		
		try {
			String name = sourceData.getString((int) length, Charset.forName("UTF-8").newDecoder());
		
			if(name.equals("onMetaData")) {
				
				int dataFrameLength = "@setDataFrame".length();
				
				IoBuffer b = IoBuffer.allocate(dataFrameLength+3+sourceData.limit());
				
				for(int i=0; i<4; i++) {
					b.put(sourceData.get(i));
				}
				
				b.putMediumInt(packetLength+dataFrameLength+3);
				b.put((byte)18);
			
				sourceData.position(8);
				
				for(int i=0; i<4; i++) {
					b.put(sourceData.get());
				}
				
				b.put((byte)2);
				b.putShort((short)dataFrameLength);
				b.putString("@setDataFrame", Charset.forName("UTF-8").newEncoder());
				
				sourceData.position(12);
				
				while(sourceData.remaining() > 0) {
					b.put(sourceData.get());
				}
				
				return b.array();
			}
			
		} catch (CharacterCodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}
	
}
