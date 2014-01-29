import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.yaml.snakeyaml.Yaml;


class LoggerMessagePasser extends MessagePasser{

	public LoggerMessagePasser(String configuration_filename, String local_name) throws IOException {
		super(configuration_filename, local_name);
	}
	
	@Override
	public void startListenerThread() throws IOException{
		Thread loggerListenerThread = new LoggerListenerThread(this);
		loggerListenerThread.start();
	}
	
}

public class MessagePasser {

	@SuppressWarnings("rawtypes")
	LinkedHashMap networkTable;
	HashMap<String, Node> nodeMap = new HashMap<String, Node>();
	HashMap<String, ObjectOutputStream> streamMap= new HashMap<String, ObjectOutputStream>();
	ServerSocket serverSocket;
	ConcurrentLinkedQueue<Message> messageQueue = new ConcurrentLinkedQueue<Message>();
	ConcurrentLinkedQueue<Message> delaySendingQueue = new ConcurrentLinkedQueue<Message>();
	ConcurrentLinkedQueue<Message> popReceivingQueue = new ConcurrentLinkedQueue<Message>();
	ConcurrentLinkedQueue<Message> delayReceivingQueue = new ConcurrentLinkedQueue<Message>();
	ArrayList<LinkedHashMap<String, String>> configList;
	ArrayList<LinkedHashMap<String, String>> sendRuleList;
	ArrayList<LinkedHashMap<String, String>> receiveRuleList;
	File configurationFile;
	long lastModifiedTime;
	String configuration_filename;
	String local_name;
	
	ClockService clockService = null;
	ClockType clockType = null;
	
