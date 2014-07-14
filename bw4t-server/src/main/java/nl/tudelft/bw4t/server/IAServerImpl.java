package nl.tudelft.bw4t.server;

import eis.exceptions.ManagementException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tudelft.bw4t.IAControllerInterface;
import nl.tudelft.bw4t.IAServerInterface;

import nl.tudelft.bw4t.RoomTime;
import nl.tudelft.bw4t.network.BW4TClientActions;

import nl.tudelft.bw4t.server.environment.BW4TEnvironment;


//import nl.tudelft.bw4t.client.environment.BW4TEnvironmentListener;
//import nl.tudelft.bw4t.client.environment.Launcher;

public class IAServerImpl extends UnicastRemoteObject implements IAServerInterface {
    private BW4TClientActions client;
    
    // PINK
    // WHITE
    // YELLOW
    // RED
    // BLUE
    // GREEN
    // ORANGE
    /*private String[] colors = new String[] {"YELLOW", "PINK", "YELLOW", "WHITE", "WHITE", "RED"};*/

    /*
    private String[] colors = new String[]{"ORANGE", "RED", "YELLOW", "BLUE", "WHITE",
        "YELLOW", "GREEN", "PINK", "WHITE", "RED",
        "BLUE", "YELLOW", "YELLOW", "WHITE", "YELLOW",
        "ORANGE", "ORANGE", "PINK", "WHITE", "GREEN"};
    */
    //private int current = 0;
    //private Map<String, IAControllerInterface> bots = new HashMap<String, IAControllerInterface>();
    long t = 0;
    int requestResetCount =0;
    //PrintWriter pw;

    public IAServerImpl() throws RemoteException {
        try {
            //pw = new PrintWriter("IAServer_log.txt");
        } catch (Exception ex) {
        }
    }
    
    @Override
    public void registerClient(BW4TClientActions client) throws RemoteException {
        this.client = client;
    }


/*
    @Override
    synchronized public void registerBot(String botName, IAControllerInterface bot) throws RemoteException {
        client.addAgent(botName, bot);
    }
*/
/*
    @Override
    synchronized public void sendMessage(String s, String sender) throws RemoteException {
        for (IAControllerInterface bot : bots.values()) {
            bot.receiveMessage(s,sender);
        }
    }

    @Override
    public void sendMessage(String botName, String sender , String s) throws RemoteException {
        for (String bot : bots.keySet()) {
            if (bot.equals(botName)) {
                bots.get(bot).receiveMessage(s,sender);
            }
        }
    }
*/
    public synchronized void askForColor(IAControllerInterface self, String color, String bot)throws RemoteException{
        self.receiveFromRoom((RoomTime)client.colorInRoom(bot,color));
    }
    /*
    public synchronized void askForColor(IAControllerInterface self, String color) throws RemoteException{
        for (IAControllerInterface bot : bots.values()) {
            if(!bot.equals(self)) {
                try{
                    self.receiveFromRoom((RoomTime)(bot.colorInRoom(color)));
                    bot.addResponceCount();
                }catch(Exception e){}
            }
        }
    }
    */
    public synchronized void noSuchColor(String room, String color) throws RemoteException{
        /*for (IAControllerInterface bot : bots.values()) {
            try{
                bot.removeFromMemory(room,color);
            }catch(Exception ex){}
        }*/
        System.out.println("noSuchColor");
    }
    
    
    public synchronized void requestReset()throws RemoteException, ManagementException{
        requestResetCount++;
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("log.txt", true)));
            out.println(client.getAgentSize());
            out.close();
        } catch (IOException ex) {
            Logger.getLogger(IAServerImpl.class.getName()).log(Level.SEVERE, null, ex);
        } 
        System.out.println("request reset");
        if(requestResetCount == client.getAgentSize()){
            BW4TEnvironment.getInstance().reset(true);
        }
            
    }
    /*public static Identifier findParameter(String[] args, InitParam param) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("-" + param.nameLower())) {
                //LOGGER.debug("Found parameter '" + param.nameLower() + "' with '" + args[(i + 1)] + "'");
                return new Identifier(args[(i + 1)]);
            }
        }
        //LOGGER.debug("Defaulting parameter '" + param.nameLower() + "' with '" + param.getDefaultValue() + "'");
        return null;
    }*/
    /*
    public static String findArgument(String[] args, InitParam param) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equalsIgnoreCase("-" + param.nameLower())) {
                return args[(i + 1)];
            }
        }
        return param.getDefaultValue();
    }
*/
/*    public static void main(String[] args) throws RemoteException, ManagementException, NoEnvironmentException {
        /*Map<String, Parameter> init = new HashMap();
        for (InitParam param : InitParam.values()) {
            if (param == InitParam.GOAL) {
                //LOGGER.info("Setting parameter 'GOAL' with 'false' because we started from commandline.");
                init.put(param.nameLower(), new Identifier("false"));
            } else {
                Parameter value = findParameter(args, param);
                if (value != null) {
                    init.put(param.nameLower(), value);
                }
            }
        }
        Launcher.launch(args);

        Map<String, Parameter> initParameters = new HashMap<String, Parameter>();
        try {
            for (InitParam param : InitParam.values()) {
                initParameters.put(param.nameLower(), new Identifier(
                        findArgument(args, param)));
            }
            BW4TRemoteEnvironment env = new BW4TRemoteEnvironment();
            env.attachEnvironmentListener(new BW4TEnvironmentListener(env));
            env.init(initParameters);
        }catch(Exception ex){}
        IAServerImpl server = new IAServerImpl();
        Registry registry;

        try {
            registry = LocateRegistry.createRegistry(8001);
        } catch (Exception ex) {
            registry = LocateRegistry.getRegistry(8001);
        }

        registry.rebind("IAServer", server);
        System.out.println("IAServer ready");
    }*/

}
