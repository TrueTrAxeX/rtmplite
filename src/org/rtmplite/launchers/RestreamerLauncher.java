package org.rtmplite.launchers;

import org.rtmplite.simple.restreamer.Restreamer;
import org.rtmplite.simple.restreamer.Restreamer.RestreamType;
import org.rtmplite.simple.restreamer.StartRestreamException;

public class RestreamerLauncher {
	public static void main(String[] args) {
		
		String inputURL = "rtmp://178.162.192.218:1935/livepkgr/raw:live299757_23414563_0_38_55_25_4";
		String outputURL = "rtmp://83.246.186.32:1935/live/test";
		
		Restreamer restreamer = new Restreamer(inputURL, outputURL);
		restreamer.setRestreamType(RestreamType.LIVE);
		//restreamer.setMaxRestreamTimeInSeconds(520);
		
		try {
			restreamer.start();
		} catch (StartRestreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
