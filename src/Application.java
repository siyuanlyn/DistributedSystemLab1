import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Application {

	final static String usage = "Enter the name of people you'd like to send message to," + " the kind of message and the message as follows:" + " \nbob/Ack/Catch one's heart, never be apart.";

	static int sequenceNumber = 0;

	public static int generateSeqNum() {
		return sequenceNumber++;
	}

	public static void main(String[] args) throws IOException,
			InterruptedException {
		MessagePasser messagePasser = new MessagePasser(args[0], args[1]);
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			System.out.println("Enter the command you want to execute: send, receive, retrieve (log from logger) or print (time stamp)");
			String command = in.readLine();
			String dest, kind, sendingMessage;
			String logIt = null;
			switch (command.toLowerCase()) {
				case "send":
					System.out.println("Do you want to log this event? Enter yes or no");
					logIt = in.readLine();
					while (!(logIt.equalsIgnoreCase("yes") || logIt.equalsIgnoreCase("no"))) {
						System.out.println("please enter \"yes\" or \"no\"\n" + "Do you want to log this event?");
						logIt = in.readLine();
					}
					if (logIt.equalsIgnoreCase("yes")) {
						messagePasser.log = true;
					}
					System.out.println(usage);
					String[] input = in.readLine().split("/");
					while (input.length != 3) {
						System.err.println("Illegal input format! Please enter again!\n" + usage);
						Thread.sleep(1);
						System.out.println(usage);
						input = in.readLine().split("/");
					}
					dest = input[0];
					kind = input[1];
					sendingMessage = input[2];
					Message message = new Message(dest, kind, sendingMessage);
					message.set_source(args[1]);
					message.set_seqNum(generateSeqNum());
					messagePasser.send(message);
					break;
				case "receive":
					System.out.println("Do you want to log this event? Enter yes or no");
					logIt = in.readLine();
					while (!(logIt.equalsIgnoreCase("yes") || logIt.equalsIgnoreCase("no"))) {
						System.out.println("please enter \"yes\" or \"no\"\n" + "Do you want to log this event?");
						logIt = in.readLine();
					}
					if (logIt.equalsIgnoreCase("yes")) {
						messagePasser.log = true;
					}
					Message receivedMessage = messagePasser.receive();
					System.out.println(receivedMessage.getClass());
					if (receivedMessage.getClass().equals(Message.class)) {
						System.out.println("Regular Message Received!");
					}
					if (receivedMessage.getClass().equals(TimeStampedMessage.class)) {
						System.out.println("Time Stamped Message Received!");
					}
					System.out.println(receivedMessage.toString());
					break;
				case "retrieve":
					System.out.println("Do you want to log this event? Enter yes or no");
					logIt = in.readLine();
					while (!(logIt.equalsIgnoreCase("yes") || logIt.equalsIgnoreCase("no"))) {
						System.out.println("please enter \"yes\" or \"no\"\n" + "Do you want to log this event?");
						logIt = in.readLine();
					}
					if (logIt.equalsIgnoreCase("yes")) {
						messagePasser.log = true;
					}
					messagePasser.retrieveLog();
					break;
				case "print":
					System.out.println("Do you want to log this event? Enter yes or no");
					logIt = in.readLine();
					while (!(logIt.equalsIgnoreCase("yes") || logIt.equalsIgnoreCase("no"))) {
						System.out.println("please enter \"yes\" or \"no\"\n" + "Do you want to log this event?");
						logIt = in.readLine();
					}
					if (logIt.equalsIgnoreCase("yes")) {
						messagePasser.log = true;
					}
					messagePasser.printTimeStamp();
					break;
				default:
					System.err.println("Illegal input format! Please enter again!");
			}
		}
	}
}
