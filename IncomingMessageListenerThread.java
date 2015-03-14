import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.lang.Math;

public class IncomingMessageListenerThread implements Runnable {
	
	private final ObjectInputStream readStream;
	private final ObjectOutputStream writeStream;
	private MazewarClient client;
	
	IncomingMessageListenerThread(MazewarClient client, ObjectInputStream readStream, ObjectOutputStream writeStream) throws IOException {
		this.readStream = readStream;
		this.writeStream = writeStream;
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
			
			MazewarClient.waitlist.add(packetFromClient.packetId);
			MazewarClient.eventQueue.add(packetFromClient);
			
			// Send acknowledgement
			packetFromClient.packetType = MazewarPacket.ACK;
			writeStream.writeObject(packetFromClient);
			
		} else if (packetFromClient.packetType == packetFromClient.ACK) {
			
			System.out.println("GOT ACK!!!");
			synchronized(MazewarClient.ackMap) {
				assert(MazewarClient.ackMap.containsKey(packetFromClient.packetId));
				int acks = MazewarClient.ackMap.get(packetFromClient.packetId);
				if (acks > 0) {
					MazewarClient.ackMap.put(packetFromClient.packetId, --acks);
				}
			}
			
			if(MazewarClient.ackMap.get(packetFromClient.packetId) == 0) {
				System.out.println("RECEIVED ALL ACKS!!!");
				client.releaseBroadcast(packetFromClient);
				MazewarClient.ackMap.remove(packetFromClient.packetId);
			}
			
		} else if (packetFromClient.packetType == packetFromClient.RELEASE) {
			System.out.println("GOT RELEASE!!!");
			MazewarClient.waitlist.remove(packetFromClient.packetId);
		}
		
	}

}
