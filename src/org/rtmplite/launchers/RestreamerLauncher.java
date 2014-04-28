package org.rtmplite.launchers;

import org.rtmplite.connectors.BasicClient.ConnectionParam;
import org.rtmplite.connectors.BasicClient.ParamType;
import org.rtmplite.simple.restreamer.ConnectListener;
import org.rtmplite.simple.restreamer.DisconnectListener;
import org.rtmplite.simple.restreamer.PublishInfo;
import org.rtmplite.simple.restreamer.Restreamer;
import org.rtmplite.simple.restreamer.RestreamerManager;
import org.rtmplite.simple.restreamer.Restreamer.RestreamType;
import org.rtmplite.simple.restreamer.StartRestreamException;

public class RestreamerLauncher {
	public static void main(String[] args) {
		
		String inputURL = "rtmp://cp82434.live.edgefcs.net/live?_fcs_vhost=cp82434.live.edgefcs.net/wabuk_ch15_500k@s40615?reportingKey=eventId-920265_partnerId-10%26auth=daEb0aqciczaHaKa_d_coa.cHaFazc8dndq-btxva9-O-fbjdo%26aifp=1".replace("%26", "&");

		PublishInfo publishInfo = new PublishInfo("83.246.186.32", 1935, "live");
		
		Restreamer restreamer = new Restreamer(inputURL, 1935, publishInfo, "test");
		
		restreamer.addDisconnectListener(new DisconnectListener() {
			
			@Override
			public void onDisconnect(Restreamer restreamer) {
				System.out.println("DISCONNECT!");
			}
		});
		
		restreamer.addConnectListener(new ConnectListener() {
			
			@Override
			public void onConnect(Restreamer restreamer) {
				System.out.println("CONNECT!");
			}
		});
		
		restreamer.setRestreamType(RestreamType.LIVE);
		//restreamer.setMaxRestreamTimeInSeconds(328);
		restreamer.setIdleTimeoutInSeconds(60);
		
		restreamer.addInputConnectionParam("swfUrl", new ConnectionParam("http://www.bet365.com/extra/Streaming/XtraStreamingPlayer_v01_04_00.swf", ParamType.String));
		
		try {
			restreamer.start();
		} catch (StartRestreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		RestreamerManager manager = RestreamerManager.getInstance();
		manager.startDisconnectedRestereamersCleaner();
		
		manager.add(restreamer);
	}
}
