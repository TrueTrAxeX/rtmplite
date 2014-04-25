package org.rtmplite.amf;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.amf.packets.ChunkSize;
import org.rtmplite.amf.packets.Ping;
import org.rtmplite.amf.packets.SWFResponse;
import org.rtmplite.amf.packets.SetBuffer;
import org.rtmplite.events.IRTMPEvent;
import org.rtmplite.main.Connection;
import org.rtmplite.main.MessageListener;
import org.rtmplite.main.MessageRawListener;
import org.rtmplite.main.SynchronizedWriter;
import org.rtmplite.messages.Constants;
import org.rtmplite.messages.Header;
import org.rtmplite.messages.Packet;
import org.rtmplite.utils.BufferUtils;
import org.rtmplite.utils.ChunksUtils;
import org.rtmplite.utils.RTMPUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XRTMPDecoder implements Constants {
	
	private Executor executor = Executors.newSingleThreadExecutor();
	private Executor executor2 = Executors.newSingleThreadExecutor();
	
	private static Logger log = LoggerFactory.getLogger(XRTMPDecoder.class);
	
	private final int TIMEOUT = 5000;

	private Map<Integer, Header> lastHeaders = new HashMap<Integer, Header>();
	private Map<Integer, Packet> lastPackets = new HashMap<Integer, Packet>();
	
	private List<MessageListener> listeners;
	private List<MessageRawListener> rawListeners;
	
	private InputStream inputStream;
	private SynchronizedWriter writer;
	
	private RTMPEncoder encoder = new RTMPEncoder();
	
	private Connection connection;
	
	public XRTMPDecoder(Connection connection, InputStream inputStream, SynchronizedWriter writer, List<MessageListener> listeners, List<MessageRawListener> rawListeners) {
		this.inputStream = inputStream;
		this.writer = writer;
		
		this.listeners = listeners;
		this.rawListeners = rawListeners;
		this.connection = connection;
	}
	
	public byte get() throws IOException {
		
		//long timeoutPoint = System.currentTimeMillis() + TIMEOUT;
		
		/*while(inputStream.available() < 1) {
			try {
				Thread.sleep(10);
				
				if(timeoutPoint < System.currentTimeMillis()) {
					System.out.println("TIMEOUT!");
					throw new IOException("Read timeout...");
				}
			} catch (InterruptedException e) {
				throw new IOException("Read timeout...");
			}
		}*/
		
		return (byte) inputStream.read();
	}
	
	public byte[] get(int amount) throws IOException {
		
		long timeoutPoint = System.currentTimeMillis() + TIMEOUT;
		
		byte[] buffer = new byte[amount];
		
		while(inputStream.available() < amount) {
			try {
				Thread.sleep(1);
				
				if(timeoutPoint < System.currentTimeMillis()) {
					throw new IOException("Read timeout...");
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		inputStream.read(buffer);
		
		return buffer;
	}
	
	private IoBuffer createRawPacketData(int chunkSize, Header header, IoBuffer buf, int channelId) {
		Header cHeader = header.clone();
		
		//if(numChunks > 0) {
		//	cHeader.setSize(cHeader.getSize()+numChunks-1);
		//}
		
		byte[] hArr = encoder.encodeHeader(cHeader, null).array();
		byte[] bArr = ChunksUtils.splitOnChunks(chunkSize, buf, (byte) channelId);
	
		//byte[] tArr = buf.array();
		
		//for(int i=0; i<bArr.length; i++) {
		//	bArr[i] = tArr[i];
		//}
		
		IoBuffer ioNew = IoBuffer.allocate(hArr.length+bArr.length);
		ioNew.put(hArr);
		ioNew.put(bArr);
		
		return ioNew;
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
		
			IoBuffer headerBuf = IoBuffer.wrap(totalBuffer.array(), 0, totalBuffer.limit());
		
			final Header header = decodeHeader(headerBuf, lastHeader);
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
			final int chunkSize = connection.getChunkSize().getSize();
			final int readAmount = (readRemaining > chunkSize) ? chunkSize : readRemaining;
		
			BufferUtils.put(buf, IoBuffer.wrap(get(readAmount)), readAmount);
			
			if (buf.position() < header.getSize()) {
				//state.continueDecoding();
				continue;
			}
			
			if (buf.position() > header.getSize()) {
				System.out.println("Packet size expanded from {} to {} ({})");
			}
			
			final IoBuffer rawPacketData = createRawPacketData(4096, header.clone(), buf, channelId);
			
			executor2.execute(new Runnable() {
				
				@Override
				public void run() {
					for(MessageRawListener l : rawListeners) {
						l.onMessage(rawPacketData, header.getDataType());
					}
				}
				
			});
			
			///System.out.println("ƒ≈ Œƒ»–Œ¬¿ÕŒ!");
			//System.out.println("CHANNEL: " + channelId);
			//System.out.println("DATA TYPE: " + header.getDataType());
			lastPackets.put(channelId, null);
		
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
							
							//Ping p = new Ping(Ping.PING_CLIENT);
							//Header hd = new Header();
							//hd.setDataType((byte)TYPE_PING);
							//writer.write(encoder.encodeEvent(hd, p).array());
							
							System.out.println("PING TYPE: " + ping.getEventType());
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
							
							if(connection.isAutoMutableChunkSize()) {
								connection.setChunkSize(chunkSIZE);
							}
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
	}
	
	IRTMPEvent message = null;
	
	private IRTMPEvent decodeMessage(final Header header, IoBuffer in) {
		
		in.rewind();
		
		byte dataType = header.getDataType();
	
		switch (dataType) {
			case TYPE_PING:
				message = decodePing(in);
			break;
			
			case TYPE_CHUNK_SIZE:
				message = decodeChunkSize(in);
			break;
			
			//case TYPE_NOTIFY:
				
			//break;
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
}
