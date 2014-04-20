package org.rtmplite.main;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MessagesReader {

	private List<MessagesListener> listeners = new ArrayList<MessagesListener>();
	
	private Socket socket;
	private InputStream inputStream;
	
	public MessagesReader(Socket socket) {
		this.socket = socket;
	}
	
	public void addListener(MessagesListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(MessagesListener listener) {
		listeners.remove(listener);
	}
	
	public boolean runWorker() {
		
		try {
			this.inputStream = socket.getInputStream();
			
			new Reader().start();
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			
			return false;
		}
	}
	
	public class Reader extends Thread {
		@Override
		public void run() {
			try {
				while(true) {
					byte b = (byte) inputStream.read();
					
					System.out.println("FUCK: " + b);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
