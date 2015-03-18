import java.net.*;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.io.*;

public class NamingServiceClientHandler implements Runnable {
	
	private final Socket clientSocket;
	private final ObjectInputStream readStream;
	private final ObjectOutputStream writeStream;
	private String clientName;
	private String clientInfo;
	private ArrayList <String> connectedClients;
	private boolean joined; 
	
	NamingServiceClientHandler(Socket clientSocket, ArrayList <String> connectedClients) throws IOException {
		this.clientSocket = clientSocket;
		this.connectedClients = connectedClients;
		
		clientName = null;
		joined = false;
		writeStream = new ObjectOutputStream(clientSocket.getOutputStream());
		writeStream.flush();
		readStream = new ObjectInputStream(clientSocket.getInputStream());
		
		
		System.out.println("Created new thread to handle client connection");
	}
	
	private void handleReceivedQuery(NamingServicePacket queryFromClient) {
		if ("join".equals(queryFromClient.packetType)) {
			handleJoin(queryFromClient);
		}
	}
	
	private void handleJoin(NamingServicePacket parameters) {
		if (joined) return;
		
		int clientId = NamingService.idCount.incrementAndGet();
		try {
			NamingServicePacket packet = new NamingServicePacket(clientId, connectedClients);
			writeStream.writeObject(packet);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		String clientInfo = clientSocket.getRemoteSocketAddress().toString() + "-" + parameters.port + "-" + parameters.clientName + "-" + clientId;
		this.clientName = parameters.clientName;
		this.clientInfo = clientInfo;
		connectedClients.add(clientInfo);
		joined = true;
	}

	@Override
	public void run() {
		try {
			NamingServicePacket queryFromClient;
			while ((queryFromClient = (NamingServicePacket)readStream.readObject()) != null) {
				System.out.println(queryFromClient);
				handleReceivedQuery(queryFromClient);
			}
		} catch (IOException e) {
			if (this.clientInfo != null) {
				synchronized (connectedClients) {
					connectedClients.remove(this.clientInfo);
				}
				System.out.println("Client " + this.clientName + " is gone.");
			} else {
				e.printStackTrace();
			}
		} catch (ClassNotFoundException e) {
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
