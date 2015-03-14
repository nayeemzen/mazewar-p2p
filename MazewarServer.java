import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


public class MazewarServer implements Runnable {
	public ServerSocket serverSocket;
	private ConcurrentHashMap <String, ObjectOutputStream> peerList;
	private MazewarClient client;
	
	MazewarServer(MazewarClient client, int port) throws IOException {
		serverSocket = new ServerSocket(port);
		this.peerList = client.peerList;
		this.client = client;
	}
	
	public void run() {
		/* Indefinitely accept new clients */
		while(true) {
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();
				System.out.println("new client " + clientSocket.getPort());
				
				ObjectOutputStream writeStream = new ObjectOutputStream(clientSocket.getOutputStream());
				// send client name to new client
				writeStream.flush();
				ObjectInputStream readStream = new ObjectInputStream(clientSocket.getInputStream());
				
				peerList.put(clientSocket.getInetAddress().toString() + "-" + clientSocket.getPort(), writeStream);
				(new Thread (new IncomingMessageListenerThread(client, readStream, writeStream))).start();
			} catch (IOException e) {
				System.err.println("Error: error accepting client connection");
				e.printStackTrace();
			}
		}
	}

}
