import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;

public class MazewarClient {
	
	private Socket socket;
	public int clientId;
	public String clientName;
	public ConcurrentHashMap <String, ObjectOutputStream> peerList;
	public ConcurrentHashMap <Integer, ObjectOutputStream> clientIdToWriteStream;
	private MazeImpl maze;
	private GUIClient localClient;
	public static PriorityBlockingQueue<MazewarPacket> eventQueue;
	public static ConcurrentHashMap <String, Integer> ackMap;
	public static Set<String> waitlist;
	public static AtomicInteger lamportClock;
	public static boolean playing = false;
	
	
	public boolean isCoordinator;
	public long inElectionSince;
	public int lastTick;
	public int lastElection;
	
	MazewarClient(int clientId) {
		socket = null;
		peerList = null;
		maze = null;
		this.clientId = clientId;
		lamportClock = new AtomicInteger(0);
		ackMap = new ConcurrentHashMap <String, Integer>();
		waitlist = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
		clientIdToWriteStream = new ConcurrentHashMap <Integer, ObjectOutputStream>();
		
		eventQueue = new PriorityBlockingQueue<MazewarPacket>(10, new Comparator <MazewarPacket> () {
			public int compare(MazewarPacket o1, MazewarPacket o2) {	
				int compareVal = o1.timestamp - o2.timestamp;
				return compareVal == 0 ?  (o1.clientId - o2.clientId) : compareVal;
			}
		});
		
		isCoordinator = clientId == 1 ? true : false;	
		inElectionSince = 0;
	}
	
	public void addMaze(Maze maze) {
		this.maze = (MazeImpl)maze;
	}
	
	public void addGUIClient(GUIClient client) {
		this.localClient = client;
	}

	public boolean sendEvent(ClientEvent clientevent) {
		if (!MazewarClient.playing == true && !clientevent.equals(ClientEvent.register)) {
			return false;
		}
		
		MazewarPacket payload = new MazewarPacket();
		
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
		} else if (clientevent.equals(ClientEvent.register)) {
			payload.eventType = MazewarPacket.REGISTER;
		} else if (clientevent.equals(ClientEvent.quit)) {
			payload.eventType = MazewarPacket.QUIT;
		} else if (clientevent.equals(ClientEvent.missileTick)){
			payload.eventType = MazewarPacket.ACTION_MISSILE_TICK;
		} else {
			return false;
		}
		
		requestBroadcast(payload);
		if(!clientevent.equals(ClientEvent.register))
			eventQueue.add(payload);
		return true;
	}
	
	public void requestBroadcast(MazewarPacket payload) {
		payload.clientId = clientId;
		payload.clientName = clientName;
		payload.packetType = MazewarPacket.REQUEST;
		payload.packetId = UUID.randomUUID().toString();
		payload.timestamp = lamportClock.incrementAndGet();
		synchronized(MazewarClient.ackMap) {
			int acksExpected = broadcast(payload);
			if (acksExpected > 0)
				ackMap.put(payload.packetId, acksExpected);
		}
	}
	
	public void releaseBroadcast(MazewarPacket payload) {
		MazewarPacket packet = new MazewarPacket(payload);
		packet.packetType = MazewarPacket.RELEASE;
		broadcast(packet);
	}
	
	public void broadcastElection() {
		inElectionSince = System.nanoTime();
		MazewarPacket payload = new MazewarPacket();
		payload.clientId = clientId;
		payload.packetType = MazewarPacket.TICK_ELECTION;
		
		Set<Entry<Integer, ObjectOutputStream>> peers = clientIdToWriteStream.entrySet();
		for (Entry<Integer, ObjectOutputStream> peer : peers) {
			if (peer.getKey() > clientId) {
				try {
					peer.getValue().writeObject(payload);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private int broadcast(MazewarPacket payload) {
		assert peerList != null;
		int acksExpected = 0;
		ObjectOutputStream writeStream;
		Enumeration<String> peers = peerList.keys();
		String peer;
		while (peers.hasMoreElements()) {
			peer = peers.nextElement();
			writeStream = peerList.get(peer);
			synchronized(writeStream) {
				try {
					writeStream.writeObject(payload);
					acksExpected++;
				} catch (IOException e) {
					peerList.remove(peer);
					e.printStackTrace();
				}
			}
		}
		
		return acksExpected;
	}

	private void connectToPeers(ArrayList<String> peerList) {
		this.peerList = new ConcurrentHashMap <String, ObjectOutputStream>();
		System.out.println("Peer size:" + peerList.size());
		
		for(String peer: peerList) {
			if(this.peerList.containsKey(peer)) continue;
			// Host, port, name are hyphen separated
			String [] connectionInfo = peer.split("-");
			
			String hostname = connectionInfo[0];
			String clientName = connectionInfo[2];
			int port = Integer.parseInt(connectionInfo[1]);
			int clientId = Integer.parseInt(connectionInfo[3]);
			
			System.out.println(peer);
			Socket socket;
			try {
				System.out.println(hostname + " " + port);
				socket = new Socket(hostname, port);
				ObjectInputStream readStream = new ObjectInputStream(socket.getInputStream());
				ObjectOutputStream writeStream = new ObjectOutputStream(socket.getOutputStream());
				
				this.peerList.put(peer, writeStream);
				RemoteClient client = new RemoteClient(clientName);
				EventQueueListener.remoteClients.put(clientId, client);
				maze.addClient(client, clientId);
				
				(new Thread (new IncomingMessageListenerThread(this, readStream, writeStream))).start();
			} catch (Exception e) {
				e.printStackTrace();
			}	
		}
		
		if (peerList.size() == 3) {
			MazewarClient.playing = true;
		}
	}
	
	public void start(ArrayList <String> peerList, int port) {
		connectToPeers(peerList);
		try {
			(new Thread (new MazewarServer(this, port))).start();
			(new Thread (new EventQueueListener(maze, localClient))).start();
			(new Thread (new MissileTick(this))).start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public synchronized void tick() {
		lastTick = 0;
	}
	
	public synchronized void noTick() {
		lastTick = lastTick + 200;
	}
}
