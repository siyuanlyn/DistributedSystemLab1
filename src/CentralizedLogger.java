import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.Comparator;

class LogicalLog{
	String processName;
	String event;
	Metadata metadata;
	int timestamp;
	public LogicalLog(TimeStampedMessage logical_log){
		this.processName = logical_log.source;
		this.event = logical_log.kind.substring(3);
		this.metadata = new Metadata((Message)logical_log.data);
		this.timestamp = ((TimeStampedMessage)logical_log).getLogicalTimeStamps().timeStamp;
	}
}

class LogicalLogComparator implements Comparator<LogicalLog> {
	public int compare(LogicalLog log1, LogicalLog log2){
		if(log1.timestamp < log2.timestamp){
			return -1;
		}
		else if(log1.timestamp == log2.timestamp){
			return 0;
		}
		else {
			return 1;
		}
	}
}

class VectorLog{
	String processName;
	String event;
	Metadata metadata;
	int[] timestamp;
	public VectorLog(TimeStampedMessage vector_log){
		this.processName = vector_log.source;
		this.event = vector_log.kind.substring(3);
		this.metadata = new Metadata((Message)vector_log.data);
		this.timestamp = ((TimeStampedMessage)vector_log).getVectorTimeStamps().timeStampMatrix;
	}
}

class VectorLogComparator implements Comparator<VectorLog>{
	public int compare(VectorLog log1, VectorLog log2){
		int i;
		for(i=0; i<log1.timestamp.length; i++){
			if(log1.timestamp[i] != log2.timestamp[i]){
				break;
			}
		}
		if(i == log1.timestamp.length){
			return 0;	//completely equal!
		}
		else{
			for(i=0; i<log1.timestamp.length; i++){
				if(log1.timestamp[i] <= log2.timestamp[i]){
					continue;
				}
				break;
			}
			if(i == log1.timestamp.length){
				return -1;	//not equal, happen before
			}
			else{
				for(i=0; i<log1.timestamp.length; i++){
					if(log1.timestamp[i] >= log2.timestamp[i]){
						continue;
					}
					break;
				}
				if(i == log1.timestamp.length){
					return 1;	//happen after
				}
				else{
					return 0;	//concurrent
				}
			}
		}

	}
}

class Metadata{
	String msgSrc;
	String msgDst;
	String msgKind;
	int msgSeqNo;
	String msgAction;
	boolean msgDup;
	public Metadata(Message logMsg){
		this.msgSrc = logMsg.source;
		this.msgDst = logMsg.destination;
		this.msgKind = logMsg.kind;
		this.msgSeqNo = logMsg.sequenceNumber;
		this.msgAction = logMsg.action;
		this.msgDup = logMsg.duplicate;
	}
	
	public String toString(){
		return "[msgSrc=" + msgSrc + "; msgDst=" + msgDst + "; msgKind=" + msgKind +
				"; msgSeqNo=" + msgSeqNo + "; msgAction=" + msgAction + "; msgDup=" + msgDup + "]";
	}
}

public class CentralizedLogger {
	public static void main(String[] args) throws IOException{
		LoggerMessagePasser loggerMessagePasser = new LoggerMessagePasser(args[0], args[1]);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("Enter the clock type you want to set for nodes: logical or vector");
		String command = in.readLine();
		TimeStampedMessage setClockMessage;
		switch(command.toLowerCase()){
		case "logical":
			System.out.println("logical");
			loggerMessagePasser.clockType = ClockType.LOGICAL;
			for(String nodeName : loggerMessagePasser.streamMap.keySet()){
				setClockMessage = new TimeStampedMessage(nodeName, "set_clock", null, ClockType.LOGICAL);
				setClockMessage.set_source(loggerMessagePasser.local_name);
				ObjectOutputStream oos = loggerMessagePasser.streamMap.get(nodeName);
				oos.writeObject(setClockMessage);
				oos.flush();
				oos.reset();
			}
			break;
		case "vector":
			System.out.println("vector");
			loggerMessagePasser.clockType = ClockType.VECTOR;
			for(String nodeName : loggerMessagePasser.streamMap.keySet()){
				setClockMessage = new TimeStampedMessage(nodeName, "set_clock", null, ClockType.VECTOR);
				setClockMessage.set_source(loggerMessagePasser.local_name);
				ObjectOutputStream oos = loggerMessagePasser.streamMap.get(nodeName);
				oos.writeObject(setClockMessage);
				oos.flush();
				oos.reset();
			}
			break;
		}
		System.out.println("INFO: Logger time set done");
		while(true){
			
		}
	}
}
