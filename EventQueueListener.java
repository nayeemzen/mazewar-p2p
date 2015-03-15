import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;


public class EventQueueListener implements Runnable {
	
	private Maze maze;
	private GUIClient localClient;
	public static ConcurrentHashMap<Integer, RemoteClient> remoteClients  = new ConcurrentHashMap<Integer, RemoteClient>();
	
	public EventQueueListener(Maze maze, GUIClient localClient) {
		this.maze = maze;
		this.localClient = localClient;
	}

	public void run() {
		while(true) {
			MazewarPacket head = MazewarClient.eventQueue.peek();
			if(head == null) continue;
			if (!MazewarClient.ackMap.containsKey(head.packetId) && !MazewarClient.waitlist.contains(head.packetId)) {
				renderEvent(MazewarClient.eventQueue.remove());
			}
		}
	}

	private void renderEvent(MazewarPacket packet) {
		System.out.println("DEQUEUED EVENT: " + packet.eventType + " FROM: " + packet.clientName);
		if(packet.eventType == MazewarPacket.REGISTER) {
			registerClient(packet.clientName, packet.clientId);
			return;
		}
		
		if (packet.eventType == MazewarPacket.ACTION_MISSILE_TICK) {
			MazeImpl maze = (MazeImpl)this.maze;
			maze.missileTick();
			return;
		}
			
		RemoteClient remoteClient = null;
		boolean isLocalClient = packet.clientName == localClient.getName();
		
		if(!isLocalClient){
			remoteClient = remoteClients.get(packet.clientId);
			if(remoteClient == null) return;
		}

		
		// Render to view engine
		switch(packet.eventType) {
		case MazewarPacket.ACTION_MOVE_DOWN:
			if(isLocalClient)
				localClient.backup();
			else
				remoteClient.backup();
			break;
		case MazewarPacket.ACTION_MOVE_UP:
			if(isLocalClient)
				localClient.forward();
			else
				remoteClient.forward();
			break;
		case MazewarPacket.ACTION_TURN_LEFT:
			if(isLocalClient)
				localClient.turnLeft();
			else
				remoteClient.turnLeft();
			break;
		case MazewarPacket.ACTION_TURN_RIGHT:
			if(isLocalClient)
				localClient.turnRight();
			else
				remoteClient.turnRight();
			break;
		case MazewarPacket.ACTION_FIRE_PROJECTILE:
			if(isLocalClient) {
				System.out.println(localClient.getName() + "fired");
				localClient.fire();
			}
			else {
				System.out.println(localClient.getName() + "fired");
				remoteClient.fire();
			}
			break;
//		case MazewarPacket.QUIT:
//			assert(isLocalClient == false);
//			maze.removeClient(remoteClient);
//			connectedRemoteClients.remove(remoteClient.getName());
//			if (connectedRemoteClients.size() == 0) {
//				// No one else is playing anymore, quit the game.
//				Mazewar.quit();
//			}
//			break;
		default:
			System.err.println("Undefined event!");
		}
		
	}
	
	private void registerClient(String clientName, int clientId) {
		RemoteClient client = new RemoteClient(clientName);
		maze.addClient(client, clientId);
		remoteClients.put(clientId, client);
	}

}
