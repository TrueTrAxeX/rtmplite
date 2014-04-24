package org.rtmplite.main;

import org.rtmplite.events.IRTMPEvent;
import org.rtmplite.messages.Header;

public abstract class MessageListener {
	public abstract void onMessage(Header header, IRTMPEvent event, byte type);
}
