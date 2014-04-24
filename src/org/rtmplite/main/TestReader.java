package org.rtmplite.main;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.amf.RTMPDecoder;
import org.rtmplite.messages.Constants;
import org.rtmplite.messages.Header;
import org.rtmplite.messages.Packet;
import org.rtmplite.utils.BufferUtils;
import org.rtmplite.utils.RTMPUtils;

public class TestReader {
	
	private int TIMEOUT = 0;
	
	private Map<Integer, Header> lastHeaders = new HashMap<Integer, Header>();
	private Map<Integer, Packet> lastPackets = new HashMap<Integer, Packet>();
	
	private InputStream inputStream;
	
	public TestReader(InputStream inputStream) {
		this.inputStream = inputStream;
	
		try {
			this.process();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public byte get() throws IOException {
		
		while(inputStream.available() < 1) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return (byte) inputStream.read();
	}
	
	public byte[] get(int amount) throws IOException {
		byte[] buffer = new byte[amount];
		
		while(inputStream.available() < amount) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		inputStream.read(buffer);
		
		return buffer;
	}
	
	public void process() throws IOException {
		
		while(true) {
			IoBuffer totalBuffer = IoBuffer.allocate(0);
			totalBuffer.setAutoExpand(true);
			
			byte headerByte = get();
			totalBuffer.put(headerByte);
			
			int headerValue;
			int byteCount;
			if ((headerByte & 0x3f) == 0) {
				byte b1 = get();
				totalBuffer.put(b1);
				
				headerValue = (headerByte & 0xff) << 8 | (b1 & 0xff);
				byteCount = 2;
			} else if ((headerByte & 0x3f) == 1) {
				byte b1 = get(); byte b2 = get();
				totalBuffer.put(b1); totalBuffer.put(b2);
				
				headerValue = (headerByte & 0xff) << 16 | (b1 & 0xff) << 8 | (b2 & 0xff);
				byteCount = 3;
			} else {
				
				// Single byte header
				headerValue = headerByte & 0xff;
				byteCount = 1;
			}
			
			final int channelId = RTMPUtils.decodeChannelId(headerValue, byteCount);
			
			if (channelId < 0) {
				throw new RuntimeException("Bad channel id: " + channelId);
			}
			
			// Get the header size and length
			byte headerSize = RTMPUtils.decodeHeaderSize(headerValue, byteCount);
			int headerLength = RTMPUtils.getHeaderLength(headerSize);
			
			Header lastHeader = lastHeaders.get(channelId);
			headerLength += byteCount - 1;
			
			switch (headerSize) {
			case Constants.HEADER_NEW:
			case Constants.HEADER_SAME_SOURCE:
			case Constants.HEADER_TIMER_CHANGE:
				
				IoBuffer tempIo = IoBuffer.wrap(get(3));
				
				int timeValue = RTMPUtils.readUnsignedMediumInt(tempIo);
				if (timeValue == 0xffffff) {
					headerLength += 4;
				}
	
				totalBuffer.put(tempIo.array());
				
				break;
			case Constants.HEADER_CONTINUE:
				if (lastHeader != null && lastHeader.getExtendedTimestamp() != 0) {
					headerLength += 4;
				}
				break;
			default:
				throw new RuntimeException("Unexpected header size " + headerSize + " check for error");
			}
			
			if(totalBuffer.limit() > 1)
				totalBuffer.put(get(headerLength-totalBuffer.limit()));
		
			final Header header = RTMPDecoder.decodeHeader(IoBuffer.wrap(totalBuffer.array(), 0, totalBuffer.limit()), lastHeader);
			if (header == null) {
				throw new RuntimeException("Header is null, check for error");
			}
			
			lastHeaders.put(channelId, header);
			
			// check to see if this is a new packets or continue decoding an existing one
			Packet packet = lastPackets.get(channelId);
			if (packet == null) {
				packet = new Packet(header.clone());
				lastPackets.put(channelId, packet);
			}
			
			final IoBuffer buf = packet.getData();
			
			final int readRemaining = header.getSize() - buf.position();
			final int chunkSize = 4096;
			final int readAmount = (readRemaining > chunkSize) ? chunkSize : readRemaining;
		
			BufferUtils.put(buf, IoBuffer.wrap(get(readAmount)), readAmount);
			
			if (buf.position() < header.getSize()) {
				//state.continueDecoding();
				continue;
			}
			
			if (buf.position() > header.getSize()) {
				System.out.println("Packet size expanded from {} to {} ({})");
			}
			
			System.out.println("дейндхпнбюмн!");
			System.out.println("CHANNEL: " + channelId);
			System.out.println("DATA TYPE: " + header.getDataType());
			lastPackets.put(channelId, null);
		
		}
	}
}
