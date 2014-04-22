package org.rtmplite.main;

import org.apache.mina.core.buffer.IoBuffer;

public abstract class MessageListener {
	public abstract void onMessage(IoBuffer buffer, int bytesRead);
}
