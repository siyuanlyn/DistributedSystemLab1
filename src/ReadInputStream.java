import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ReadInputStream extends Thread{
	ObjectInputStream ois;
	@SuppressWarnings("rawtypes")
	MessagePasser messagePasser;
	public ReadInputStream(Socket clientSocket, MessagePasser messagePasser) throws IOException{
		ois = new ObjectInputStream(clientSocket.getInputStream());
		this.messagePasser = messagePasser;
	}

	@SuppressWarnings("unchecked")
	public void run(){ 
		while(true){
			try {
				Message receivedMessage = (Message)ois.readObject();
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
