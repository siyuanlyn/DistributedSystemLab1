import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;


public class ReadInputStream extends Thread{
	ObjectInputStream ois;
	@SuppressWarnings("rawtypes")
	ConcurrentLinkedQueue messageQueue;
	public ReadInputStream(Socket clientSocket, ConcurrentLinkedQueue messageQueue) throws IOException{
		//		System.out.println("new input stream: " + clientSocket.toString());
		ois = new ObjectInputStream(clientSocket.getInputStream());
		this.messageQueue = messageQueue;
	}

	@SuppressWarnings("unchecked")
	public void run(){ 
		while(true){
//			System.out.println("reading the input stream!");
			try {
				messageQueue.offer(ois.readObject());
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
