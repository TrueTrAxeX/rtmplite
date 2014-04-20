package org.rtmplite.connectors;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rtmplite.amf.AMFArray;
import org.rtmplite.amf.AMFObject;
import org.rtmplite.messages.Header;
import org.rtmplite.messages.Message;
import org.rtmplite.messages.Header.PacketTypes;

public class BasicConnector {
	
	public enum ParamType {
		Null, Number, String, Boolean
	}
	
	public class ConnectionParam {
		
		private Object param;
		private ParamType paramType;
		
		public ConnectionParam(Object param, ParamType paramType) {
			this.param = param;
			this.paramType = paramType;
		}
	}
	
	private Socket socket;
	private AMFObject amfObject;
	private AMFArray amfArr;
	private Header header;
	private long transactionNumber = 1L;
	
	private Map<String, ConnectionParam> connectionParams = new HashMap<String, ConnectionParam>();
	
	public BasicConnector(Socket socket) {
		this.socket = socket;
	}
	
	public void addConnectionParam(String name, ConnectionParam param) {
		connectionParams.put(name, param);
	}
	
	private void setDefaultParams() {
		if(connectionParams.get("objectEncoding") == null) 
			connectionParams.put("objectEncoding", new ConnectionParam(0d, ParamType.Number));
		if(connectionParams.get("flashver") == null) 
			connectionParams.put("flashver", new ConnectionParam("WIN 11,2,202,235", ParamType.String));
		if(connectionParams.get("audioCodecs") == null)  
			connectionParams.put("audioCodecs", new ConnectionParam(3575d, ParamType.Number));
		if(connectionParams.get("videoFunction") == null)  
			connectionParams.put("videoFunction", new ConnectionParam(1d, ParamType.Number));
		if(connectionParams.get("pageUrl") == null)  
			connectionParams.put("pageUrl", new ConnectionParam(null, ParamType.Null));
		if(connectionParams.get("capabilities") == null)  
			connectionParams.put("capabilities", new ConnectionParam(15d, ParamType.Number));
		if(connectionParams.get("videoCodecs") == null)  
			connectionParams.put("videoCodecs", new ConnectionParam(252d, ParamType.Number));
		if(connectionParams.get("swfUrl") == null)  
			connectionParams.put("swfUrl", new ConnectionParam(null, ParamType.Null));
	}
	
	private void fillAmfObject() {
		for(Entry<String, ConnectionParam> param : connectionParams.entrySet()) {
			
			switch(param.getValue().paramType) {
				
				case String: 
					amfArr.append(param.getKey(), (String) param.getValue().param);
				break;
				
				case Number:
					amfArr.append(param.getKey(), (Double) param.getValue().param);
				break;
				
				case Boolean:
					amfArr.append(param.getKey(), (Boolean) param.getValue().param);
				break;
				
				case Null:
					amfArr.appendNull(param.getKey());
				break;
			}
		}
	}
	
	public void setTransactionNumber(long number) {
		this.transactionNumber = number;
	}
	
	public void connect(String url) throws IOException {
		
		header = new Header();
		header.setPacketType(PacketTypes.AMF_COMMAND);
		
		this.amfObject = new AMFObject();
		this.amfArr = new AMFArray();
		
		amfObject.addString("connect");
		amfObject.addNumber((double)transactionNumber);
		
		setDefaultParams();
		
		parseUrl(url);
		
		fillAmfObject();

		amfObject.addArray(amfArr);
		
		Message message = new Message(header, amfObject);
		byte[] messageBytes = message.getRawBytes();
		
		socket.getOutputStream().write(messageBytes);
		socket.getOutputStream().flush();

		sendCreateStreamMessage();
		
		Pattern pattern = Pattern.compile("rtmp://(.*?)/(.*)[/]+?(.*)?");
		Matcher matcher = pattern.matcher(url);
		
		if(matcher.find()) {
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			this.sendPlay(matcher.group(3));
		}
	}
	
	private void sendPlay(String name) throws IOException {
		header = new Header();
		header.setChannelId((byte)3);
		header.setPacketType(PacketTypes.AMF_COMMAND);
		header.setStreamId(1);
		
		AMFObject amfObject = new AMFObject();
		amfObject.addString("play");
		amfObject.addNumber(2d);
		amfObject.addNull();
		amfObject.addString(name);
		amfObject.addNumber(0d);
		amfObject.addNumber(-1000d);
		
		Message message = new Message(header, amfObject);
		
		this.socket.getOutputStream().write(message.getRawBytes());
		this.socket.getOutputStream().flush();
	}
	
	private void sendCreateStreamMessage() throws IOException {

		Header header = new Header();
		header.setPacketType(PacketTypes.AMF_COMMAND);
		
		AMFObject createStreamAMFObject = new AMFObject();
		
		createStreamAMFObject.addString("createStream");
		createStreamAMFObject.addNumber(2d);
		createStreamAMFObject.addNull();
		
		Message message = new Message(header, createStreamAMFObject);
		
		socket.getOutputStream().write(message.getRawBytes());
		socket.getOutputStream().flush();
	}
	
	private void parseUrl(String url) {
		Pattern pattern = Pattern.compile("rtmp://(.*?)/(.*)[/]+?(.*)?");
		Matcher matcher = pattern.matcher(url);
		
		if(matcher.find()) { 
			connectionParams.put("app", new ConnectionParam(matcher.group(2), ParamType.String));
			connectionParams.put("path", new ConnectionParam(matcher.group(2), ParamType.String));
			connectionParams.put("tcUrl", new ConnectionParam("rtmp://"+matcher.group(1) + "/" + matcher.group(2), ParamType.String));
		} else {
			throw new RuntimeException("RTMP connection parse params error...");
		}
	}
	
	public AMFObject getAmfObject() {
		return amfObject;
	}
}
