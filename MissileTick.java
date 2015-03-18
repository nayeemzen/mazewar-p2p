public class MissileTick implements Runnable {
	
	private MazewarClient client;
	
	MissileTick(MazewarClient client) {
		this.client = client;
	}
	
	public void run() {
		while (true) {
			try {
				Thread.sleep(200);
	        } catch(Exception e) {
	            // shouldn't happen
	        }
			
			if (client.inElectionSince != 0 && System.nanoTime() - client.inElectionSince > 200 * 1000000) {
				client.inElectionSince = 0;
				client.isCoordinator = true;
			}
			
			if (client.isCoordinator) {
				client.sendEvent(ClientEvent.missileTick);
			} else {
				client.noTick();
				if (client.lastTick >= 800) {
					client.broadcastElection();
				}
			}
			
		}
	}
}
