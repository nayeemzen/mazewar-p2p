import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentHashMap;
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
			MazewarPacket packetFromClient;
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
			if (packetFromClient.eventType == MazewarPacket.REGISTER) {
				client.clientIdToWriteStream.put(packetFromClient.clientId, this.writeStream);
			} else if (packetFromClient.eventType == MazewarPacket.QUIT) {
				System.out.println("PLAYER QUITTING, STOP PLAYING.");
				MazewarClient.playing = false;
			} else if (packetFromClient.eventType == MazewarPacket.ACTION_MISSILE_TICK) {
				client.tick();
			}
			
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
			
			synchronized(writeStream) {
				writeStream.writeObject(packetFromClient);
			}
		} else if (packetFromClient.packetType == packetFromClient.ACK) {
			//System.out.println("GOT ACK!!!");
			synchronized(MazewarClient.ackMap) {
				if(MazewarClient.ackMap.containsKey(packetFromClient.packetId) == false) {
					System.out.println("orphan ACK!");
					return;
				}
				
				int acks = MazewarClient.ackMap.get(packetFromClient.packetId);
				if (acks > 0) {
					MazewarClient.ackMap.put(packetFromClient.packetId, --acks);
				}
				
				if(MazewarClient.ackMap.get(packetFromClient.packetId) == 0) {
					//System.out.println("RECEIVED ALL ACKS!!!");
					client.releaseBroadcast(packetFromClient);
					MazewarClient.ackMap.remove(packetFromClient.packetId);
					if (packetFromClient.eventType == MazewarPacket.QUIT) {
						Mazewar.quit();
					}
				}
			}
			
		} else if (packetFromClient.packetType == packetFromClient.RELEASE) {
			//System.out.println("GOT RELEASE!!!");
			MazewarClient.waitlist.remove(packetFromClient.packetId);
			if (packetFromClient.eventType == MazewarPacket.QUIT) {
				System.out.println("PLAYER QUIT, START PLAYING.");
				synchronized(client.peerList) {
					ConcurrentHashMap <String, ObjectOutputStream> peerList = client.peerList;
					Enumeration<String> peers = peerList.keys();
					String peer;
					while (peers.hasMoreElements()) {
						peer = peers.nextElement();
						if (peerList.get(peer) == writeStream) {
							peerList.remove(peer);
						}
					}
					
					if (peerList.size() == 0) {
						Mazewar.quit();
					}
				}
				
				client.clientIdToWriteStream.remove(packetFromClient.clientId);
				
				MazewarClient.playing = true;
			}
		} else if (packetFromClient.packetType == packetFromClient.TICK_ELECTION) {
			System.out.println("TICK ELECTION STARTED.");
			packetFromClient.packetType = MazewarPacket.TICK_OK;
			writeStream.writeObject(packetFromClient);
		} else if (packetFromClient.packetType == packetFromClient.TICK_OK || packetFromClient.packetType == packetFromClient.TICK_COORDINATOR) {
			// TODO stop election process
			System.out.println("TICK ELECTION DONE.");
			client.inElectionSince = 0;
		}
		
	}

}
