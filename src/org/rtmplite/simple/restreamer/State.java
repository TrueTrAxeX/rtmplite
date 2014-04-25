package org.rtmplite.simple.restreamer;

/**
 * Restreamer current state
 * @author blade-x
 */
public enum State {
	CONNECTED, // Restreamer connected
	DISCONNECTED, // Restreamer disconnected
	PENDING_CONNECTION, // Waiting for connection...
	NEW, // Have not run...
	RECONNECTING // Reconnect to server
}
