import java.io.Serializable;
import java.util.ArrayList;


public class NamingServicePacket implements Serializable {
	public int clientID;
	public String clientName;
	public int port;
	public String packetType;
	public ArrayList <String> connectedClients;
	
	NamingServicePacket(int clientID, ArrayList<String> connectedClients) {
		this.clientID = clientID;
		this.connectedClients = connectedClients;
	}
	
	NamingServicePacket(String packetType, String clientName, int port) {
		this.packetType = packetType;
		this.port = port;
		this.clientName = clientName;
	}
	
	public String toString() {
		return clientID + "," + port + "," + packetType + "," + connectedClients;
	}
}
