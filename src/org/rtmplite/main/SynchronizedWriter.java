package org.rtmplite.main;

import java.io.IOException;
import java.io.OutputStream;

public class SynchronizedWriter {
	
	private OutputStream stream;
	
	public SynchronizedWriter(OutputStream stream) {
		this.stream = stream;
	}
	
	public synchronized void write(byte[] buffer) throws IOException {
		stream.write(buffer);
		stream.flush();
	}
}
