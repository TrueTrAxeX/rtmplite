package org.rtmplite.messages;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.apache.mina.core.buffer.IoBuffer;
import org.rtmplite.events.IRTMPEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RTMP packet. Consists of packet header, data and event context.
 */
public class GeneratedMessage implements Externalizable {

	private static final long serialVersionUID = -6415050845346626950L;
	
	private static Logger log = LoggerFactory.getLogger(GeneratedMessage.class);
	
	private static final boolean noCopy = System.getProperty("packet.noCopy") == null ? false : Boolean.valueOf(System.getProperty("packet.noCopy"));

	/**
	 * Header
	 */
	private Header header;

	/**
	 * RTMP event
	 */
	private IRTMPEvent message;

	/**
	 * Packet data
	 */
	private IoBuffer data;

	public GeneratedMessage() {
		log.trace("ctor");
	}

	/**
	 * Create packet with given header
	 * @param header       Packet header
	 */
	public GeneratedMessage(Header header) {
		log.trace("Header: {}", header);
		this.header = header;
		data = IoBuffer.allocate(header.getSize()).setAutoExpand(true);
	}

	/**
	 * Create packet with given header and event context
	 * @param header     RTMP header
	 * @param event      RTMP message
	 */
	public GeneratedMessage(Header header, IRTMPEvent event) {
		log.trace("Header: {} event: {}", header, event);
		this.header = header;
		this.message = event;
	}

	/**
	 * Getter for header
	 *
	 * @return  Packet header
	 */
	public Header getHeader() {
		return header;
	}

	/**
	 * Setter for event context
	 *
	 * @param message  RTMP event context
	 */
	public void setMessage(IRTMPEvent message) {
		this.message = message;
	}

	/**
	 * Getter for event context
	 *
	 * @return RTMP event context
	 */
	public IRTMPEvent getMessage() {
		return message;
	}

	/**
	 * Setter for data
	 *
	 * @param buffer Packet data
	 */
	public void setData(IoBuffer buffer) {
		if (noCopy) {
			log.trace("Using buffer reference");
			this.data = buffer;
		} else {
			// try the backing array first if it exists
			if (buffer.hasArray()) {
				log.trace("Buffer has backing array, making a copy");
				byte[] copy = new byte[buffer.limit()];
				buffer.mark();
				buffer.get(copy);
				buffer.reset();
				this.data = IoBuffer.wrap(copy);
			} else {
				log.trace("Buffer has no backing array, using ByteBuffer");
				// fallback to ByteBuffer
				this.data.put(buffer.buf()).flip();
			}
		}
	}

	/**
	 * Getter for data
	 *
	 * @return Packet data
	 */
	public IoBuffer getData() {
		return data;
	}

	/**
	 * Returns whether or not the packet has a data buffer.
	 * 
	 * @return true if data buffer exists and false otherwise
	 */
	public boolean hasData() {
		return data != null;
	}

	/**
	 * Clears the data buffer.
	 */
	public void clearData() {
		if (data != null) {
			data.clear();
			data.free();
			data = null;
		}
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		header = (Header) in.readObject();
		message = (IRTMPEvent) in.readObject();
		message.setHeader(header);
		message.setTimestamp(header.getTimer());
	}

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(header);
		out.writeObject(message);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Packet [");
		if (header != null) {
			sb.append("[header data type=" + header.getDataType()+ ", channel=" + header.getChannelId() + ", timer=" + header.getTimer() + "]");
		} else {
			sb.append("[header=null]");
		}
		if (message != null) {
			sb.append(", [message timestamp=" + message.getTimestamp() + "]");
		} else {
			sb.append(", [message=null]");
		}
		sb.append("]");
		return sb.toString();
	}

}
