package org.rtmplite.red5.trash.amf.io;

public interface IRemotingClient {

	Object invokeMethod(String method, Object[] params);
	
}
