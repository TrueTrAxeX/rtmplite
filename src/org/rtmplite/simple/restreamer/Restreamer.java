package org.rtmplite.simple.restreamer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.amf.RTMPEncoder;
import org.rtmplite.connectors.BasicClient;
import org.rtmplite.connectors.BasicClient.ConnectionParam;
import org.rtmplite.connectors.BasicClient.ParamType;
import org.rtmplite.connectors.BasicClient.TranslationType;
import org.rtmplite.connectors.BasicClient.Type;
import org.rtmplite.main.Connection;
import org.rtmplite.main.Handshake;
import org.rtmplite.main.MessageRawListener;
import org.rtmplite.main.MessageReader;
import org.rtmplite.main.SynchronizedWriter;
import org.rtmplite.messages.Constants;
import org.rtmplite.messages.Header;
import org.rtmplite.utils.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Restreamer {
	
	private long uniqueId = 0;
	
	public void setUniqueId(long uniqueId) {
		this.uniqueId = uniqueId;
	}
	
	public long getUniqueId() {
		return uniqueId;
	}
	
	private static Logger log = LoggerFactory.getLogger(Restreamer.class);
	
	private IdleTimeoutHandler idleTimeoutHandler;

	private static ExecutorService executor = Executors.newCachedThreadPool();
	
	public enum RestreamType {
		LIVE, RECORD
	}
	
	public class IdleTimeoutHandler implements Runnable {

		private int idleTimeInSecs = -1;
		
		public void cooldown() {
			this.idleTimeInSecs = 0;
		}
		
		@Override
		public void run() {
			while(state == State.CONNECTED || state == State.PENDING_CONNECTION) {
				
				if(idleTimeoutInSecs == -1) continue;
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				this.idleTimeInSecs++;
				
				//System.out.println("IDLE TIMEOUT..." + this.idleTimeInSecs);
				
				if(idleTimeoutInSecs < idleTimeInSecs) {
					try {
						disconnect();
						break;
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
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
	

	private final static int MAX_HANDSHAKE_ATTEMPTS = 3;
	
	private State state = State.NEW; // Restreamer state
	
	private List<DisconnectListener> disconnectListeners = new Vector<DisconnectListener>();
	private List<ConnectListener> connectListeners = new Vector<ConnectListener>();
	private List<FirstMediaPacketListener> firstMediaPacketListeners = new Vector<>();
	
	private boolean wasMediaPackets = false; 
	
	private int reconnectAttempts = 3;
	
	private int currentReconnectAttempt = 0;
	
	private int idleTimeoutInSecs = 60;
	
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
	
	private String publishName;

	private boolean isKilled = false;
	
	public void setPublishName(String name) {
		this.publishName = name;
	}
	
	public String getName() {
		return publishName;
	}
	
	public boolean isKilled() {
		return isKilled;
	}
	
	public void incrementReconnectAttempt() {
		currentReconnectAttempt++;
	}
	
	public int getCurrentReconnectAttempt() {
		return currentReconnectAttempt;
	}
	
	public Connection getOutputConnection() {
		return publishConnection;
	}
	
	public Connection getInputConnection() {
		return playConnection;
	}
	
	/**
	 * If media packets being received, then return true, else false
	 * @return
	 */
	public boolean wasMediaPackets() {
		return wasMediaPackets;
	}
	
	/**
	 * Get restreamer current state
	 * @return
	 */
	public State getState() {
		return state;
	}

	public Restreamer(String inputURL, int inputPort, PublishInfo publishInfo, String publishName) {
		this.inputURL = inputURL;
		this.outputURL = "rtmp://"+publishInfo.getIp()+":"+publishInfo.getPort()+"/"+publishInfo.getApp()+"/"+publishName;
	
		this.inputPort = inputPort;
		this.outputPort = publishInfo.getPort();

		this.publishName = publishName;
	}
	
	/**
	 * Get publish url
	 */
	public String getPublishUrl() {
		return this.outputURL;
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
	 * Idle timeout
	 * If restreamer don't received or send video packets a long time, then restreamer will be disconnect
	 */
	public void setIdleTimeoutInSeconds(int seconds) {
		this.idleTimeoutInSecs = seconds;
	}
	
	int currentHandshakeAttempt = 0;
	
	private Object startSynchronizer = new Object(); // Synchronizer of start method
	
	/**
	 * Start restreaming
	 * @throws StartRestreamException 
	 * @throws IOException 
	 */
	public void start() throws StartRestreamException {
		
		synchronized(startSynchronizer) {
			
			if(state == State.CONNECTED || state == State.PENDING_CONNECTION || state == State.RECONNECTING) return;
			
			first = true;
			isKilled = false;
			currentReconnectAttempt = 0;
			wasMediaPackets = false;
			
			try {
				state = State.PENDING_CONNECTION; // Set state in pending
				
				publishConnection = new Connection(outputURL, outputPort);
				
				publishConnection.connect();
				publishConnection.setAutoMutableChunkSize(false);
	
				boolean h1 = handshake(publishConnection);
				
				playConnection = new Connection(inputURL, inputPort);
				playConnection.connect();
				
				boolean h2 = handshake(playConnection);
				
				if(!h1 || !h2) {
					this.stop();
					
					if(currentHandshakeAttempt >= MAX_HANDSHAKE_ATTEMPTS) {
						currentHandshakeAttempt = 0;
						throw new StartRestreamException("Handshake error...");
					}
					
					log.warn("Handshake failed... Retrying handshake...");
					
					currentHandshakeAttempt++;
					
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					state = State.DISCONNECTED; // Set state in pending
					
					start();
					
					return;
					
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
				
				outputMessageReader = new MessageReader(publishConnection);
				outputMessageReader.runWorker();
				
				try {
					Thread.sleep(1500);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				BasicClient inputBasicClient = new BasicClient(playConnection.getSocket());
				inputBasicClient.setTranslationType(restreamTypeToTranslationType(restreamType)); // Setting restream type
				
				inputBasicClient.addConnectionParam("flashVer", new ConnectionParam("WIN 13,0,0,182", ParamType.String));
				inputBasicClient.addConnectionParam("pageUrl", new ConnectionParam("http://www.bet365.com/extra/en/streaming/live-sport/", ParamType.String));
				inputBasicClient.addConnectionParam("capabilities", new ConnectionParam("239", ParamType.String));

				for(Entry<String, ConnectionParam> entry : inputConnectionParams.entrySet()) {
					inputBasicClient.addConnectionParam(entry.getKey(), entry.getValue());
				}
				
				inputBasicClient.connect(inputURL, Type.PLAY);
				
				inputMessageReader = new MessageReader(playConnection);
				inputMessageReader.runWorker();
				
				executor.execute(new MaxRestreamerWorkingHandler());
				
				idleTimeoutHandler = new IdleTimeoutHandler();
				executor.execute(idleTimeoutHandler);
				
				new Thread() {
					public void run() {
						restreamProcess();
					}
				}.start();
				
				for(ConnectListener l : connectListeners) {
					l.onConnect(this);
				}
				
			} catch(IOException e) {
				try {
					this.disconnect();
					
					throw new StartRestreamException("Restreamer connection error... " + e.getMessage());
				} catch (IOException e1) {
					throw new StartRestreamException("Restreamer connection error... " + e.getMessage());
				}
			}
		}
	}
	
	private Map<String, ConnectionParam> inputConnectionParams = new HashMap<String, ConnectionParam>();
	
	public void addInputConnectionParam(String name, ConnectionParam param) {
		if(param == null) {
			inputConnectionParams.remove(name);
			return;
		}
		inputConnectionParams.put(name, param);
	}
	
	private boolean first = true; 
	
	private void restreamProcess() {
		inputMessageReader.addRawListener(new MessageRawListener() {
			
			private SynchronizedWriter writer = publishConnection.getSynchronizedWriter();
			private RTMPEncoder encoder = new RTMPEncoder();
			
			@Override
			public void onMessage(IoBuffer rawBytes, byte dataType) {
				
				if(dataType == Constants.TYPE_CHUNK_SIZE && first == true) {
					Header h = new Header();
					h.setDataType(Constants.TYPE_CHUNK_SIZE);
					h.setChannelId((byte)3);
					
					try {
						byte[] sBytes = encoder.encodeEvent(h, playConnection.getChunkSize()).array();
						writer.write(sBytes);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					first = false;
				}
				
				if(dataType == 9 || dataType == 8 || dataType == 22 || dataType == 18) {
					
					if(!wasMediaPackets) {
						state = State.CONNECTED; // Set state in sucessfull connection
						
						for(FirstMediaPacketListener l : firstMediaPacketListeners) {
							l.onPacket(Restreamer.this);
						}
						
						wasMediaPackets = true;
					}
						
					try {
						if(dataType == 18) {
							byte[] data = Converter.onMetaDataToSetDataFrame(rawBytes);
							
							if(data != null) {
								writer.write(data);
								return;
							} else {
								return;
							}
						}
						
						writer.write(rawBytes.array());
					} catch (IOException e) {
						try {
							disconnect();
						} catch (IOException e1) {
						}
					}
					
					if(idleTimeoutHandler != null) idleTimeoutHandler.cooldown();
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
	
	public synchronized void disconnect() throws IOException {
		
		if(state != State.DISCONNECTED && state != State.NEW) { 
		
			state = State.DISCONNECTED;
			
			if(playConnection != null)
			playConnection.disconnect();
			
			if(publishConnection != null)
			publishConnection.disconnect();
			
			for(DisconnectListener l : disconnectListeners) {
				l.onDisconnect(this);
			}
		}
	}
	
	/**
	 * Stop restreaming
	 * @throws IOException 
	 */
	public void stop() throws IOException {
		isKilled = true;
		this.disconnect();
	}
	
	/**
	 * Set count reconnect attempts after lost connection...
	 * @param attempts 
	 */
	public void setMaxReconnectAttempts(int attempts) {
		reconnectAttempts = attempts;
	}
	
	/**
	 * Get count reconnect attempts after lost connection...
	 * @param listener
	 */
	public int getMaxReconnectAttempts() {
		return reconnectAttempts;
	}
	
	public void addDisconnectListener(DisconnectListener listener) {
		disconnectListeners.add(listener);
	}
	
	public void addConnectListener(ConnectListener listener) {
		connectListeners.add(listener);
	}
	
	public void addFirstMediaPacketListener(FirstMediaPacketListener listener) {
		firstMediaPacketListeners.add(listener);
	}
	
	private boolean handshake(Connection connection) {
		try {
			Handshake handshake = new Handshake(connection.getSocket());
			handshake.doHandshake();
			
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		
		if(obj instanceof Restreamer) {
			Restreamer rs = (Restreamer) obj;
			
			if(rs.getUniqueId() == rs.getUniqueId()) {
				return true;
			}
		} else {
			return false;
		}
		
		return false;
	}

	private Object commonInfo;
	
	public void setCommonInfo(Object commonInfo) {
		this.commonInfo = commonInfo;
	}
	
	public Object getCommonInfo() {
		return commonInfo;
	}
}
