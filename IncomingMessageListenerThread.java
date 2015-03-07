import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.lang.Math;

public class IncomingMessageListenerThread implements Runnable {
	
	private final ObjectInputStream readStream;
	private final ObjectOutputStream writeStream;
	private MazewarClient client;
	
	IncomingMessageListenerThread(MazewarClient client, Socket socket) throws IOException {
		readStream = new ObjectInputStream(socket.getInputStream());
		writeStream = new ObjectOutputStream(socket.getOutputStream());
		this.client = client;
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
	
	private void handleReceivedPacket(MazewarPacket packetFromClient) throws IOException {
		
		if (packetFromClient.packetType == packetFromClient.REQUEST) {
			// Update Lamport clock
			synchronized(client) {
				int lamportTimestamp = 1 + Math.max(packetFromClient.timestamp, MazewarClient.lamportClock.get());
				MazewarClient.lamportClock.set(lamportTimestamp);
				packetFromClient.timestamp = MazewarClient.lamportClock.get();
			}
			
			MazewarClient.eventQueue.add(packetFromClient);
			MazewarClient.releaseMap.put(packetFromClient.md5, false);
			packetFromClient.packetType = MazewarPacket.ACK;
			writeStream.writeObject(packetFromClient);
		} else if (packetFromClient.packetType == packetFromClient.ACK) {
			synchronized(MazewarClient.ackMap) {
				if (MazewarClient.ackMap.containsKey(packetFromClient.md5)) {
					int count = MazewarClient.ackMap.get(packetFromClient.md5);
					MazewarClient.ackMap.put(packetFromClient.md5, --count);
				}
			}
		} else if (packetFromClient.packetType == packetFromClient.RELEASE) {
			
		}
		
	}

}
