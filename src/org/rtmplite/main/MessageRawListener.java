package org.rtmplite.main;

import org.apache.mina.core.buffer.IoBuffer;

public abstract class MessageRawListener {
	public abstract void onMessage(IoBuffer rawBytes, byte dataType);
}
