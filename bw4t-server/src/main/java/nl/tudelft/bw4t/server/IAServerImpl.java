package nl.tudelft.bw4t.server;

import eis.exceptions.ManagementException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import nl.tudelft.bw4t.IAControllerInterface;
import nl.tudelft.bw4t.IAServerInterface;
import nl.tudelft.bw4t.RoomTime;
import nl.tudelft.bw4t.network.BW4TClientActions;
import nl.tudelft.bw4t.server.environment.BW4TEnvironment;
import nl.tudelft.bw4t.server.environment.Stepper;

//import nl.tudelft.bw4t.client.environment.BW4TEnvironmentListener;
//import nl.tudelft.bw4t.client.environment.Launcher;
public class IAServerImpl extends UnicastRemoteObject implements IAServerInterface {

    private Map<String, BW4TClientActions> clients = new HashMap<>();
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
    long t = System.currentTimeMillis();
    static final long TIMELIMIT = 5 * 60 * 1000;
    int requestResetCount = 0;
    //PrintWriter pw;
    // bw4t directory
    private String dir = "E:/Y2_comp/research/sharedFolder/bw4t";
    private int maxTimes = 200;
    private static int times = 1; // do not modify
    private int agentNo = 2; //number of agents to be called after reset

    public IAServerImpl() throws RemoteException {
        try {
            //pw = new PrintWriter("IAServer_log.txt");
        } catch (Exception ex) {
        }
    }

    @Override
    public synchronized void registerClient(String agentId, BW4TClientActions client) throws RemoteException {
        clients.put(agentId, client);
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
    public synchronized void askForColor(String selfAgentId, String color, String agentId) throws RemoteException {
        clients.get(agentId).addResponceCount();
        RoomTime temp = (RoomTime) (clients.get(agentId).colorInRoom(color));
        clients.get(selfAgentId).receiveFromRoom(temp);
        clients.get(selfAgentId).setFastestResponceAgent(agentId);
    }

    public synchronized void askForColor(String selfAgentId, String color) throws RemoteException {
        for (String agentId : clients.keySet()) {
            if (!agentId.equals(selfAgentId)) {
                try {
                    askForColor(selfAgentId,color,agentId);
                } catch (Exception e) {
                }
            }
        }
    }

    public synchronized void noSuchColor(String room, String color) throws RemoteException {
        /*for (IAControllerInterface bot : bots.values()) {
         try{
         bot.removeFromMemory(room,color);
         }catch(Exception ex){}
         }*/
        System.out.println("noSuchColor");
    }

    public synchronized void requestReset() throws RemoteException, ManagementException {
        requestResetCount++;
        System.out.println("request reset");
        if (requestResetCount == clients.size() && times < maxTimes) {
            try {
                PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("../bw4t-client/log.txt", true)));
                out.println("log for "+times);
                out.println("===============================================================");

                out.close();
            } catch (IOException ex) {
            }
            BW4TEnvironment.getInstance().reset(true);
            try {
                Thread.sleep(1000);
                BW4TEnvironment.getInstance().setTps(Stepper.MAX_TPS);
                ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/c");
                processBuilder.directory(new File(dir + "/bw4t-client"));
                processBuilder.command("java", "-cp", "target/bw4t-client-3.5.0-jar-with-dependencies.jar", "nl.tudelft.bw4t.client.environment.RemoteEnvironment");

                for (int i = 0; i < agentNo; i++) {
                    processBuilder.start();
                    Thread.sleep(1000);
                }
                times++;
            } catch (Exception ex) {
            }

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

    public synchronized void addAllAgentIndex() throws RemoteException {
        for (BW4TClientActions c : clients.values()) {
            try{
                c.addNextBlockIndex();
            }catch(Exception ex){}
        }
    }
}
