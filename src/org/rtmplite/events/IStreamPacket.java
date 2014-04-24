package org.rtmplite.events;

import org.apache.mina.core.buffer.IoBuffer;

/**
 * Packet containing stream data.
 * 
 * @author The Red5 Project
 * @author Joachim Bauch (jojo@struktur.de)
 */
public interface IStreamPacket {

	/**
	 * Type of this packet. This is one of the <code>TYPE_</code> constants.
	 * 
	 * @return the type
	 */
	public byte getDataType();
	
	/**
	 * Timestamp of this packet.
	 * 
	 * @return the timestamp in milliseconds
	 */
	public int getTimestamp();
	
	/**
	 * Packet contents.
	 * 
	 * @return the contents
	 */
	public IoBuffer getData();
	
}