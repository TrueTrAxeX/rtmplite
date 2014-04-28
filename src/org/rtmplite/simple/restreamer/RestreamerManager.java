package org.rtmplite.simple.restreamer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

public class RestreamerManager {
	
	public class DisconnectedRestereamersCleaner implements Runnable {
		
		private static final int interval = 4460000; // 2 hours (default)

		@Override
		public void run() {
			while(disconnectedRestreamersCleanerWorking) {
				
				for(int i=0; i<storage.size(); i++) {
					
					Restreamer r = storage.get(i);
					
					if(r.getState() == State.DISCONNECTED) {
						storage.remove(i);
					}
				}
				
				try {
					Thread.sleep(interval);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public class AutoReconnecter implements Runnable {

		private static final int interval = 3000;
		
		@Override
		public void run() {
			while(autoReconnectorWorking) {
				
				for(Restreamer r : storage) {
					if(r.getState() == State.DISCONNECTED && !r.isKilled()) {
						try {
							
							if(r.getCurrentReconnectAttempt() > r.getMaxReconnectAttempts()) continue;
							
							r.stop();
							
							r.incrementReconnectAttempt();
							System.out.println("RECONNECTING!!!");
							r.start();
							
						} catch (StartRestreamException | IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				
				try {
					Thread.sleep(interval);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	private boolean disconnectedRestreamersCleanerWorking = false; 
	private boolean autoReconnectorWorking = false;
	private Thread disconnectedRestreamersCleanerThread;
	
	/**
	 * Disconnected resteramers cleaner start
	 */
	public void startDisconnectedRestereamersCleaner() {
		if(!this.disconnectedRestreamersCleanerWorking) { 
			this.disconnectedRestreamersCleanerWorking = true;
			
			disconnectedRestreamersCleanerThread = new Thread(new DisconnectedRestereamersCleaner());
			disconnectedRestreamersCleanerThread.start();
		}
	}
	
	/**
	 * Disconnected restreamers cleaner stop
	 */
	public void stopDisconnectedRestereamersCleaner() {
		this.disconnectedRestreamersCleanerWorking = false;
		disconnectedRestreamersCleanerThread.interrupt();
	}
	
	/**
	 * Stop auto reconnector worker
	 */
	public void stopAutoReconnector() {
		this.autoReconnectorWorking = false;
	}
	
	/**
	 * Start auto reconnector worker
	 */
	public void startAutoReconnector() {
		
		if(!this.autoReconnectorWorking) {
			this.autoReconnectorWorking = true;
			
			new Thread(new AutoReconnecter()).start();
		}
	}
	
	/**
	 * First init manager
	 */
	public void init() {
		startAutoReconnector();
		startDisconnectedRestereamersCleaner();
	}
	
	private RestreamerManager() {}
	
	private static RestreamerManager instance;
	
	public static RestreamerManager getInstance() {
		if(instance == null) instance = new RestreamerManager();
		
		return instance;
	}
	
	private static final List<Restreamer> storage = new CopyOnWriteArrayList<Restreamer>();
	
	/**
	 * Add new restreamer for manager
	 * @param restreamer
	 */
	public void add(Restreamer restreamer) {
		
		/*for(Restreamer rs : storage) {
			if(rs.equals(restreamer)) {
				try {
					rs.disconnect();
					storage.remove(rs);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}*/
		
		storage.add(restreamer);
	}
	
	/**
	 * Kill restreamer by object
	 * @param restreamer
	 * @throws IOException
	 */
	public void kill(Restreamer restreamer) throws IOException {
		restreamer.stop();
		storage.remove(restreamer);
	}
	
	/**
	 * Kill restreamer by index
	 * @param index
	 * @throws IOException
	 */
	public void kill(int index) throws IOException {
		storage.get(index).stop();
		
		storage.remove(index);
	}
	
	/**
	 * Get all restreamers
	 * @return
	 */
	public List<Restreamer> getAll() {
		return storage;
	}
	
	/**
	 * Kill restreamer by name
	 * @throws IOException 
	 */
	public Restreamer killByName(String name) throws IOException {
		for(int i=0; i<storage.size(); i++) {
			
			Restreamer r = storage.get(i);
			
			if(r.getName().equals(name)) {
				r.stop();
				storage.remove(i);
				return r;
			}
		}
		
		return null;
	}
	
	/**
	 * Kill restreamer by unique id
	 * @throws IOException 
	 */
	public Restreamer killByUniqueId(Long uniqueId) throws IOException {
		for(int i=0; i<storage.size(); i++) {
			
			Restreamer r = storage.get(i);
			
			if(r.getUniqueId() == uniqueId) {
				r.stop();
				storage.remove(i);
				return r;
			}
		}
		
		return null;
	}
}
