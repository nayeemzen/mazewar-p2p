import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.*;

public class NamingServiceClientHandler implements Runnable {
	
	private final Socket clientSocket;
	private final BufferedReader readStream;
	private final BufferedWriter writeStream;
	private String clientName;
	private ArrayList <String> connectedClients;
	private boolean joined; 
	
	NamingServiceClientHandler(Socket clientSocket, ArrayList <String> connectedClients) throws IOException {
		this.clientSocket = clientSocket;
		this.connectedClients = connectedClients;
		
		clientName = null;
		joined = false;
		readStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		writeStream = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
		
		System.out.println("Created new thread to handle client connection");
	}
	
	private void handleReceivedQuery(String queryFromClient) {
		String[] tokens = queryFromClient.split(" ");
		if (tokens.length < 1) {
			return;
		}
		
		switch (tokens[0]) {
			case "join":
				if (tokens.length > 1) {
					handleJoin(tokens);
				}
				break;
			case "quit":
			default:
				break;
		}
	}
	
	private void handleJoin(String[] parameters) {
		if (joined) return;
		
		try {
			writeStream.write(NamingService.idCount.incrementAndGet() + " " + connectedClients.toString());
			writeStream.newLine();
			writeStream.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		connectedClients.add(clientSocket.getInetAddress().toString() + "-" + parameters[1]);
		joined = true;
	}

	@Override
	public void run() {
		try {
			String queryFromClient;
			while ((queryFromClient = readStream.readLine()) != null) {
				System.out.println(queryFromClient);
				handleReceivedQuery(queryFromClient);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				clientSocket.close();
				readStream.close();
				writeStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
