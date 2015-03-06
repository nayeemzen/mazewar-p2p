import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;


public class IncomingMessageListenerThread implements Runnable {
	
	private final ObjectInputStream readStream;

	IncomingMessageListenerThread(Socket socket) throws IOException {
		readStream = new ObjectInputStream(socket.getInputStream()); 
	}
	
	public void run() {
		try {
			MazewarPacket packetFromClient = new MazewarPacket();
			while ((packetFromClient = (MazewarPacket) readStream.readObject()) != null) {
				handleReceivedPacket(packetFromClient);
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// broadcastClientShutdown();
			e.printStackTrace();
		}
	}

	private void handleReceivedPacket(MazewarPacket packetFromClient) {
		// TODO Auto-generated method stub
		
	}

}
