package org.rtmplite.launchers;

import org.rtmplite.simple.restreamer.DisconnectListener;
import org.rtmplite.simple.restreamer.Restreamer;
import org.rtmplite.simple.restreamer.Restreamer.RestreamType;
import org.rtmplite.simple.restreamer.StartRestreamException;

public class RestreamerLauncher {
	public static void main(String[] args) {

		String inputURL = "rtmp://178.162.192.218:1935/livepkgr/raw:live300061_23439375_15_37_52_25_4";
		String outputURL = "rtmp://83.246.186.32:1935/live/test";
	
		Restreamer restreamer = new Restreamer(inputURL, outputURL);
		
		restreamer.addDisconnectListener(new DisconnectListener() {
			
			@Override
			public void onDisconnect(Restreamer restreamer) {
				System.out.println("Отцепились нахуй!");
			}
		});
		
		restreamer.setRestreamType(RestreamType.LIVE);
		restreamer.setMaxRestreamTimeInSeconds(128);
		restreamer.setIdleTimeoutInSeconds(60);
		
		try {
			restreamer.start();
		} catch (StartRestreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
