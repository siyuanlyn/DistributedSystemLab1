import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;


public class ReadInputStream extends Thread{
	ObjectInputStream ois;
	MessagePasser messagePasser;

	public ReadInputStream(Socket clientSocket, MessagePasser messagePasser) throws IOException{
		ois = new ObjectInputStream(clientSocket.getInputStream());
		this.messagePasser = messagePasser;
	}

	@SuppressWarnings("resource")
	public void run(){ 
		while(true){
			try {
				Message receivedMessage = (Message)ois.readObject();
				//if the message is set clock from logger, set the clock and don't enqueue it
				if(receivedMessage.kind.equals("set_clock") && receivedMessage.source.equals("logger")){
					messagePasser.clockType = ((TimeStampedMessage)receivedMessage).getClockType();
					continue;
				}
				if(!messagePasser.streamMap.containsKey(receivedMessage.source)){
					//add the stream in the stream map
					System.out.println("call back");
					Node callBackNode = messagePasser.nodeMap.get(receivedMessage.source);
					Socket callBackSocket = new Socket(InetAddress.getByName(callBackNode.ip), callBackNode.port); 
					ObjectOutputStream oos = new ObjectOutputStream(callBackSocket.getOutputStream());
					messagePasser.streamMap.put(receivedMessage.source, oos);
				}
				
				this.messagePasser.messageQueue.offer(receivedMessage);
			} catch (SocketException e){
				System.err.println("Remote socket down.");
				break;
			} catch (IOException e) {
				System.err.println("Remote socket down.");
				break;
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
	}
}

class LoggerReadInputStream extends Thread{
	ObjectInputStream ois;
	LoggerMessagePasser loggerMessagePasser;

	public LoggerReadInputStream(Socket clientSocket, LoggerMessagePasser loggerMessagePasser) throws IOException{
		ois = new ObjectInputStream(clientSocket.getInputStream());
		this.loggerMessagePasser = loggerMessagePasser;
	}

	@SuppressWarnings("resource")
	public void run(){
		while(true){
			try {
				TimeStampedMessage receivedTimeStampedMessage = (TimeStampedMessage)ois.readObject();
				if(!loggerMessagePasser.streamMap.containsKey(receivedTimeStampedMessage.source)){
					System.out.println("logger call back");
					Node callBackNode = loggerMessagePasser.nodeMap.get(receivedTimeStampedMessage.source);
					Socket callBackSocket = new Socket(InetAddress.getByName(callBackNode.ip),callBackNode.port);
					ObjectOutputStream oos = new ObjectOutputStream(callBackSocket.getOutputStream());
					loggerMessagePasser.streamMap.put(receivedTimeStampedMessage.source, oos);
					//tell the node the clock type
					TimeStampedMessage setClockMessage;
					switch(loggerMessagePasser.clockType){
					case LOGICAL:
						//send back time stamps information of logical clock
						if(!loggerMessagePasser.clockSet){
							setClockMessage = new TimeStampedMessage(receivedTimeStampedMessage.source, "set_clock", null, ClockType.LOGICAL);
							setClockMessage.set_source(loggerMessagePasser.local_name);
							oos.writeObject(setClockMessage);
							oos.flush();
							oos.reset();
							loggerMessagePasser.clockSet = true;
						}
						break;
					case VECTOR:
						//send back time stamps information of vector clock
						if(!loggerMessagePasser.clockSet){
							setClockMessage = new TimeStampedMessage(receivedTimeStampedMessage.source, "set_clock", null, ClockType.VECTOR);
							setClockMessage.set_source(loggerMessagePasser.local_name);
							oos.writeObject(setClockMessage);
							oos.flush();
							oos.reset();
							loggerMessagePasser.clockSet = true;
						}
						break;
					default:
						//send back time stamps information of clock type not yet set
					}

				}
			} catch (SocketException e){
				System.err.println("Remote socket down.");
				break;
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}
}
