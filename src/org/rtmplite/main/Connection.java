package org.rtmplite.main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Connection {
	
	private InetSocketAddress inetAddress;
	
	private Socket socket = new Socket();

	private SynchronizedWriter writer;
	
	public Connection(String url, int port) {
		
		Pattern pattern = Pattern.compile("rtmp://(.*?)(:[0-9]+)?/(.*?)[/]+?(.*?)");
		Matcher matcher = pattern.matcher(url);
		
		if(matcher.find()) { 
			this.inetAddress = new InetSocketAddress(matcher.group(1), port);
		} else {
			throw new RuntimeException("Error connection params...");
		}
	}
	
	/**
	 * Connect with RTMP server
	 * @throws IOException Failed connect...
	 */
	public void connect() throws IOException {
		socket.connect(inetAddress);

		
		try {
			writer = new SynchronizedWriter(socket.getOutputStream());
		} catch (IOException e) {
			writer = null;
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Disconnect from server
	 * @throws IOException
	 */
	public void disconnect() throws IOException {
		socket.close();
	}
	
	/**
	 * Get socket
	 * @return
	 */
	public Socket getSocket() {
		return socket;
	}
	
	/**
	 * Get syncronized writer
	 */
	public SynchronizedWriter getSynchronizedWriter() {
		return writer;
	}
}
