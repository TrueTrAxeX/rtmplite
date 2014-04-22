package org.rtmplite.amf;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.events.IRTMPEvent;
import org.rtmplite.messages.Constants;
import org.rtmplite.messages.Header;
import org.rtmplite.messages.Packet;
import org.rtmplite.messages.RTMPDecodeState;
import org.rtmplite.utils.BufferUtils;
import org.rtmplite.utils.RTMPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncomingDataParser implements Constants {
	private int globalChunkSize = 4096;
	// protection for the decoder when using multiple threads per connection
	public static Semaphore decoderLock = new Semaphore(1, true);
	
	private Logger log = LoggerFactory.getLogger(IncomingDataParser.class);
	
	private Map<Integer, Header> lastHeaders = new HashMap<Integer, Header>();
	private Map<Integer, Packet> lastPackets = new HashMap<Integer, Packet>();
	
	private RTMPDecodeState state;
	
	public IncomingDataParser(RTMPDecodeState state) {
		this.state = state;
	}
	
	public void onData(IoBuffer in, int bytesRead) {
		final int remaining = in.remaining();
	
		// We need at least one byte
		if (remaining < 1) {
			state.bufferDecoding(1);
			return;
		}
		
		final int position = in.position();
		byte headerByte = in.get();
		
		int headerValue;
		int byteCount;
		if ((headerByte & 0x3f) == 0) {
			// Two byte header
			if (remaining < 2) {
				in.position(position);
				state.bufferDecoding(2);
				return;
			}
			headerValue = (headerByte & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 2;
		} else if ((headerByte & 0x3f) == 1) {
			// Three byte header
			if (remaining < 3) {
				in.position(position);
				state.bufferDecoding(3);
				return;
			}
			headerValue = (headerByte & 0xff) << 16 | (in.get() & 0xff) << 8 | (in.get() & 0xff);
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
		//headerLength += byteCount - 1;
		
		switch (headerSize) {
			case Constants.HEADER_NEW:
			case Constants.HEADER_SAME_SOURCE:
			case Constants.HEADER_TIMER_CHANGE:
				if (remaining >= headerLength) {
					int timeValue = RTMPUtils.readUnsignedMediumInt(in);
					if (timeValue == 0xffffff) {
						headerLength += 4;
					}
				}
				break;
			case Constants.HEADER_CONTINUE:
				if (lastHeader != null && lastHeader.getExtendedTimestamp() != 0) {
					headerLength += 4;
				}
				break;
			default:
				throw new RuntimeException("Unexpected header size " + headerSize + " check for error");
		}
		
		if (remaining < headerLength) {
			log.trace("Header too small (hlen: {}), buffering. remaining: {}", headerLength, remaining);
			in.position(position);
			state.bufferDecoding(headerLength);
			return;
		}
		
		// Move the position back to the start
		in.position(position);
		
		final Header header = decodeHeader(in, lastHeader);
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
		final int chunkSize = globalChunkSize;
		final int readAmount = (readRemaining > chunkSize) ? chunkSize : readRemaining;
		
		if (in.remaining() < readAmount) {
			log.debug("Chunk too small, buffering ({},{})", in.remaining(), readAmount);
			// skip the position back to the start
			in.position(position);
			state.bufferDecoding(headerLength + readAmount);
			return;
		}
		
		BufferUtils.put(buf, in, readAmount);
		
		if (buf.position() < header.getSize()) {
			state.continueDecoding();
			return;
		}
		
		if (buf.position() > header.getSize()) {
			log.warn("Packet size expanded from {} to {} ({})", new Object[] { (header.getSize()), buf.position(), header });
		}
		
		buf.flip();
		
		try {
			final IRTMPEvent message = decodeMessage(packet.getHeader(), buf);
			//message.setHeader(packet.getHeader());
			// Unfortunately flash will, especially when resetting a video stream with a new key frame, sometime 
			// send an earlier time stamp.  To avoid dropping it, we just give it the minimal increment since the 
			// last message.  But to avoid relative time stamps being mis-computed, we don't reset the header we stored.
			final Header lastReadHeader = lastHeaders.get(channelId);
			
			//lastHeaders.put(channelId, packet.getHeader());
			//packet.setMessage(message);
			
			// collapse the time stamps on the last packet so that it works right for chunk type 3 later
			lastHeader = lastHeaders.get(channelId);
			lastHeader.setTimerBase(header.getTimer());
			
			System.out.println("HEADER SIZE: " + lastHeader.getSize());
			System.out.println("DATA TIME: " + lastHeader.getDataType());
			System.out.println("CHANNEL ID: " + lastHeader.getChannelId());
			System.out.println("TIMESTAMP: " + lastHeader.getTimerBase());
		} finally {
			lastPackets.put(channelId, null);
		}
		
		System.out.println("¡¿…“ œ–Œ◊»“¿ÕŒ: " + bytesRead);
	}
	
	private IRTMPEvent decodeMessage(Header header, IoBuffer in) {
		
		IRTMPEvent message;
		byte dataType = header.getDataType();
	
		return null;
	}

	/**
	 * Decodes packet header.
	 * 
	 * @param in Input IoBuffer
	 * @param lastHeader Previous header
	 * @return Decoded header
	 */
	public Header decodeHeader(IoBuffer in, Header lastHeader) {
		if (log.isTraceEnabled()) {
			log.trace("decodeHeader - lastHeader: {} buffer: {}", lastHeader, in);
		}
		byte headerByte = in.get();
		int headerValue;
		int byteCount = 1;
		if ((headerByte & 0x3f) == 0) {
			// Two byte header
			headerValue = (headerByte & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 2;
		} else if ((headerByte & 0x3f) == 1) {
			// Three byte header
			headerValue = (headerByte & 0xff) << 16 | (in.get() & 0xff) << 8 | (in.get() & 0xff);
			byteCount = 3;
		} else {
			// Single byte header
			headerValue = headerByte & 0xff;
			byteCount = 1;
		}
		final int channelId = RTMPUtils.decodeChannelId(headerValue, byteCount);
		final int headerSize = RTMPUtils.decodeHeaderSize(headerValue, byteCount);
		Header header = new Header();
		header.setChannelId(channelId);
		if (headerSize != HEADER_NEW && lastHeader == null) {
			log.error("Last header null not new, headerSize: {}, channelId {}", headerSize, channelId);
			//this will trigger an error status, which in turn will disconnect the "offending" flash player
			//preventing a memory leak and bringing the whole server to its knees
			return null;
		}
		int timeValue;
		switch (headerSize) {
			case HEADER_NEW:
				// an absolute time value
				timeValue = RTMPUtils.readUnsignedMediumInt(in);
				header.setSize(RTMPUtils.readUnsignedMediumInt(in));
				header.setDataType(in.get());
				header.setStreamId(RTMPUtils.readReverseInt(in));
				if (timeValue == 0xffffff) {
					timeValue = (int) (in.getUnsignedInt() & Integer.MAX_VALUE);
					header.setExtendedTimestamp(timeValue);
				}
				header.setTimerBase(timeValue);
				header.setTimerDelta(0);
				break;
			case HEADER_SAME_SOURCE:
				// a delta time value
				timeValue = RTMPUtils.readUnsignedMediumInt(in);
				header.setSize(RTMPUtils.readUnsignedMediumInt(in));
				header.setDataType(in.get());
				header.setStreamId(lastHeader.getStreamId());
				if (timeValue == 0xffffff) {
					timeValue = (int) (in.getUnsignedInt() & Integer.MAX_VALUE);
					header.setExtendedTimestamp(timeValue);
				} else if (timeValue == 0 && header.getDataType() == TYPE_AUDIO_DATA) {
					// header.setIsGarbage(true);
					log.trace("Audio with zero delta; setting to garbage; ChannelId: {}; DataType: {}; HeaderSize: {}", new Object[] { header.getChannelId(), header.getDataType(),
							headerSize });
				}
				header.setTimerBase(lastHeader.getTimerBase());
				header.setTimerDelta(timeValue);
				break;
			case HEADER_TIMER_CHANGE:
				// a delta time value
				timeValue = RTMPUtils.readUnsignedMediumInt(in);
				header.setSize(lastHeader.getSize());
				header.setDataType(lastHeader.getDataType());
				header.setStreamId(lastHeader.getStreamId());
				if (timeValue == 0xffffff) {
					timeValue = (int) (in.getUnsignedInt() & Integer.MAX_VALUE);
					header.setExtendedTimestamp(timeValue);
				} else if (timeValue == 0 && header.getDataType() == TYPE_AUDIO_DATA) {
					// header.setIsGarbage(true);
					log.trace("Audio with zero delta; setting to garbage; ChannelId: {}; DataType: {}; HeaderSize: {}", new Object[] { header.getChannelId(), header.getDataType(),
							headerSize });
				}
				header.setTimerBase(lastHeader.getTimerBase());
				header.setTimerDelta(timeValue);
				break;
			case HEADER_CONTINUE:
				header.setSize(lastHeader.getSize());
				header.setDataType(lastHeader.getDataType());
				header.setStreamId(lastHeader.getStreamId());
				header.setTimerBase(lastHeader.getTimerBase());
				header.setTimerDelta(lastHeader.getTimerDelta());
				if (lastHeader.getExtendedTimestamp() != 0) {
					timeValue = (int) (in.getUnsignedInt() & Integer.MAX_VALUE);
					header.setExtendedTimestamp(timeValue);
					log.trace("HEADER_CONTINUE with extended timestamp: {}", timeValue);
				}
				break;
			default:
				log.error("Unexpected header size: {}", headerSize);
				return null;
		}
		
		log.trace("CHUNK, D, {}, {}", header, headerSize);
		return header;
	}
}
