package nl.tudelft.bw4t;

import java.rmi.Remote;
import java.rmi.RemoteException;
import nl.tudelft.bw4t.RoomTime;

public interface IAControllerInterface extends Remote {
        public String getBot()throws RemoteException;
	public void receiveMessage(String s,String sender) throws RemoteException;
        public RoomTime colorInRoom(String color) throws RemoteException;
        public void goToMostPossibleExistRoom(String room) throws RemoteException;
        public void removeFromMemory(String room,String color) throws RemoteException;
        public void receiveFromRoom(RoomTime r) throws RemoteException;
}
