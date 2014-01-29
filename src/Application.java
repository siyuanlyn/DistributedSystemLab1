import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Application {
	final static String usage = "Enter the name of people you'd like to send message to,"
							  + " the kind of message and the message as follows:"
							  + " \nbob/Ack/Catch one's heart, never be apart.";
	static int sequenceNumber = 0;
	public static int generateSeqNum(){
		return sequenceNumber++;
	}

	public static void main(String[] args) throws IOException, InterruptedException{
		MessagePasser messagePasser = new MessagePasser(args[0], args[1]);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while(true){
			System.out.println("Enter the command you want to execute: send or receive");
			String command = in.readLine();
			String dest, kind, sendingMessage;	
			switch(command){
				case "send":
					System.out.println(usage);
					String[] input = in.readLine().split("/");
					while(input.length != 3){
						System.err.println("Illegal input format! Please enter again!");
						Thread.sleep(1);
						System.out.println(usage);
						input = in.readLine().split("/");
					}
					dest = input[0];
					kind = input[1];
					sendingMessage = input[2];
					Message message = new Message(dest,kind, sendingMessage);
					message.set_source(args[1]);
					message.set_seqNum(generateSeqNum());
					messagePasser.send(message);
					break;
				case "receive":
					System.out.println(messagePasser.receive().toString());
					break;
				default:
					System.err.println("Illegal input format! Please enter again!");
				}
		}
	}
}
