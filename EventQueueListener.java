import java.util.PriorityQueue;


public class EventQueueListener implements Runnable {
	
	Maze maze;
	
	public EventQueueListener(Maze maze) {
		this.maze = maze;
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
		// Render to view engine
		switch(packet.eventType) {
		case MazewarPacket.REGISTER:
			registerClient(packet.clientName, packet.clientId);
		}
		
	}

	private void registerClient(String clientName, int clientId) {
		RemoteClient client = new RemoteClient(clientName);
		maze.addClient(client, clientId);
	}

}
