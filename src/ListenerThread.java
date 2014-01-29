import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings("rawtypes")
public class ListenerThread extends Thread{
	ServerSocket serverSocket;
	MessagePasser messagePasser;
	public ListenerThread(MessagePasser messagePasser) throws IOException{
		this.serverSocket = messagePasser.serverSocket;
		this.messagePasser = messagePasser;
	}
	public void run(){
		while(true){
			try {
				Socket client = serverSocket.accept();
				System.out.println("accepted: " + client.toString());
				Thread readInputStreamThread = new ReadInputStream(client, messagePasser);
				readInputStreamThread.start();
			} catch (SocketException e){
				System.err.println("server listening socket down");
				break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}
}

class LoggerListenerThread extends Thread{
	
	ServerSocket serverSocket;
	LoggerMessagePasser loggerMessagePasser;
	public LoggerListenerThread(LoggerMessagePasser loggerMessagePasser){
		this.serverSocket = loggerMessagePasser.serverSocket;
		this.loggerMessagePasser = loggerMessagePasser;
	}
	public void run(){
		while(true){
			try {
				Socket client = serverSocket.accept();
				System.out.println("accepted: " + client.toString());
				Thread LoggerReadInputStream = new LoggerReadInputStream(client, loggerMessagePasser);
				LoggerReadInputStream.start();
			} catch (SocketException e){
				System.err.println("server listening socket down");
				break;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}
	}
}