package nl.tudelft.bw4t.client;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IAServerInterface extends Remote {
    public String[] getColors() throws RemoteException;
    public void setColors(String[] colors) throws RemoteException;
    public int getCurrent() throws RemoteException;
    public void setCurrent(int current) throws RemoteException;
    public void putBox(String color) throws RemoteException;
    public String getCurrentColor() throws RemoteException;
    public void registerBot(String botName, IAControllerInterface bot) throws RemoteException;
    public void sendMessage(String s,String sender) throws RemoteException;
    public void sendMessage(String botName,String sender, String s) throws RemoteException;
    public String askForColor(IAControllerInterface self, String color) throws RemoteException;
    public void noSuchColor(String room, String color) throws RemoteException;
}
