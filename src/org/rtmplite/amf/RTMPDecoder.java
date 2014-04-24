package org.rtmplite.amf;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.amf.packets.ChunkSize;
import org.rtmplite.amf.packets.Notify;
import org.rtmplite.amf.packets.Ping;
import org.rtmplite.amf.packets.SWFResponse;
import org.rtmplite.amf.packets.SetBuffer;
import org.rtmplite.events.IRTMPEvent;
import org.rtmplite.main.MessageListener;
import org.rtmplite.main.MessageRawListener;
import org.rtmplite.main.SynchronizedWriter;
import org.rtmplite.messages.Constants;
import org.rtmplite.messages.Header;
import org.rtmplite.messages.Packet;
import org.rtmplite.messages.RTMPDecodeState;
import org.rtmplite.red5.trash.amf.io.AMF;
import org.rtmplite.red5.trash.amf.io.DataTypes;
import org.rtmplite.red5.trash.amf.io.IInput;
import org.rtmplite.red5.trash.amf.io.Input;
import org.rtmplite.utils.BufferUtils;
import org.rtmplite.utils.RTMPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RTMPDecoder implements Constants {
	
	private Executor executor = Executors.newSingleThreadExecutor();
	private Executor executor2 = Executors.newSingleThreadExecutor();
	
	private int globalChunkSize = 128;
	
	// protection for the decoder when using multiple threads per connection
	public static Semaphore decoderLock = new Semaphore(1, true);
	
	private static Logger log = LoggerFactory.getLogger(RTMPDecoder.class);
	
	private Map<Integer, Header> lastHeaders = new HashMap<Integer, Header>();
	private Map<Integer, Packet> lastPackets = new HashMap<Integer, Packet>();
	
	private List<MessageListener> listeners;
	private List<MessageRawListener> rawListeners;
	
	public Map<Integer, Header> getLastHeaders() {
		return lastHeaders;
	}
	
	private SynchronizedWriter writer;
	private RTMPEncoder encoder;
	
	private RTMPDecodeState state;
	
	public RTMPDecoder(SynchronizedWriter writer, RTMPDecodeState state, List<MessageListener> listeners, List<MessageRawListener> rawListeners) {
		this.state = state;
		this.listeners = listeners;
		this.rawListeners = rawListeners;
		this.writer = writer;
		this.encoder = new RTMPEncoder();
		
	}
	
	public void onData(IoBuffer in, int bytesRead, InputStream inputStream) {
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
		headerLength += byteCount - 1;
		
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
		
		//final Header oldHeader = header.clone(); 
		
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
			
			try {
				while(inputStream.available() < readAmount) {
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
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
		
		executor2.execute(new Runnable() {
		
			@Override
			public void run() {
				for(MessageRawListener l : rawListeners) {
					
					byte[] hArr = encoder.encodeHeader(header, null).array();
					byte[] bArr = new byte[buf.limit()];
					
					byte[] tArr = buf.array();
					
					for(int i=0; i<bArr.length; i++) {
						bArr[i] = tArr[i];
					}
					
					IoBuffer ioNew = IoBuffer.allocate(hArr.length+bArr.length);
					ioNew.put(hArr);
					ioNew.put(bArr);

					l.onMessage(ioNew, header.getDataType());
				}
			}
			
		});

		
		buf.flip();
		
		try {
			final IRTMPEvent message = decodeMessage(packet.getHeader(), buf);
			//message.setHeader(packet.getHeader());
			// Unfortunately flash will, especially when resetting a video stream with a new key frame, sometime 
			// send an earlier time stamp.  To avoid dropping it, we just give it the minimal increment since the 
			// last message.  But to avoid relative time stamps being mis-computed, we don't reset the header we stored.
			//final Header lastReadHeader = lastHeaders.get(channelId);
			
			//lastHeaders.put(channelId, packet.getHeader());
			//packet.setMessage(message);
			
			// collapse the time stamps on the last packet so that it works right for chunk type 3 later
			lastHeader = lastHeaders.get(channelId);
			lastHeader.setTimerBase(header.getTimer());
		
			if(message != null) {
				switch(header.getDataType()) {
					case TYPE_PING:
						Ping ping = (Ping) message;
						
						if(ping.getEventType() == Ping.PING_CLIENT) {
							try {
								Ping pong = new Ping(Ping.PONG_SERVER);
								pong.setTimestamp((int) (System.currentTimeMillis() & 0xffffffff));
								
								writer.write(encoder.encodeEvent(header, pong).array());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
					break;
					
					case TYPE_CHUNK_SIZE:
						ChunkSize chunkSIZE = (ChunkSize) message;
						
						globalChunkSize = chunkSIZE.getSize();
						
					break;
				}
			}
			
			//System.out.println("HEADER SIZE: " + header.getSize());
			//System.out.println("DATA TIME: " + header.getDataType());
			//System.out.println("CHANNEL ID: " + header.getChannelId());
			//System.out.println("TIMESTAMP: " + header.getTimer());
		} finally {
			lastPackets.put(channelId, null);
		}
		
	}
	
	IRTMPEvent message = null;
	
	private IRTMPEvent decodeMessage(final Header header, IoBuffer in) {
		
		byte dataType = header.getDataType();
	
		switch (dataType) {
			case TYPE_PING:
				message = decodePing(in);
			break;
			
			case TYPE_CHUNK_SIZE:
				message = decodeChunkSize(in);
			break;
		}
		
		if(message != null) {
			
			executor.execute(new Runnable() {

				@Override
				public void run() {
					for(MessageListener l : listeners) {
						l.onMessage(header, message, header.getDataType());
					}
				}
				
			});
			
			return message;
		} else {
			return null;
		}
	}

	/**
	 * Decodes packet header.
	 * 
	 * @param in Input IoBuffer
	 * @param lastHeader Previous header
	 * @return Decoded header
	 */
	public static Header decodeHeader(IoBuffer in, Header lastHeader) {
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
	
	/**
	 * Decodes ping event.
	 * 
	 * @param in IoBuffer
	 * @return Ping event
	 */
	public static Ping decodePing(IoBuffer in) {
		Ping ping = null;
		if (log.isTraceEnabled()) {
			// gets the raw data as hex without changing the data or pointer
			String hexDump = in.getHexDump();
			log.trace("Ping dump: {}", hexDump);
		}
		// control type
		short type = in.getShort();
		switch (type) {
			case Ping.CLIENT_BUFFER:
				ping = new SetBuffer(in.getInt(), in.getInt());
			break;
			
			case Ping.PING_SWF_VERIFY:
				// only contains the type (2 bytes)
				ping = new Ping(type);
				break;
			case Ping.PONG_SWF_VERIFY:
				byte[] bytes = new byte[42];
				in.get(bytes);
				ping = new SWFResponse(bytes);
				break;
			default:
				//STREAM_BEGIN, STREAM_PLAYBUFFER_CLEAR, STREAM_DRY, RECORDED_STREAM
				//PING_CLIENT, PONG_SERVER
				//BUFFER_EMPTY, BUFFER_FULL
				ping = new Ping(type, in.getInt());
				break;
		}
		return ping;
	}

	/** {@inheritDoc} */
	public static ChunkSize decodeChunkSize(IoBuffer in) {
		int chunkSize = in.getInt();
		log.debug("Decoded chunk size: {}", chunkSize);
		return new ChunkSize(chunkSize);
	}
	
	public enum Encoding {
		AMF0, AMF3
	}
	
	/**
	 * Decodes stream meta data, to include onMetaData, onCuePoint, and onFI.
	 * 
	 * @param in
	 * @return Notify
	 */
	@SuppressWarnings("unchecked")
	public static Notify decodeStreamMetadata(IoBuffer in) {
		Encoding encoding = Encoding.AMF0;
		
		IInput input = null;

		// check to see if the encoding is set to AMF3. 
		// if it is then check to see if first byte is set to AMF0
		byte amfVersion = 0x00;
		if (encoding == Encoding.AMF3) {
			amfVersion = in.get();
		}
		
		// reset the position back to 0
		in.position(0);
		
		//make a pre-emptive copy of the incoming buffer here to prevent issues that occur fairly often
		IoBuffer copy = in.duplicate();
		
		
		if (encoding == Encoding.AMF0 || amfVersion != AMF.TYPE_AMF3_OBJECT ) {
			input = new Input(copy);
		}
		//get the first datatype
		byte dataType = input.readDataType();
		if (dataType == DataTypes.CORE_STRING) {
			String setData = input.readString(String.class);
			if ("@setDataFrame".equals(setData)) {
				// get the second datatype
				byte dataType2 = input.readDataType();
				log.debug("Dataframe method type: {}", dataType2);
				String onCueOrOnMeta = input.readString(String.class);
				// get the params datatype
				byte object = input.readDataType();
				log.debug("Dataframe params type: {}", object);
				Map<Object, Object> params;
				if (object == DataTypes.CORE_MAP) {
					// the params are sent as a Mixed-Array. Required to support the RTMP publish provided by ffmpeg/xuggler
					params = (Map<Object, Object>) input.readMap(null);
				} else {
					// read the params as a standard object
					params = (Map<Object, Object>) input.readObject(Object.class);
				}
				log.debug("Dataframe: {} params: {}", onCueOrOnMeta, params.toString());

				IoBuffer buf = IoBuffer.allocate(1024);
				buf.setAutoExpand(true);
				//Output out = new Output(buf);
				//out.writeString(onCueOrOnMeta);
				//out.writeMap(params);

				buf.flip();
				return new Notify(buf);
			} else if ("onFI".equals(setData)) {
				// the onFI request contains 2 items relative to the publishing client application
				// sd = system date (12-07-2011)
				// st = system time (09:11:33.387)
				byte object = input.readDataType();
				log.debug("onFI params type: {}", object);
				Map<Object, Object> params;
				if (object == DataTypes.CORE_MAP) {
					// the params are sent as a Mixed-Array
					params = (Map<Object, Object>) input.readMap(null);
				} else {
					// read the params as a standard object
					params = (Map<Object, Object>) input.readObject(Object.class);
				}
				log.debug("onFI params: {}", params.toString());
			} else {
				log.info("Unhandled request: {}", setData);
				if (log.isDebugEnabled()) {
					byte object = input.readDataType();
					log.debug("Params type: {}", object);
					if (object == DataTypes.CORE_MAP) {
						Map<Object, Object> params = (Map<Object, Object>) input.readMap(null);
						log.debug("Params: {}", params.toString());
					} else {
						log.debug("The unknown request was did not provide a parameter map");
					}
				}
			}
		}
		return new Notify(in.asReadOnlyBuffer());
	}

}
