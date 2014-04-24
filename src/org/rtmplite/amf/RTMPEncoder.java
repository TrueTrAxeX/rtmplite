package org.rtmplite.amf;

import java.util.HashMap;
import java.util.Map;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.amf.packets.ChunkSize;
import org.rtmplite.amf.packets.Ping;
import org.rtmplite.amf.packets.SWFResponse;
import org.rtmplite.amf.packets.SetBuffer;
import org.rtmplite.events.IRTMPEvent;
import org.rtmplite.messages.Constants;
import org.rtmplite.messages.Header;
import org.rtmplite.utils.RTMPUtils;

public class RTMPEncoder implements Constants {
	
	private Map<Integer, Integer> lastFullTimestampWritten = new HashMap<Integer, Integer>();
	
	/** {@inheritDoc} */
	public IoBuffer encodePing(Ping ping) {
		int len;
		short type = ping.getEventType();
		switch (type) {
			case Ping.CLIENT_BUFFER:
				len = 10;
				break;
			case Ping.PONG_SWF_VERIFY:
				len = 44;
				break;
			default:
				len = 6;
		}
		final IoBuffer out = IoBuffer.allocate(len);
		out.putShort(type);
		switch (type) {
			case Ping.STREAM_BEGIN:
			case Ping.STREAM_PLAYBUFFER_CLEAR:
			case Ping.STREAM_DRY:
			case Ping.RECORDED_STREAM:
			case Ping.PING_CLIENT:
			case Ping.PONG_SERVER:
			case Ping.BUFFER_EMPTY:
			case Ping.BUFFER_FULL:
				out.putInt(ping.getValue2());
				break;
			case Ping.CLIENT_BUFFER:
				if (ping instanceof SetBuffer) {
					SetBuffer setBuffer = (SetBuffer) ping;
					out.putInt(setBuffer.getStreamId());
					out.putInt(setBuffer.getBufferLength());
				} else {
					out.putInt(ping.getValue2());
					out.putInt(ping.getValue3());
				}
				break;
			case Ping.PING_SWF_VERIFY:
				break;
			case Ping.PONG_SWF_VERIFY:
				out.put(((SWFResponse) ping).getBytes());
				break;
		}
		// this may not be needed anymore
		if (ping.getValue4() != Ping.UNDEFINED) {
			out.putInt(ping.getValue4());
		}
		return out;
	}
	
	/**
	 * Calculate number of bytes necessary to encode the header.
	 * 
	 * @param header      RTMP message header
	 * @param lastHeader  Previous header
	 * @return            Calculated size
	 */
	private int calculateHeaderSize(final Header header, final Header lastHeader) {
		final byte headerType = getHeaderType(header, lastHeader);
		int channelIdAdd = 0;
		int channelId = header.getChannelId();
		if (channelId > 320) {
			channelIdAdd = 2;
		} else if (channelId > 63) {
			channelIdAdd = 1;
		}
		return RTMPUtils.getHeaderLength(headerType) + channelIdAdd;
	}
	
	/**
	 * Encode RTMP header. 
	 * 
	 * @param header      RTMP message header
	 * @param lastHeader  Previous header
	 * @return            Encoded header data
	 */
	public IoBuffer encodeHeader(final Header header, final Header lastHeader) {
		final IoBuffer result = IoBuffer.allocate(calculateHeaderSize(header, lastHeader));
		encodeHeader(header, lastHeader, result);
		return result;
	}
	
	/**
	 * Determine type of header to use.
	 * 
	 * @param header      RTMP message header
	 * @param lastHeader  Previous header
	 * @return            Header type to use.
	 */
	private byte getHeaderType(final Header header, final Header lastHeader) {
		if (lastHeader == null) {
			return HEADER_NEW;
		}
		final Integer lastFullTs = lastFullTimestampWritten.get(header.getChannelId());
		if (lastFullTs == null) {
			return HEADER_NEW;
		}
		final byte headerType;
		final long diff = RTMPUtils.diffTimestamps(header.getTimer(), lastHeader.getTimer());
		final long timeSinceFullTs = RTMPUtils.diffTimestamps(header.getTimer(), lastFullTs);
		if (header.getStreamId() != lastHeader.getStreamId() || diff < 0 || timeSinceFullTs >= 250) {
			// New header mark if header for another stream
			headerType = HEADER_NEW;
		} else if (header.getSize() != lastHeader.getSize() || header.getDataType() != lastHeader.getDataType()) {
			// Same source header if last header data type or size differ
			headerType = HEADER_SAME_SOURCE;
		} else if (header.getTimer() != lastHeader.getTimer() + lastHeader.getTimerDelta()) {
			// Timer change marker if there's time gap between header time stamps
			headerType = HEADER_TIMER_CHANGE;
		} else {
			// Continue encoding
			headerType = HEADER_CONTINUE;
		}
		return headerType;
	}
	
