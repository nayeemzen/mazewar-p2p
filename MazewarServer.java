import java.io.BufferedWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;


public class MazewarServer implements Runnable {
	public ServerSocket serverSocket;
	private ConcurrentHashMap <String, ObjectOutputStream> peerList;
	private MazewarClient client;
	
	MazewarServer(MazewarClient client, int port, ConcurrentHashMap <String, ObjectOutputStream> peerList) throws IOException {
		serverSocket = new ServerSocket(port);
		this.peerList = peerList;
		this.client = client;
	}
	
	public void run() {
		/* Indefinitely accept new clients */
		while(true) {
			Socket clientSocket = null;
			try {
				clientSocket = serverSocket.accept();
			} catch (IOException e) {
				System.err.println("Error: error accepting client connection");
				e.printStackTrace();
			}
			
			ObjectOutputStream outStream;
			try {
				outStream = new ObjectOutputStream(clientSocket.getOutputStream());
				peerList.put(clientSocket.getInetAddress().toString() + "-" + clientSocket.getPort(), outStream);
				(new Thread (new IncomingMessageListenerThread(client, clientSocket))).start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
