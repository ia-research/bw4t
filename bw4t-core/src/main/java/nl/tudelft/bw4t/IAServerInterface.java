package nl.tudelft.bw4t;

import eis.exceptions.ManagementException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import nl.tudelft.bw4t.map.BlockColor;
import nl.tudelft.bw4t.network.BW4TClientActions;

public interface IAServerInterface extends Remote {
    public void registerClient(String agentId, BW4TClientActions client) throws RemoteException;
    //public void registerBot(String botName, IAControllerInterface bot) throws RemoteException;
    /*
    public void sendMessage(String s,String sender) throws RemoteException;
    public void sendMessage(String botName,String sender, String s) throws RemoteException;
    */
    public void askForColor(String self, String color, String bot) throws RemoteException;
    //broadcasting method
    public void askForColor(String self, String color) throws RemoteException;
    public void noSuchColor(String room, String color) throws RemoteException;
    public void requestReset()throws RemoteException, ManagementException;
    public void addAllAgentIndex() throws RemoteException;
    public void reset() throws RemoteException;
}
