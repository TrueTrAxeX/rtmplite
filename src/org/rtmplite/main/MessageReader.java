package org.rtmplite.main;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.amf.AMFObjectEncoder;
import org.rtmplite.amf.IncomingDataParser;
import org.rtmplite.messages.HeaderEncoder;
import org.rtmplite.messages.Message;
import org.rtmplite.utils.NumberUtils;

public class MessageReader {

	private List<MessageListener> listeners = new ArrayList<MessageListener>();
	
	private Socket socket;
	private InputStream inputStream;
	
	public MessageReader(Socket socket) {
		this.socket = socket;
	}
	
	public void addListener(MessageListener listener) {
		listeners.add(listener);
	}
	
	public void removeListener(MessageListener listener) {
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
		
		private int totalBytesRead = 0;
		
		public void copyStream(InputStream input, java.io.OutputStream output) throws IOException
		{
		    byte[] buffer = new byte[128]; // Adjust if you want
		    int bytesRead;
		    while ((bytesRead = input.read(buffer)) != -1)
		    {
		    	synchronized(this) {
			        output.write(buffer, 0, bytesRead);
			        totalBytesRead += bytesRead;
		    	}
		    }
		}
		
		@Override
		public void run() {
	
				//final java.io.OutputStream os = Launcher.publishConnection.getSocket().getOutputStream();
				
				new Thread() {
					public void run() {
						try {
							while(true) {
								try {
									Thread.sleep(10000);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								synchronized(MessageReader.this) {
									HeaderEncoder header = new HeaderEncoder();
									header.setChannelId((byte) 2);
									header.setPacketType((byte)0x3);
									
									AMFObjectEncoder amfObject = new AMFObjectEncoder();
									amfObject.addBytes(NumberUtils.intToBytes(totalBytesRead));
									
									Message message = new Message(header, amfObject);
									
									socket.getOutputStream().write(message.getRawBytes());
									socket.getOutputStream().flush();
								}
							}
							
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}.start();
				
				 
				byte[] buffer = new byte[65536]; // Adjust if you want
				
			    int bytesRead;
			    
			    try {
			    	
					while ((bytesRead = inputStream.read(buffer)) > 0)  {
						 
					     totalBytesRead += bytesRead;
					     
					     IoBuffer buff = IoBuffer.wrap(buffer, 0, bytesRead);
					     
					     for(MessageListener l : listeners) {
					    	 l.onMessage(buff, bytesRead);
						 }
					     
					     try {
							Thread.sleep(1000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}
}
