package org.rtmplite.simple.restreamer;

public class PublishInfo {
	
	private String ip;
	private int port;
	private String app;
	
	public PublishInfo(String ip, int port, String app) {
		this.ip = ip;
		this.port = port;
		this.app = app;
	}
	
	public String getApp() {
		return app;
	}
	
	public String getIp() {
		return ip;
	}
	
	public int getPort() {
		return port;
	}
}
