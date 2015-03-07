import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

public class MazewarClient {
	
	private Socket socket;
	private int clientId;
	public ConcurrentHashMap <String, ObjectOutputStream> peerList;
	public static PriorityQueue<MazewarPacket> eventQueue;
	public static ConcurrentHashMap <MazewarPacket, Integer> ackMap;
	
	MazewarClient(int clientId) {
		socket = null;
		peerList = null;
		this.clientId = clientId;
		ackMap = new ConcurrentHashMap <MazewarPacket, Integer>();
		eventQueue = new PriorityQueue<MazewarPacket>(0, new Comparator <MazewarPacket> () {
			public int compare(MazewarPacket o1, MazewarPacket o2) {	
				int compare = o1.timestamp - o2.timestamp;
				return compare == 0 ?  (o1.PID - o2.PID) : compare;
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
		
		broadcast(payload);
			
		return true;
	}
	
	private void broadcast(MazewarPacket payload) {
		assert peerList != null;
		int acksExpected = 0;
		int lamportTimestamp;
		// TODO: Tag with Lamport Timestamp
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
	}

	private void connectToPeers(ArrayList<String> peerList) {
		this.peerList = new ConcurrentHashMap <String, ObjectOutputStream>();
		for(String peer: peerList) {
			if(this.peerList.containsKey(peer)) continue;
			// Host and port are hyphen separated
			String [] connectionInfo = peer.split("-");
			Socket socket;
			try {
				socket = new Socket(connectionInfo[0], Integer.parseInt(connectionInfo[1]));
				this.peerList.put(peer, new ObjectOutputStream(socket.getOutputStream()));
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
	}
	
	public void start(ArrayList <String> peerList, int port) {
		connectToPeers(peerList);
		/*try {
			(new Thread (new MazewarServer(this, port))).start();
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	}
}
