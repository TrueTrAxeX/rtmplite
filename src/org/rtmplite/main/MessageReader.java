package org.rtmplite.main;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.amf.AMFObjectEncoder;
import org.rtmplite.amf.RTMPDecoder;
import org.rtmplite.amf.XRTMPDecoder;
import org.rtmplite.messages.HeaderEncoder;
import org.rtmplite.messages.Message;
import org.rtmplite.messages.RTMPDecodeState;
import org.rtmplite.utils.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.xml.internal.fastinfoset.Encoder;

public class MessageReader {

	private List<MessageListener> listeners = new ArrayList<MessageListener>();
	private List<MessageRawListener> rawListeners = new ArrayList<MessageRawListener>();
	
	private Connection connection;
	private SynchronizedWriter writer;
	private InputStream inputStream;
	
	public MessageReader(Connection connection) {
		this.connection = connection;
		this.writer = connection.getSynchronizedWriter();
	}
	
	public void addRawListener(MessageRawListener listener) {
		rawListeners.add(listener);
	}
	
	public void addListener(MessageListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(MessageListener listener) {
		listeners.remove(listener);
	}
	
	public boolean runWorker() {
		
		try {
			this.inputStream = connection.getSocket().getInputStream();
			
			Reader reader = new Reader();
			reader.setName("Reader thread");
			
			reader.start();
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			
			return false;
		}
	}

	public class Reader extends Thread {
		
		private int totalBytesRead = 0;
		
		@Override
		public void run() {
			/*new Thread() {
				public void run() {
					try {
						while(true) {
							try {
								Thread.sleep(20000);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
							HeaderEncoder header = new HeaderEncoder();
							header.setChannelId((byte) 2);
							header.setPacketType((byte)0x3);
							
							AMFObjectEncoder amfObject = new AMFObjectEncoder();
							amfObject.addBytes(NumberUtils.intToBytes(totalBytesRead));
							
							Message message = new Message(header, amfObject);
							
							writer.write(message.getRawBytes());
						}
						
					} catch (IOException e) {}
				}
			}.start();*/
			
			//byte[] buf = new byte[65536]; // Adjust if you want
			
			//Logger log = LoggerFactory.getLogger(MessageReader.class);
			
			//RTMPDecodeState state = new RTMPDecodeState("1");
			
			//RTMPDecoder rtmpDecoder = new RTMPDecoder(MessageReader.this.writer, state, listeners, rawListeners);
			//byte[] lastBuffer = null;
			
			//int bytesRead;

			try {
				XRTMPDecoder decoder = new XRTMPDecoder(connection, inputStream, writer, listeners, rawListeners);

				decoder.process();
				
				/*if(1 == 1) return;
				
				while ((bytesRead = inputStream.read(buf)) > 0) {
					
					System.out.println("BYTES READ: " + bytesRead);
					
					totalBytesRead += bytesRead;

					IoBuffer buffer = IoBuffer.allocate(bytesRead);
					
					buffer.put(buf, 0, bytesRead);
					buffer.rewind();
					
					while(buffer.hasRemaining()) {
						final int remaining = buffer.remaining();
						
						if (state.canStartDecoding(remaining)) {
							log.trace("Can start decoding");
							state.startDecoding();
						} else {
							log.trace("Cannot start decoding");
							break;
						}
						
						if(lastBuffer == null) {
							rtmpDecoder.onData(buffer, bytesRead, inputStream);
						} else {
							IoBuffer newBuffer = IoBuffer.allocate(lastBuffer.length+buffer.remaining());
							newBuffer.put(lastBuffer);
							newBuffer.put(buffer);
							newBuffer.rewind();
							
							buffer = newBuffer;
						
							rtmpDecoder.onData(buffer, bytesRead, inputStream);
						
							lastBuffer = null;
						}
						
						if (state.hasDecodedObject()) {
							log.trace("Has decoded object");
							//if (decodedObject != null) {
							//	result.add(decodedObject);
							//}
						} else if (state.canContinueDecoding()) {
							log.trace("Can continue decoding");
							continue;
						} else {
							log.trace("Cannot continue decoding");
							if(buffer.remaining() < state.getDecoderBufferAmount()) {
						
								int pos = 0;
								
								lastBuffer = new byte[buffer.remaining()];
								
								for(int i=buffer.position(); i<buffer.limit(); i++) {
									lastBuffer[pos++] = buffer.get(i);
								}
								
							}
							break;
						}
					}

				}
				 */
			} catch (IOException e) {
				
				try {
					connection.disconnect();
				} catch (IOException e1) {}
				
				return;
			}
		}
	}
}
