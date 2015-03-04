import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;


public class NamingService {
	public ServerSocket serverSocket;
	private ArrayList <String> connectedClients;
	
	NamingService(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		connectedClients = new ArrayList<String>();
	}
	
	public void run() {
		while(true) {
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();
			} catch (IOException e) {
				System.err.println("Error: error accepting client connection");
				e.printStackTrace();
			}
			
			try {
				BufferedWriter writeStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
				connectedClients.add(clientSocket.getInetAddress().toString() + "-" + clientSocket.getPort());
				writeStream.write(connectedClients.toString());
				writeStream.flush();
				writeStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	public static void main (String [] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Error: expected <port> as argument");
			System.exit(-1);
		}
		
		int port = Integer.parseInt(args[0]);
		(new NamingService(port)).run();
	}
}
