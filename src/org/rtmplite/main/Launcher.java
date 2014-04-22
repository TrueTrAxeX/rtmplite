package org.rtmplite.main;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.proxy.utils.IoBufferDecoder;
import org.rtmplite.amf.AMFArrayEncoder;
import org.rtmplite.amf.AMFObjectEncoder;
import org.rtmplite.amf.IncomingDataParser;
import org.rtmplite.connectors.BasicClient;
import org.rtmplite.connectors.BasicClient.Type;
import org.rtmplite.messages.HeaderEncoder;
import org.rtmplite.messages.RTMPDecodeState;
import org.rtmplite.messages.HeaderEncoder.PacketTypes;
import org.rtmplite.messages.Message;
import org.rtmplite.utils.Hex;
import org.rtmplite.utils.NumberUtils;
import org.rtmplite.utils.RTMPUtils;
import org.slf4j.LoggerFactory;

import com.sun.corba.se.impl.ior.ByteBuffer;

/**
 * Main Launcher
 * @author TrueTrAxeX
 */
public class Launcher {
	
	private org.slf4j.Logger log = LoggerFactory.getLogger(Launcher.class);
	public static Connection publishConnection = null;
			
	public void publishConnection() {
		String rtmpUrl = "rtmp://83.246.186.32:1935/live/test";
		int port = 1935;
		
		publishConnection = new Connection(rtmpUrl, port);
		
		try {
			publishConnection.connect();
			
			boolean handshakeSuccess = false;
			
			try {
				Handshake handshake = new Handshake(publishConnection.getSocket());
				handshake.doHandshake();
				
				handshakeSuccess = true;
			} catch(Exception e) {
				log.error("Error handshake...");
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(handshakeSuccess) {

				BasicClient basicClient = new BasicClient(publishConnection.getSocket());
				basicClient.connect(rtmpUrl, Type.PUBLISH);
				
				//MessageReader messagesReader = new MessageReader(publishConnection.getSocket());
				//messagesReader.runWorker();
				
				new Thread() {
					public void run() {
						while(true) {

							byte[] buffer = new byte[128]; // Adjust if you want
						    int bytesRead;
						    try {
								while ((bytesRead = publishConnection.getSocket().getInputStream().read(buffer)) != -1)
								{
									
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}.start();
				
			
			}
			
		} catch (IOException e) {
			log.error("Failed connect...");
			e.printStackTrace();
		}
	}

	public void playConnection() {
		String rtmpUrl = "rtmp://178.162.192.218:1935/livepkgr/raw:live298838_23287661_10_59_17_22_4";
		int port = 1935;
		
		Connection connection = new Connection(rtmpUrl, port);
		
		try {
			connection.connect();
			
			boolean handshakeSuccess = false;
			
			try {
				Handshake handshake = new Handshake(connection.getSocket());
				handshake.doHandshake();
				
				handshakeSuccess = true;
			} catch(Exception e) {
				log.error("Error handshake...");
			}
			
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(handshakeSuccess) {

				BasicClient basicClient = new BasicClient(connection.getSocket());
				basicClient.connect(rtmpUrl, Type.PLAY);
			
				MessageReader messagesReader = new MessageReader(connection.getSocket());
				messagesReader.runWorker();
				
				MessageListener listener = new MessageListener() {
					RTMPDecodeState state = new RTMPDecodeState("1");
					IncomingDataParser incomingDataParser = new IncomingDataParser(state);
					byte[] lastBuffer;
					
					@Override
					public void onMessage(IoBuffer buffer, int bytesRead) {

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
								incomingDataParser.onData(buffer, bytesRead);
							} else {
								IoBuffer newBuffer = IoBuffer.allocate(lastBuffer.length+buffer.remaining());
								newBuffer.put(lastBuffer);
								newBuffer.put(buffer);
								newBuffer.rewind();
								
								buffer = newBuffer;
								
								incomingDataParser.onData(buffer, bytesRead);
								System.out.println("BITCH");
								
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
						
						//buffer.compact();
					}
				};
				
				messagesReader.addListener(listener);
			}
			
		} catch (IOException e) {
			log.error("Failed connect...");
			e.printStackTrace();
		}
	}

	public Launcher() {

		//publishConnection();

		//try {
		//	Thread.sleep(2000);
		//} catch (InterruptedException e) {
		//	// TODO Auto-generated catch block
		//	e.printStackTrace();
		//}

		playConnection();
		
	}
	
	public static void main(String[] args) {
		new Launcher();
	}
}
