package nl.tudelft.bw4t.server;

import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;


import java.util.PriorityQueue;

//import nl.tudelft.bw4t.client.environment.BW4TEnvironmentListener;
//import nl.tudelft.bw4t.client.environment.Launcher;

public class IAServerImpl extends UnicastRemoteObject implements IAServerInterface {
    // PINK
    // WHITE
    // YELLOW
    // RED
    // BLUE
    // GREEN
    // ORANGE
    /*private String[] colors = new String[] {"YELLOW", "PINK", "YELLOW", "WHITE", "WHITE", "RED"};*/

    private String[] colors = new String[]{"ORANGE", "RED", "YELLOW", "BLUE", "WHITE",
        "YELLOW", "GREEN", "PINK", "WHITE", "RED",
        "BLUE", "YELLOW", "YELLOW", "WHITE", "YELLOW",
        "ORANGE", "ORANGE", "PINK", "WHITE", "GREEN"};
    private int current = 0;
    private Map<String, IAControllerInterface> bots = new HashMap<String, IAControllerInterface>();
    long t = 0;
    PrintWriter pw;

    public IAServerImpl() throws RemoteException {
        try {
            pw = new PrintWriter("IAServer_log.txt");
        } catch (Exception ex) {
        }
    }

    @Override
    synchronized public String[] getColors() throws RemoteException {
        return colors;
    }

    @Override
    synchronized public void setColors(String[] colors) throws RemoteException {
        this.colors = colors;
    }

    @Override
    synchronized public int getCurrent() throws RemoteException {
        System.out.println("getCurrent() : " + current);
        return current;
    }

    @Override
    synchronized public void setCurrent(int current) throws RemoteException {
        this.current = current;
    }

    @Override
    synchronized public void putBox(String color) throws RemoteException {
        System.out.println(color + "(current: " + current + ")");
        if (current < colors.length && color.equals(colors[current])) {
            current++;
        }
        if (current == colors.length) {
            try {
                pw.println((System.currentTimeMillis() - t) / 1000.0 + "sec(s)");
                pw.close();
            } catch (Exception ex) {
            }
            //System.out.println((System.currentTimeMillis() - t) / 1000.0 + "sec(s)");
        }
    }

    @Override
    synchronized public String getCurrentColor() throws RemoteException {
        if (t == 0) {
            t = System.currentTimeMillis();
            System.out.println("Stopwatch starts");
        }

        System.out.println("getCurrentColor()" + "(current: " + current + ")");
        if (current < colors.length) {
            return colors[current];
        }
        return null;
    }

    @Override
    synchronized public void registerBot(String botName, IAControllerInterface bot) throws RemoteException {
        bots.put(botName, bot);
    }

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

    public synchronized String askForColor(IAControllerInterface self, String color) throws RemoteException{
        PriorityQueue<RoomTime> queue = new PriorityQueue<RoomTime>(1,new Compare());
        for (IAControllerInterface bot : bots.values()) {
            //if(!bot.equals(self))
            try{
                queue.add((RoomTime)(bot.colorInRoom(color)));
            }catch(Exception e){}
        }
        try{
            self.goToMostPossibleExistRoom(queue.peek().getRoom());
            return queue.peek().getRoom();
        }catch(NullPointerException npe){
            self.goToMostPossibleExistRoom(null);
            return null;
        }
    }
    
    public synchronized void noSuchColor(String room, String color) throws RemoteException{
        for (IAControllerInterface bot : bots.values()) {
            try{
                bot.removeFromMemory(room,color);
            }catch(Exception ex){}
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
