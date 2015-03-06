import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicInteger;


public class ClientEventQueue implements Comparator<MazewarPacket> {
	
	public static PriorityQueue<MazewarPacket> eventQueue = new PriorityQueue<MazewarPacket>(0, new ClientEventQueue());
	
	public int compare(MazewarPacket o1, MazewarPacket o2) {
		return o1.timestamp - o2.timestamp;
	}

}
