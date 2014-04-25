package org.rtmplite.launchers;

import org.rtmplite.simple.restreamer.ConnectListener;
import org.rtmplite.simple.restreamer.DisconnectListener;
import org.rtmplite.simple.restreamer.PublishInfo;
import org.rtmplite.simple.restreamer.Restreamer;
import org.rtmplite.simple.restreamer.RestreamerManager;
import org.rtmplite.simple.restreamer.Restreamer.RestreamType;
import org.rtmplite.simple.restreamer.StartRestreamException;

public class RestreamerLauncher {
	public static void main(String[] args) {
		
		String inputURL = "rtmp://178.162.192.218:1935/livepkgr/raw:live#70538108_23470135_23_27_47_25_4";

		PublishInfo publishInfo = new PublishInfo("83.246.186.32", 1935, "live");
		
		Restreamer restreamer = new Restreamer(inputURL, 1935, publishInfo, "sashok");
		
		restreamer.addDisconnectListener(new DisconnectListener() {
			
			@Override
			public void onDisconnect(Restreamer restreamer) {
				System.out.println("Отцепились!");
			}
		});
		
		restreamer.addConnectListener(new ConnectListener() {
			
			@Override
			public void onConnect(Restreamer restreamer) {
				System.out.println("Подсоединились!");
			}
		});
		
		restreamer.setRestreamType(RestreamType.LIVE);
		//restreamer.setMaxRestreamTimeInSeconds(328);
		restreamer.setIdleTimeoutInSeconds(60);
		
		try {
			restreamer.start();
		} catch (StartRestreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		RestreamerManager manager = RestreamerManager.getInstance();
		manager.init();
		
		manager.add(restreamer);
	}
}