	public void setClockService(ClockType clockType){
		switch(clockType){
		case LOGICAL:
			clockService = Clock.getClockService(LogicalClock.factory);
			break;
		case VECTOR:
			clockService = Clock.getClockService(VectorClock.factory);
			break;
		default:
			System.err.println("SET CLOCK SERVICE ERROR. LOGGER SERVER MAY FAIL TO SET UP");
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void parseConfigurationFile() throws IOException{
		configurationFile = new File("D:\\Dropbox\\" + configuration_filename);
		lastModifiedTime = configurationFile.lastModified();
		InputStream input = new FileInputStream("D:\\Dropbox\\" + configuration_filename);
		Yaml yaml = new Yaml();
		Object data = yaml.load(input);
		input.close();
		networkTable = (LinkedHashMap)data;	//get the information
		configList = (ArrayList<LinkedHashMap<String, String>>) networkTable.get("configuration");
		sendRuleList = (ArrayList<LinkedHashMap<String, String>>) networkTable.get("sendRules");
		receiveRuleList = (ArrayList<LinkedHashMap<String, String>>) networkTable.get("receiveRules");
		//System.out.println(sendRuleList.toString());
		//System.out.println(receiveRuleList.toString());
		for(Map m : configList){
			String name = (String)m.get("name");
			String ip = (String)m.get("ip");
			int port = (int)m.get("port");
			nodeMap.put(name, new Node(ip, port));
		}
		int portNumber = nodeMap.get(local_name).port;
		serverSocket = new ServerSocket(portNumber);
		startListenerThread();
	}
	
	public void startListenerThread() throws IOException{
		Thread listenerThread = new ListenerThread(this);
		listenerThread.start();
	}
	
	public MessagePasser(String configuration_filename, String local_name) throws IOException{
		this.configuration_filename = configuration_filename;
		this.local_name = local_name;
		parseConfigurationFile();
	}
	
	void reconfiguration() throws IOException{
		if(configurationFile.lastModified() > lastModifiedTime){
			lastModifiedTime = configurationFile.lastModified();
			//System.out.println("configuration file modified!!!");
			nodeMap.clear();
			//System.out.println("nodeMap cleared! "+ nodeMap.toString());
//			socketMap.clear();
			//System.out.println("socketMap cleared! "+ socketMap.toString());
			streamMap.clear();
			//System.out.println("streamMap cleared! "+ streamMap.toString());
			configList.clear();
			sendRuleList.clear();
			receiveRuleList.clear();
			//System.out.println("config and rule list cleared!");
			serverSocket.close();
			//System.out.println("reparsing new configuration file!");
			parseConfigurationFile();
			//System.out.println("reparsing new configuration file done!");
			//System.out.println("nodeMap reparsed! "+ nodeMap.toString());
			//System.out.println("socketMap reparsed! "+ socketMap.toString());
			//System.out.println("streamMap reparsed! "+ streamMap.toString());
		}
	}

	@SuppressWarnings("resource")
	void clockServiceInit() throws UnknownHostException, IOException, InterruptedException{
		if(this.clockType == null){	//clock type is not yet set by logger
			//send request to logger
			if(!this.streamMap.containsKey("logger")){	//not connect yet
				try{
					Socket destSocket = new Socket(InetAddress.getByName(nodeMap.get("logger").ip), nodeMap.get("logger").port);
					ObjectOutputStream oos = new ObjectOutputStream(destSocket.getOutputStream());
					streamMap.put("logger", oos);
					System.out.println("streamMap updated! " + streamMap.toString());
				} catch (ConnectException e){
					System.err.println("CANNOT CONNECT TO LOGGER");
					return;
				}
			}
			TimeStampedMessage clockSetRequest = new TimeStampedMessage("logger", "clock_set_request", null, null);
			clockSetRequest.set_source(local_name);
			//send it
			ObjectOutputStream oos = this.streamMap.get("logger");
			System.out.println("sending clock set request");
			oos.writeObject(clockSetRequest);
			oos.flush();
			oos.reset();
			System.out.println("clock set request sent");
			//wait the logger's set up message
			System.out.println("Wait for logger's response for 1 sec.");
			Thread.sleep(1000);
			if(this.clockType != null){
				System.out.println("clock type set as: " + this.clockType);
				if(this.clockType == ClockType.LOGICAL) 
					this.clockService = Clock.getClockService(LogicalClock.factory);
				if(this.clockType == ClockType.VECTOR)
					this.clockService = Clock.getClockService(VectorClock.factory);
			}
			else{
				System.err.println("NO RESPONSE FROM LOGGER");
				return;
			}
		}
	}
	
	void send(Message message) throws UnknownHostException, IOException, InterruptedException{
		
		reconfiguration();
		try{
			clockServiceInit();
		} catch (SocketException e){
			System.err.println("CANNOT CONNECT TO LOGGER");
			return;
		}
		
		//System.out.println("sending..................");
		message.set_action(checkSendingRules(message));
		switch(message.action){
		case "drop":
			//System.out.println("send: drop");
			//do nothing, just drop it
			//System.out.println("send: drop");
			break;
		case "duplicate":
			//System.out.println("send: duplicate");
			sendMessage(message);
			message.set_duplicate();
			sendMessage(message);
			break;
		case "delay":
			//System.out.println("send: delay");
			delaySendingQueue.offer(message);
			break;
		default:
			//System.out.println("send: default");
			sendMessage(message);
			break;
		}
		//System.out.println("sending done..................");
	}

	@SuppressWarnings("resource")
	void sendMessage(Message message) throws IOException{
		
		if(!streamMap.containsKey(message.destination)){
			System.out.println("new socket: " + nodeMap.get(message.destination).ip + " " + nodeMap.get(message.destination).port);
			if(!nodeMap.containsKey(message.destination)){
				System.err.println("Can't find this node in configuration file!");
				return;
			}
			try{
				Socket destSocket = new Socket(InetAddress.getByName(nodeMap.get(message.destination).ip), nodeMap.get(message.destination).port);
//				socketMap.put(message.destination, destSocket);
				//System.out.println("socketMap updated! " + socketMap.toString());
				ObjectOutputStream oos = new ObjectOutputStream(destSocket.getOutputStream());
				streamMap.put(message.destination, oos);
				//System.out.println("streamMap updated! " + streamMap.toString());
				
			} catch(IOException e){
				System.err.println("Connection Fail!");
				return;
			}
		}
		if(this.clockType == null){
			System.out.println("Message without time stamp will be sent!");
			streamMap.get(message.destination).writeObject(message);
		}
		else{
			System.out.println("Time stamped message will be sent!");
			TimeStampedMessage tsm = new TimeStampedMessage(message.destination, message.kind, message.data, this.clockType);
			tsm.set_source(message.source);
			tsm.set_action(message.action);
			tsm.duplicate = message.duplicate;
			tsm.sequenceNumber = message.sequenceNumber;
			
			
			if(this.clockType == ClockType.LOGICAL){
				tsm.setLogicalTimeStamps(((LogicalClock)this.clockService).internalLogicalClock);
			}
			if(this.clockType == ClockType.VECTOR){
				tsm.setVectorTimeStamps(((VectorClock)this.clockService).internalVectorClock);
			}
			streamMap.get(message.destination).writeObject(tsm);
		}
		streamMap.get(message.destination).flush();
		streamMap.get(message.destination).reset();
		while(!delaySendingQueue.isEmpty()){
			sendMessage(delaySendingQueue.poll());
		}
	}

	Message receive() throws IOException, InterruptedException{
		
		reconfiguration();
		try{
			clockServiceInit();
		} catch (SocketException e){
			System.err.println("CANNOT CONNECT TO LOGGER");
		}
		
		receiveMessage();
		if(!popReceivingQueue.isEmpty()){
			Message popMessage = popReceivingQueue.poll();
			return popMessage;
		}
		else{
			return new Message(null, null, "No message to receive!");
		}
	}

	void receiveMessage(){
		Message receivedMessage;
		//System.out.println("Receiving..................");
		if(!messageQueue.isEmpty()){
			receivedMessage = messageQueue.poll();
			String action = checkReceivingRules(receivedMessage);
			switch(action){
			case "drop":
				//System.out.println("receive: drop");
				//do nothing, just drop it
				//System.out.println("receive: drop");
				break;
			case "duplicate":
				//System.out.println("receive: duplicate");
				popReceivingQueue.offer(receivedMessage);
				popReceivingQueue.offer(receivedMessage);
				while(!delayReceivingQueue.isEmpty()){
					popReceivingQueue.offer(delayReceivingQueue.poll());
				}
				break;
			case "delay":
				//System.out.println("receive: delay");
				delayReceivingQueue.offer(receivedMessage);
				receiveMessage();
				break;
			default:
				//default action
				//System.out.println("receive: default");
				popReceivingQueue.offer(receivedMessage);
				while(!delayReceivingQueue.isEmpty()){
					popReceivingQueue.offer(delayReceivingQueue.poll());
				}
			}
		}
		//System.out.println("Receiving done..................");
	}

	@SuppressWarnings("rawtypes")
	String checkSendingRules(Message message){

		for(Map m : sendRuleList){

			boolean srcMatch = false;
			boolean dstMatch = false;
			boolean seqMatch = false;
			boolean kindMatch = false;
			boolean duplicate = false;

			if(!m.containsKey("src")){
				srcMatch = true;
			}
			else if(((String)m.get("src")).equalsIgnoreCase(message.source)){
				srcMatch = true;
			}

			if(!m.containsKey("dest")){
				dstMatch = true;
			}
			else if(((String)m.get("dest")).equalsIgnoreCase(message.destination)){
				dstMatch = true;
			}

			if(!m.containsKey("seqNum")){
				seqMatch = true;
			}
			else if((int)m.get("seqNum") == message.sequenceNumber){
				seqMatch = true;
			}

			if(!m.containsKey("kind")){
				kindMatch = true;
			}
			else if(((String)m.get("kind")).equalsIgnoreCase(message.kind)){
				kindMatch = true;
			}
			
			if(!m.containsKey("duplicate")){
				duplicate = true;
			}
			else if(m.get("duplicate").equals(message.duplicate)){
				duplicate = true;
			}

			if(srcMatch && dstMatch && seqMatch && kindMatch && duplicate){
				return (String)m.get("action");
			}
		}
		return "none";
	}

	@SuppressWarnings("rawtypes")
	String checkReceivingRules(Message message){

		for(Map m : receiveRuleList){

			boolean srcMatch = false;
			boolean dstMatch = false;
			boolean seqMatch = false;
			boolean kindMatch = false;
			boolean duplicate = false;

			if(!m.containsKey("src")){
				srcMatch = true;
			}
			else if(((String)m.get("src")).equalsIgnoreCase(message.source)){
				srcMatch = true;
			}

			if(!m.containsKey("dest")){
				dstMatch = true;
			}
			else if(((String)m.get("dest")).equalsIgnoreCase(message.destination)){
				dstMatch = true;
			}

			if(!m.containsKey("seqNum")){
				seqMatch = true;
			}
			else if((int)m.get("seqNum") == message.sequenceNumber){
				seqMatch = true;
			}

			if(!m.containsKey("kind")){
				kindMatch = true;
			}
			else if(((String)m.get("kind")).equalsIgnoreCase(message.kind)){
				kindMatch = true;
			}
			
			if(!m.containsKey("duplicate")){
				duplicate = true;
			}
			else if(m.get("duplicate").equals(message.duplicate)){
				duplicate = true;
			}

			if(srcMatch && dstMatch && seqMatch && kindMatch && duplicate){
				return (String)m.get("action");
			}

		}
		return "none";
	}
}
