package org.rtmplite.main;

import java.io.IOException;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.amf.RTMPEncoder;
import org.rtmplite.amf.packets.Ping;
import org.rtmplite.connectors.BasicClient;
import org.rtmplite.connectors.BasicClient.Type;
import org.rtmplite.events.IRTMPEvent;
import org.rtmplite.messages.Constants;
import org.rtmplite.messages.Header;
import org.slf4j.LoggerFactory;

/**
 * Main Launcher
 * @author TrueTrAxeX
 */
public class Launcher implements Constants {
	
	private org.slf4j.Logger log = LoggerFactory.getLogger(Launcher.class);
	private Connection publishConnection;
	
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
				
				final SynchronizedWriter writer = publishConnection.getSynchronizedWriter();
				MessageReader messagesReader = new MessageReader(publishConnection.getSocket(), writer);
				messagesReader.runWorker();
				
			}
			
		} catch (IOException e) {
			log.error("Failed connect...");
			e.printStackTrace();
		}
	}

	public void playConnection() {
		String rtmpUrl = "rtmp://178.162.192.218:1935/livepkgr/raw:live299741_23376195_9_0_5_24_4";
		int port = 1935;
		
		final Connection connection = new Connection(rtmpUrl, port);
		
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
				
				final SynchronizedWriter writer = connection.getSynchronizedWriter();
				MessageReader messagesReader = new MessageReader(connection.getSocket(), writer);
				messagesReader.runWorker();
				
				/*MessageListener listener = new MessageListener() {
					
					@Override
					public void onMessage(Header header, IRTMPEvent event, byte type) {
						
					}
				};*/

				//messagesReader.addListener(listener);
				
				//final SynchronizedWriter publishWriter = publishConnection.getSynchronizedWriter();
				
				MessageRawListener rawListener = new MessageRawListener() {
					
					@Override
					public void onMessage(IoBuffer rawBytes, byte dataType) {
						
						if(dataType == 9 || dataType == 8 || dataType == 18 || dataType == 22) {
							System.out.println("SEND DATA TYPE: " + dataType);
							//try {
							//	//publishWriter.write(rawBytes.array());
							//} catch (IOException e) {
							//	// TODO Auto-generated catch block
							//	e.printStackTrace();
							//}
						}
						
					}
				};
				
				messagesReader.addRawListener(rawListener);
				
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
