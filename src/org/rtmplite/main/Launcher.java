package org.rtmplite.main;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.slf4j.LoggerFactory;

import com.sun.istack.internal.logging.Logger;

/**
 * Main Launcher
 * @author TrueTrAxeX
 */
public class Launcher {
	
	private org.slf4j.Logger log = LoggerFactory.getLogger(Launcher.class);
	
	public Launcher() {
		Connection connection = new Connection(new InetSocketAddress("178.162.192.218", 1935));
		
		try {
			connection.connect();
			
			try {
				Handshake handshake = new Handshake(connection.getSocket());
				handshake.doHandshake();
			} catch(Exception e) {
				log.error("Error handshake...");
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
