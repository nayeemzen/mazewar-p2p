import java.io.Serializable;

public class MazewarPacket implements Serializable {
	
	public MazewarPacket() {}
	
	public MazewarPacket(MazewarPacket packet) {
		clientId = packet.clientId;
		timestamp = packet.timestamp;
		eventType = packet.eventType;
		errorCode = packet.errorCode;
		packetType = packet.packetType;
		md5 = packet.md5;
		clientName = packet.clientName;
		clientId = packet.clientId;
		score = packet.score;	
	}
	
	/* Client action events */
	public static final int ACTION_TURN_LEFT  		= 101;
	public static final int ACTION_TURN_RIGHT 		= 102;
	public static final int ACTION_MOVE_UP    		= 103;
	public static final int ACTION_MOVE_DOWN  		= 104;
	public static final int ACTION_FIRE_PROJECTILE	= 105;
	public static final int ACTION_MISSILE_TICK	= 106;
	
	/* Event to sync projectile movements between clients */
	public static final int SYNC_PROJECTILE_MOVEMENT = 200;
	
	/* Events */
	public static final int REGISTER	= 300;
	public static final int BEGIN		= 301;
	public static final int ERROR		= 302;
	public static final int QUIT	 	= 303;
	
	/* Packet Types (for Distributed Mutex) */
	public static final int REQUEST = 400;
	public static final int ACK = 401;
	public static final int RELEASE = 402;

	
	/* Error codes */
	public static final int ERROR_TIMEOUT 			  	 = -100;
	public static final int ERROR_CLIENT_ALREADY_EXISTS  = -101;
	
	/* Packet information variables */
	public int timestamp;
	public int eventType;
	public int errorCode;
	public int packetType;
	public String md5;
	public String clientName;
	
	/* Client information variables */
	public int clientId;
	public int score;
	
}
