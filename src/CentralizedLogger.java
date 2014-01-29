import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


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
			if(!loggerMessagePasser.clockSet){
				for(String nodeName : loggerMessagePasser.streamMap.keySet()){
					setClockMessage = new TimeStampedMessage(nodeName, "set clock", null, ClockType.LOGICAL);
					setClockMessage.set_source(loggerMessagePasser.local_name);
					loggerMessagePasser.streamMap.get(nodeName).writeObject(setClockMessage);
				}
				loggerMessagePasser.clockSet = true;
			}
			break;
		case "vector":
			System.out.println("vector");
			loggerMessagePasser.clockType = ClockType.VECTOR;
			if(!loggerMessagePasser.clockSet){
				for(String nodeName : loggerMessagePasser.streamMap.keySet()){
					setClockMessage = new TimeStampedMessage(nodeName, "set clock", null, ClockType.VECTOR);
					setClockMessage.set_source(loggerMessagePasser.local_name);
					loggerMessagePasser.streamMap.get(nodeName).writeObject(setClockMessage);
				}
				loggerMessagePasser.clockSet = true;
			}
			break;
		}
		
		System.out.println("logger terminates");
	}
}
