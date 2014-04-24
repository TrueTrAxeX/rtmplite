package org.rtmplite.simple.restreamer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.connectors.BasicClient;
import org.rtmplite.connectors.BasicClient.TranslationType;
import org.rtmplite.connectors.BasicClient.Type;
import org.rtmplite.main.Connection;
import org.rtmplite.main.Handshake;
import org.rtmplite.main.MessageRawListener;
import org.rtmplite.main.MessageReader;
import org.rtmplite.main.SynchronizedWriter;

public class Restreamer {
	
	private static ExecutorService executor = Executors.newCachedThreadPool();
	
	public enum RestreamType {
		LIVE, RECORD
	}
	
	public class MaxRestreamerWorkingHandler implements Runnable {

		private long startTime = System.currentTimeMillis();
		
		public void setStartTime(long startTime) {
			this.startTime = startTime;
		}
		
		@Override
		public void run() {
			while(state == State.CONNECTED) {
				
				if(maxRestreamTimeInSecs == -1) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					continue;
				}
				
				if((startTime + (maxRestreamTimeInSecs*1000)) < System.currentTimeMillis())  {
					try {
						disconnect();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					return;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	private State state = State.NEW; // Restreamer state
	
	private List<DisconnectListener> disconnectListeners = new ArrayList<DisconnectListener>();
	
	private int reconnectAttempts = 3;
	
	private String inputURL; // Input url
	private String outputURL; // Output url
	
	private int inputPort = 1935; // Input port
	private int outputPort = 1935; // Output port
	
	private Connection playConnection = null;
	private Connection publishConnection = null;
	
	private MessageReader inputMessageReader = null;
	private MessageReader outputMessageReader = null;
	
	private int maxRestreamTimeInSecs = -1; // Maximal restream time in seconds

	private RestreamType restreamType = RestreamType.LIVE; // default live
	
	public Restreamer(String inputURL, int inputPort, String outputURL, int outputPort) {
		this.inputURL = inputURL;
		this.outputURL = outputURL;
	
		this.inputPort = inputPort;
		this.outputPort = outputPort;
	}
	
	public Restreamer(String inputURL, String outputURL) {
		this.inputURL = inputURL;
		this.outputURL = outputURL;
		
		this.inputPort = 1935;
		this.outputPort = 1935;
	}
	
	/**
	 * Setting restream type
	 */
	public void setRestreamType(RestreamType type) {
		this.restreamType  = type;
	}
	
	/**
	 * Setting max restream time
	 */
	public void setMaxRestreamTimeInSeconds(int seconds) {
		this.maxRestreamTimeInSecs = seconds;
	}
	
	/**
	 * Start restreaming
	 * @throws StartRestreamException 
	 * @throws IOException 
	 */
	public void start() throws StartRestreamException {
		
		try {
			state = State.PENDING_CONNECTION; // Set state in pending
			
			publishConnection = new Connection(outputURL, outputPort);
			
			publishConnection.connect();

			boolean h1 = handshake(publishConnection);

			playConnection = new Connection(inputURL, inputPort);
			
			playConnection.connect();

			boolean h2 = handshake(playConnection);
			
			if(!h1 || !h2) {
				this.disconnect();
				throw new StartRestreamException("Handshake error...");
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			BasicClient outputBasicClient = new BasicClient(publishConnection.getSocket());
			outputBasicClient.setTranslationType(restreamTypeToTranslationType(restreamType)); // Setting restream type
			outputBasicClient.connect(outputURL, Type.PUBLISH);
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			outputMessageReader = new MessageReader(publishConnection);
			outputMessageReader.runWorker();
			
			
			BasicClient inputBasicClient = new BasicClient(playConnection.getSocket());
			inputBasicClient.setTranslationType(restreamTypeToTranslationType(restreamType)); // Setting restream type
			inputBasicClient.connect(inputURL, Type.PLAY);
			
			inputMessageReader = new MessageReader(playConnection);
			inputMessageReader.runWorker();
			
			state = State.CONNECTED; // Set state in sucessfull connection
			
			executor.execute(new MaxRestreamerWorkingHandler());
			
			new Thread() {
				public void run() {
					restreamProcess();
				}
			}.start();
			
		} catch(IOException e) {
			try {
				this.disconnect();
				
				throw new StartRestreamException("Restreamer connection error... " + e.getMessage());
			} catch (IOException e1) {
				throw new StartRestreamException("Restreamer connection error... " + e.getMessage());
			}
		}
	}
	
	private void restreamProcess() {
		inputMessageReader.addRawListener(new MessageRawListener() {
			
			private SynchronizedWriter writer = publishConnection.getSynchronizedWriter();
			
			@Override
			public void onMessage(IoBuffer rawBytes, byte dataType) {
				
				if(dataType == 9 || dataType == 8 || dataType == 22 || dataType == 18) {
					try {
						writer.write(rawBytes.array());
					} catch (IOException e) {
						try {
							disconnect();
						} catch (IOException e1) {
						}
					}
				}
			}
		});
	}
	
	private TranslationType restreamTypeToTranslationType(RestreamType restreamType) {
		
		switch(restreamType) {
			case LIVE:
				return TranslationType.LIVE;
			
			case RECORD:
				return TranslationType.RECORD;
		}
		
		return null;
	}
	
	public void disconnect() throws IOException {
		
		state = State.DISCONNECTED;
		
		if(playConnection != null)
		playConnection.disconnect();
		
		if(publishConnection != null)
		publishConnection.disconnect();
	}
	
	/**
	 * Stop restreaming
	 */
	public void stop() {
		
	}
	
	/**
	 * Count reconnect attempts after lost connection...
	 * @param attempts 
	 */
	public void setReconnectAttempts(int attempts) {
		reconnectAttempts = attempts;
	}
	
	public void addDisconnectListener(DisconnectListener listener) {
		disconnectListeners.add(listener);
	}
	
	private boolean handshake(Connection connection) {
		try {
			Handshake handshake = new Handshake(connection.getSocket());
			handshake.doHandshake();
			
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
