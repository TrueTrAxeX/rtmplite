package org.rtmplite.simple.restreamer;

public abstract class DisconnectListener {
	
	/**
	 * Disconnect event
	 * @param restreamer
	 */
	public abstract void onDisconnect(Restreamer restreamer);
}
