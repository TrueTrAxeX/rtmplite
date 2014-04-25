package org.rtmplite.main;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Stack;

public class SynchronizedWriter {
	
	public class WriterProcess extends Thread {
		
		@Override
		public void run() {
			while(true) {
				try {
					if(!writerQueue.empty()) {
						byte[] arr = writerQueue.pop().array();
						stream.write(arr);
						stream.flush();
						System.out.println("WRITE " + arr);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}
	
	WriterProcess writerProcess;
	
	public SynchronizedWriter() {
		writerProcess = new WriterProcess();
		writerProcess.start();
	}
	
	private Stack<ByteBuffer> writerQueue = new Stack<ByteBuffer>();
	
	private OutputStream stream;
	
	public SynchronizedWriter(OutputStream stream) {
		this.stream = stream;
	}
	
	public synchronized void write(byte[] buffer) throws IOException {
		
		//writerQueue.push(ByteBuffer.wrap(buffer));
		stream.write(buffer);
		stream.flush();
	}
}
