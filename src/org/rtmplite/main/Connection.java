package org.rtmplite.main;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Connection {
	
	private InetSocketAddress inetAddress;
	
	private Socket socket = new Socket();

	public Connection(InetSocketAddress inetSocketAddress) {
		this.inetAddress = inetSocketAddress;
	}
	
	/**
	 * Connect with RTMP server
	 * @throws IOException Failed connect...
	 */
	public void connect() throws IOException {
		socket.connect(inetAddress);
	}
	
	/**
	 * Get socket
	 * @return
	 */
	public Socket getSocket() {
		return socket;
	}
}
