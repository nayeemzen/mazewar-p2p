public class MissileTick implements Runnable {
	
	private MazewarClient client;
	
	MissileTick(MazewarClient client) {
		this.client = client;
	}
	
	public void run() {
		while (true) {
			client.sendEvent(ClientEvent.missileTick);
			try {
				Thread.sleep(200);
	        } catch(Exception e) {
	            // shouldn't happen
	        }
		}
	}
}
