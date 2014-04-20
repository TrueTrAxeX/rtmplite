package org.rtmplite.main;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.rtmplite.amf.AMFArray;
import org.rtmplite.amf.AMFObject;
import org.rtmplite.connectors.BasicConnector;
import org.rtmplite.messages.Header;
import org.rtmplite.messages.Header.PacketTypes;
import org.rtmplite.messages.Message;
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

	public Launcher() {
		String rtmpUrl = "rtmp://109.72.149.120:1935/livepkgr/raw:live#54174309_23209135_20_21_59_20_4";
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
				
				BasicConnector basicConnector = new BasicConnector(connection.getSocket());
				basicConnector.connect(rtmpUrl);

				MessagesReader messagesReader = new MessagesReader(connection.getSocket());
				messagesReader.runWorker();
				
			}
			
		} catch (IOException e) {
			log.error("Failed connect...");
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		new Launcher();
	}
}
