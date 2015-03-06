import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;


public class NamingService {
	public ServerSocket serverSocket;
	private ArrayList <String> connectedClients;
	public static AtomicInteger idCount = new AtomicInteger(0);
	
	NamingService(int port) throws IOException {
		serverSocket = new ServerSocket(port);
		connectedClients = new ArrayList<String>();
	}
	
	public void run() {
		while(true) {
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();
				(new Thread (new NamingServiceClientHandler(clientSocket, connectedClients))).start();
			} catch (IOException e) {
				System.err.println("Error: error accepting client connection");
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
