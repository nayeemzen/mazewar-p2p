import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.security.*;

public class MazewarClient {
	
	private Socket socket;
	private int clientId;
	public ConcurrentHashMap <String, ObjectOutputStream> peerList;
	public static PriorityQueue<MazewarPacket> eventQueue;
	public static ConcurrentHashMap <String, Integer> ackMap;
	public static Set<String> waitlist;
	public static AtomicInteger lamportClock;
	
	MazewarClient(int clientId) {
		socket = null;
		peerList = null;
		this.clientId = clientId;
		lamportClock = new AtomicInteger(0);
		
		ackMap = new ConcurrentHashMap <String, Integer>();
		waitlist = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		
		eventQueue = new PriorityQueue<MazewarPacket>(10, new Comparator <MazewarPacket> () {
			public int compare(MazewarPacket o1, MazewarPacket o2) {	
				int compareVal = o1.timestamp - o2.timestamp;
				return compareVal == 0 ?  (o1.clientId - o2.clientId) : compareVal;
			}
		});
		
	}

	public boolean sendEvent(LocalClient client, ClientEvent clientevent) {
		MazewarPacket payload = new MazewarPacket();
		// payload.clientName = client.getName();
		
		if (clientevent.equals(ClientEvent.moveForward)) {
			payload.eventType = MazewarPacket.ACTION_MOVE_UP;
		} else if (clientevent.equals(ClientEvent.moveBackward)) {
			payload.eventType = MazewarPacket.ACTION_MOVE_DOWN;
		} else if (clientevent.equals(ClientEvent.turnLeft)) {
			payload.eventType = MazewarPacket.ACTION_TURN_LEFT;
		} else if (clientevent.equals(ClientEvent.turnRight)) {
			payload.eventType = MazewarPacket.ACTION_TURN_RIGHT;
		} else if (clientevent.equals(ClientEvent.fire)) {
			payload.eventType = MazewarPacket.ACTION_FIRE_PROJECTILE;
		/*} else if (clientevent.equals(ClientEvent.register)) {
			payload.eventType = MazewarPacket.REGISTER;
		} else if (clientevent.equals(ClientEvent.quit)) {
			payload.eventType = MazewarPacket.QUIT;*/
		} else {	
			return false;
		}
		
		requestBroadcast(payload);
			
		return true;
	}
	
	public void requestBroadcast(MazewarPacket payload) {
		payload.timestamp = lamportClock.incrementAndGet();
		int acksExpected = broadcast(payload);
		ackMap.put(payload.md5, acksExpected);
	}
	
	public void releaseBroadcast(MazewarPacket payload) {
		MazewarPacket packet = new MazewarPacket(payload);
		packet.packetType = MazewarPacket.RELEASE;
		broadcast(packet);
	}
	
	private int broadcast(MazewarPacket payload) {
		assert peerList != null;
		int acksExpected = 0;
		for(ObjectOutputStream writeStream : peerList.values()) {
			synchronized(writeStream) {
				try {
					writeStream.writeObject(payload);
					acksExpected++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return acksExpected;
	}

	private void connectToPeers(ArrayList<String> peerList) {
		this.peerList = new ConcurrentHashMap <String, ObjectOutputStream>();
		System.out.println(peerList.size());
		for(String peer: peerList) {
			if(this.peerList.containsKey(peer)) continue;
			// Host and port are hyphen separated
			String [] connectionInfo = peer.split("-");
			System.out.println(peer);
			Socket socket;
			try {
				socket = new Socket(connectionInfo[0], Integer.parseInt(connectionInfo[1]));
				this.peerList.put(peer, new ObjectOutputStream(socket.getOutputStream()));
				(new Thread (new IncomingMessageListenerThread(this, socket))).start();
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
	}
	
	public void start(ArrayList <String> peerList, int port) {
		connectToPeers(peerList);
		try {
			(new Thread (new MazewarServer(this, port))).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