	/**
	 * Encode RTMP header into given IoBuffer.
	 *
	 * @param header      RTMP message header
	 * @param lastHeader  Previous header
	 * @param buf         Buffer to write encoded header to
	 */
	public void encodeHeader(final Header header, final Header lastHeader, final IoBuffer buf) {
		final byte headerType = getHeaderType(header, lastHeader);
		RTMPUtils.encodeHeaderByte(buf, headerType, header.getChannelId());
		final int timer;
		switch (headerType) {
			case HEADER_NEW:
				timer = header.getTimer();
				if (timer < 0 || timer >= 0xffffff) {
					RTMPUtils.writeMediumInt(buf, 0xffffff);
				} else {
					RTMPUtils.writeMediumInt(buf, timer);
				}
				RTMPUtils.writeMediumInt(buf, header.getSize());
				buf.put(header.getDataType());
				RTMPUtils.writeReverseInt(buf, header.getStreamId());
				if (timer < 0 || timer >= 0xffffff) {
					buf.putInt(timer);
					header.setExtendedTimestamp(timer);
				}
				header.setTimerBase(timer);
				header.setTimerDelta(0);
			
				lastFullTimestampWritten.put(header.getChannelId(), timer);
				
				break;
			case HEADER_SAME_SOURCE:
				timer = (int) RTMPUtils.diffTimestamps(header.getTimer(), lastHeader.getTimer());
				if (timer < 0 || timer >= 0xffffff) {
					RTMPUtils.writeMediumInt(buf, 0xffffff);
				} else {
					RTMPUtils.writeMediumInt(buf, timer);
				}
				RTMPUtils.writeMediumInt(buf, header.getSize());
				buf.put(header.getDataType());
				if (timer < 0 || timer >= 0xffffff) {
					buf.putInt(timer);
					header.setExtendedTimestamp(timer);
				}
				header.setTimerBase(header.getTimer() - timer);
				header.setTimerDelta(timer);
				break;
			case HEADER_TIMER_CHANGE:
				timer = (int) RTMPUtils.diffTimestamps(header.getTimer(), lastHeader.getTimer());
				if (timer < 0 || timer >= 0xffffff) {
					RTMPUtils.writeMediumInt(buf, 0xffffff);
					buf.putInt(timer);
					header.setExtendedTimestamp(timer);
				} else {
					RTMPUtils.writeMediumInt(buf, timer);
				}
				header.setTimerBase(header.getTimer() - timer);
				header.setTimerDelta(timer);
				break;
			case HEADER_CONTINUE:
				timer = (int) RTMPUtils.diffTimestamps(header.getTimer(), lastHeader.getTimer());
				header.setTimerBase(header.getTimer() - timer);
				header.setTimerDelta(timer);
				if (lastHeader.getExtendedTimestamp() != 0) {
					buf.putInt(lastHeader.getExtendedTimestamp());
					header.setExtendedTimestamp(lastHeader.getExtendedTimestamp());
				}
				break;
			default:
				break;
		}
		//log.trace("CHUNK, E, {}, {}", header, headerType);
	}
	
	public IoBuffer encodeEvent(Header header, IRTMPEvent event) {
		return encodeEvent(header, null, event);
	}
	
	public IoBuffer encodeEvent(Header header, Header lastHeader, IRTMPEvent event) {
		
		IoBuffer encodedBody = null;
		
		switch(event.getDataType()) {
			case TYPE_PING:
				encodedBody = this.encodePing((Ping)event);
			break;
			
			case TYPE_CHUNK_SIZE:
				encodedBody = this.encodeChunkSize((ChunkSize)event);
		}
		
		if(encodedBody == null) return encodedBody;
		
		header.setSize(encodedBody.limit());
		
		IoBuffer eh = this.encodeHeader(header, lastHeader);
		
		IoBuffer completeData = IoBuffer.allocate(eh.limit()+encodedBody.limit());
		completeData.put(eh.array());
		completeData.put(encodedBody.array());
		
		return completeData;
	}
	
	/** {@inheritDoc} */
	public IoBuffer encodeChunkSize(ChunkSize chunkSize) {
		final IoBuffer out = IoBuffer.allocate(4);
		out.putInt(chunkSize.getSize());
		return out;
	}
}
