package nl.tudelft.bw4t.client.agent;

import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Translator;
import eis.exceptions.ActException;
import eis.exceptions.EntityException;
import eis.exceptions.PerceiveException;
import eis.iilang.Action;
import eis.iilang.Parameter;
import eis.iilang.Percept;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import nl.tudelft.bw4t.Compare;
import nl.tudelft.bw4t.IAControllerInterface;
import nl.tudelft.bw4t.IAServerInterface;

import nl.tudelft.bw4t.RoomTime;
import nl.tudelft.bw4t.client.environment.PerceptsHandler;
import nl.tudelft.bw4t.client.environment.RemoteEnvironment;
import nl.tudelft.bw4t.client.gui.BW4TClientGUI;
import nl.tudelft.bw4t.client.message.BW4TMessage;
import nl.tudelft.bw4t.client.message.MessageTranslator;
import nl.tudelft.bw4t.map.view.ViewBlock;
import nl.tudelft.bw4t.map.view.ViewEntity;
import nl.tudelft.bw4t.scenariogui.BotConfig;
import nl.tudelft.bw4t.scenariogui.EPartnerConfig;

/**
 * Java agent that can control an entity.
 */
public class BW4TAgent extends UnicastRemoteObject implements ActionInterface, IAControllerInterface, Runnable {

    /**
     * The agent id.
     */
    protected String agentId;
    /**
     * The entity id
     */
    protected String entityId;
    /**
     * The environment killed.
     */
    protected boolean environmentKilled;
    /**
     * The bw4tenv.
     */
    private final RemoteEnvironment bw4tenv;
    private BotConfig botConfig;
    private EPartnerConfig epartnerConfig;
    private IAServerInterface server;
    private int goToCount, pickUpCount, putDownCount, askForCount, responseCount;

    /**
     * Create a new BW4TAgent that can be registered to an entity.
     *
     * @param agentId , the id of this agent used for registering to an entity.
     * @param env the remote environment.
     */
    public BW4TAgent(String agentId, RemoteEnvironment env) throws RemoteException {
        this.agentId = agentId;
        this.bw4tenv = env;
    }

    public BW4TAgent(String agentId, RemoteEnvironment env, IAServerInterface server) throws RemoteException {
        this.agentId = agentId;
        this.bw4tenv = env;
        this.server = server;
    }

    public void setServer(IAServerInterface server) {
        this.server = server;
    }
    private final List<String> places = new ArrayList<String>();
    private String state = "arrived"; // [ arrived | collided | traveling ]
    private int nextDestination = (int) (Math.random() * 1024);
    private int colorSequenceIndex = 0;
    private final Set<ViewBlock> visibleBlocks = new HashSet<>();
    private final List<String> colorSequence = new LinkedList<>();
    private boolean holding = false;
    //private boolean isActionPerforming = false;
    private int updateDelay = 41;
    protected Map<String, Integer> nameToIndex = new HashMap<>();
    protected Map<String, ArrayList<String>> memory = new HashMap<>();
    public int ROOMS;
    protected long[] timeStamp;
    protected int next = 0;
    protected PriorityQueue<RoomTime> queue = new PriorityQueue<>();
    protected int nextBlockIndex = 0;
    protected String holdingColor = null;
    protected String fastestResponceAgent = null;

    public List<String> getPlaces() {
        return places;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setColorSequenceIndex(int colorSequenceIndex) {
        this.colorSequenceIndex = colorSequenceIndex;
    }

    public Set<ViewBlock> getVisibleBlocks() {
        return visibleBlocks;
    }

    public List<String> getColorSequence() {
        return colorSequence;
    }

    public void setHolding(boolean holding) {
        this.holding = holding;
    }

    private boolean isArrived() {
        //long timeLimit = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(updateDelay);
            } catch (Exception ex) {
            }

            if (state.equals("arrived")) {
                state = "traveling";
                return true;
            }
            if (state.equals("collided")/* || System.currentTimeMillis() - timeLimit >3000*/) {
                state = "traveling";
                return false;
            }
        }
    }

    private boolean goToBlockIsArrived() {
        long timeLimit = System.currentTimeMillis();
        while (true) {
            try {
                Thread.sleep(updateDelay);
            } catch (Exception ex) {
            }

            if (state.equals("arrived") || System.currentTimeMillis() - timeLimit > 1500) {
                state = "traveling";
                return true;
            }
            if (state.equals("collided")) {
                state = "traveling";
                return false;
            }
        }
    }

    private boolean isHolding() {
        while (true) {
            try {
                Thread.sleep(updateDelay);
            } catch (Exception ex) {
            }

            if (holding) {
                holding = false;
                return true;
            }
        }
    }

    public void traverseAndGetBlocks() throws ActException {
        state = "traveling";
        while (true) {
            //goTo(places.get((int) (Math.random() * places.size())));
            goTo(places.get(nextDestination++ % places.size()));

            if (isArrived()) {
                for (ViewBlock b : visibleBlocks) {
                    if (b.getColor().getName().equalsIgnoreCase(colorSequence.get(colorSequenceIndex))) {
                        goToBlock(b.getObjectId());
                        isArrived();
                        pickUp();
                        isHolding();
                        do {
                            goTo("DropZone");
                        } while (!isArrived());
                        putDown();
                        break;
                    }
                }
            }

            try {
                Thread.sleep(updateDelay);
            } catch (Exception ex) {
            }

            // stop traversal
            if (colorSequenceIndex >= colorSequence.size()) {
                break;
            }
        }
    }

    public void handleMessagePercept(String message) {
        String[] s = message.split(", ");
        String receiver = s[0];
        String command = s[1];

        if (receiver.equals(entityId)) {
            // commands
            try {
                if (command.contains("go to room ")) {
                    String room = command.replaceAll("go to room ", "");
                    goTo(room);
                }
            } catch (Exception ex) {
            }
        }
    }

    /**
     * Register an entity to this agent.
     *
     * @param entityId , the Id of the entity
     */
    public void registerEntity(String entityId) {
        this.entityId = entityId;
    }

    /**
     * Run the reasoning process of this agent.
     */
    @Override
    public void run() {
        goToCount = pickUpCount = putDownCount = askForCount = responseCount = 0;
        try {
            Thread.sleep(5000); // for initialization
            ROOMS = places.size();
            timeStamp = new long[ROOMS];
            if (!environmentKilled) {
                //traverseAndGetBlocks();
                //System.out.println(agentId + ":" + this.server.getCurrentColor());
                traverse();
            }
        } catch (Exception ex) {
        }
        /*if (environmentKilled) {
         return;
         }*/
    }

    /**
     * Gets all agent with this type.
     *
     * @param type The type of the agent.
     * @return A list with the agents.
     */
    public List<BW4TAgent> getAgentsWithType(String type) {
        List<BW4TAgent> res = new LinkedList<BW4TAgent>();
        for (String agentName : bw4tenv.getAgents()) {
            try {
                BW4TAgent agent = bw4tenv.getRunningAgent(agentName);
                if (bw4tenv.getType(agent.getEntityId()).equals(type)) {
                    res.add(agent);
                }
            } catch (EntityException e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void goTo(double x, double y) throws ActException {
        try {
            Parameter[] xParam = Translator.getInstance().translate2Parameter(x);
            Parameter[] yParam = Translator.getInstance().translate2Parameter(y);
            Parameter[] parameters = new Parameter[2];
            parameters[0] = xParam[0];
            parameters[1] = yParam[0];
            bw4tenv.performEntityAction(entityId, new Action("goTo", parameters));
        } catch (RemoteException | TranslationException e) {
            ActException ex = new ActException("goTo", e);
            ex.setType(ActException.FAILURE);
            throw ex;
        } finally {
            goToCount++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void goToBlock(long id) throws ActException {
        try {
            Parameter[] idParam = Translator.getInstance().translate2Parameter(id);
            bw4tenv.performEntityAction(entityId, new Action("goToBlock", idParam));
        } catch (TranslationException | RemoteException e) {
            ActException ex = new ActException("goToBlock failed", e);
            ex.setType(ActException.FAILURE);
            throw ex;
        } finally {
            goToCount++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void goTo(String navPoint) throws ActException {
        try {
            Parameter[] idParam = Translator.getInstance().translate2Parameter(navPoint);
            bw4tenv.performEntityAction(entityId, new Action("goTo", idParam));
        } catch (TranslationException | RemoteException e) {
            ActException ex = new ActException("goTo failed", e);
            ex.setType(ActException.FAILURE);
            throw ex;
        } finally {
            goToCount++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void navigateObstacles() throws ActException {
        try {
            bw4tenv.performEntityAction(entityId, new Action("navigateObstacles"));
        } catch (RemoteException e) {
            ActException ex = new ActException("navigateObstacles failed", e);
            ex.setType(ActException.FAILURE);
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void pickUp() throws ActException {
        try {
            bw4tenv.performEntityAction(entityId, new Action("pickUp"));
        } catch (RemoteException e) {
            ActException ex = new ActException("pickUp failed", e);
            ex.setType(ActException.FAILURE);
            throw ex;
        } finally {
            pickUpCount++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putDown() throws ActException {
        try {
            bw4tenv.performEntityAction(entityId, new Action("putDown"));
        } catch (RemoteException e) {
            ActException ex = new ActException("putDown failed", e);
            ex.setType(ActException.FAILURE);
            throw ex;
        } finally {
            putDownCount++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendMessage(String receiver, BW4TMessage message) throws ActException {
        this.sendMessage(receiver, MessageTranslator.translateMessage(message));
    }

    /**
     * Sends a message to certain other agents.
     *
     * @param receiver , a receiver (can be either all or the id of another
     * agent)
     * @param message , the translated message (as String)
     * @throws ActException , if an attempt to perform an action has failed.
     */
    private void sendMessage(String receiver, String message) throws ActException {
        try {
            Parameter[] messageParam = Translator.getInstance().translate2Parameter(message);
            Parameter[] receiverParam = Translator.getInstance().translate2Parameter(receiver);
            Parameter[] parameters = new Parameter[2];
            parameters[0] = receiverParam[0];
            parameters[1] = messageParam[0];
            bw4tenv.performEntityAction(entityId, new Action("sendMessage", parameters));
        } catch (RemoteException | TranslationException e) {
            ActException ex = new ActException("putDown failed", e);
            ex.setType(ActException.FAILURE);
            throw ex;
        }
    }

    /**
     * Get all percepts for the associated entity.
     *
     * @return a list of percepts
     * @throws PerceiveException if there was a problem retrieving the percepts.
     */
    public List<Percept> getPercepts() throws PerceiveException {
        return PerceptsHandler.getAllPerceptsFromEntity(entityId, bw4tenv);
    }

    /**
     * Sets the killed.
     */
    public void setKilled() {
        environmentKilled = true;
    }

    /**
     * Gets the agent id.
     *
     * @return the agent id
     */
    public String getAgentId() {
        return agentId;
    }

    /**
     * Gets the entity id.
     *
     * @return the entity id
     */
    public String getEntityId() {
        return entityId;
    }

    /**
     * Gets the environment.
     *
     * @return the environment
     */
    public RemoteEnvironment getEnvironment() {
        return bw4tenv;
    }

    /**
     * Whether this agent can pick up another box based on their gripper
     * capacity and the amount of boxes they're already holding.
     *
     * @param sameEntity The {@link ViewEntity} type of this agent.
     * @return Whether this agent can pick up another object.
     */
    public boolean canPickupAnotherObject(ViewEntity sameEntity) {
        if (getBotConfig() == null) {
            return true;
        }
        if (getBotConfig().getGripperHandicap()) {
            return false;
        }
        int grippersTotal = getBotConfig().getGrippers();
        int grippersInUse = sameEntity.getHolding().size();
        return grippersInUse < grippersTotal;
    }

    /**
     * Checks if the bot controlled by the GUI can pick up another object
     *
     * @param gui The GUI controlling the bot that needs to be checked
     * @return Returns true if the bot can pick up another object
     */
    public boolean canPickupAnotherObject(BW4TClientGUI gui) {
        return canPickupAnotherObject(gui.getController().getMapController().getTheBot());
    }

    public boolean isColorBlind() {
        return getBotConfig() != null && getBotConfig().getColorBlindHandicap();
    }

    public BotConfig getBotConfig() {
        return botConfig;
    }

    public void setBotConfig(BotConfig botConfig) {
        this.botConfig = botConfig;
    }

    public boolean isGps() {
        return getEpartnerConfig() != null && getEpartnerConfig().isGps();
    }

    public EPartnerConfig getEpartnerConfig() {
        return epartnerConfig;
    }

    public void setEpartnerConfig(EPartnerConfig epartnerConfig) {
        this.epartnerConfig = epartnerConfig;
    }

    //
    public String getBot() throws RemoteException {
        return this.agentId;
    }

    public void receiveMessage(String s, String sender) throws RemoteException {
        String action = s.substring(0, 2);
        if (action.equals("00")) // display message
        {
            System.out.println(sender + ":" + s.substring(3));
        }
        if (action.equals("01")) // go to rooms
        {
            try {
                goTo(s.substring(3));
            } catch (Exception ex) {
            }
        }
    }

    public RoomTime colorInRoom(String color) throws RemoteException {
        PriorityQueue<RoomTime> queue = new PriorityQueue<RoomTime>(1, new Compare());
        for (int i = 0; i < ROOMS; i++) {
            try {
                if (memory.get(places.get(i)).contains(color.toLowerCase())) {
                    RoomTime temp = new RoomTime(places.get(i), Calendar.getInstance().getTimeInMillis() - timeStamp[nameToIndex.get(places.get(i))]);
                    queue.add(temp);
                }
            } catch (NullPointerException npe) {
            }
        }
        return queue.peek();
    }

    public void goToMostPossibleExistRoom(String room) throws RemoteException {
        if (room == null) {
            throw new NullPointerException();
        }
        System.out.println(room);
        try {
            goTo(room);
        } catch (Exception ex) {
        }
    }

    public void addToMemory(List<Percept> percepts, String room) {
        if (nameToIndex.containsKey(room)) {
            timeStamp[nameToIndex.get(room)] = Calendar.getInstance().getTimeInMillis();
        } else {
            timeStamp[next] = Calendar.getInstance().getTimeInMillis();
            nameToIndex.put(room, next++);
        }
        if (memory.keySet().contains(room)) {
            memory.get(room).clear();
        } else {
            memory.put(room, new ArrayList<String>());
        }
        for (Percept p : percepts) {
            //System.out.println(p.toProlog());
            String color = p.toProlog().substring(p.toProlog().indexOf(',') + 1, p.toProlog().indexOf(")")).toLowerCase();
            memory.get(room).add(color);
        }
    }

    public void removeFromMemory(String room, String color) throws RemoteException {
        memory.get(room).remove(color);
    }

    public boolean goToBlock(List<Percept> percepts, String room) throws Exception {
        System.out.println("adding to memory");
        addToMemory(percepts, room);
        System.out.println("finish add to memory");
        try {
            Thread.sleep(updateDelay);
        } catch (Exception ex) {
        }
        for (ViewBlock b : visibleBlocks) {
            if (b.getColor().getName().equalsIgnoreCase(colorSequence.get(nextBlockIndex))) {
                memory.get(room).remove(b.getColor().getName().toLowerCase());
                goToBlock(b.getObjectId());
                goToBlockIsArrived();
                pickUp();
                this.holdingColor = b.getColor().getName().toLowerCase();
                isHolding();
                if (this.colorSequence.get(nextBlockIndex).equalsIgnoreCase(this.holdingColor)) {
                    server.addAllAgentIndex();
                    do {
                        goTo("FrontDropZone");
                        System.out.println("in while loop(arrived)");
                    } while (!isArrived()/* && ias.getCurrent() < ias.getColors().length*/);
                    while (!this.colorSequence.get(this.colorSequenceIndex).equalsIgnoreCase(this.holdingColor)) {
                        System.out.println("in while loop(equals)");
                    }
                    System.out.println("out while loop");
                    goTo("DropZone");
                    System.out.println("before isArrived");
                    isArrived();
                    System.out.println("after isArrived");
                }
                putDown();
                this.holdingColor = null;
                //Thread.sleep(200);
                System.out.println("finish putting");
                //break;
                return true;
            }
        }
        return false;
    }

    public long getBlockId(String block) {
        if (block == null) {
            return -1;
        }
        return Long.parseLong(block.substring(6, block.indexOf(",")));
    }

    public String getBlockColor(String block) {
        if (block == null) {
            return null;
        }
        return block.substring(block.indexOf(",") + 1, block.length() - 1);
    }

    /*public void askAllBotForColor(String color) throws RemoteException {
     askForCount++;
     server.askForColor(this, color);
     }*/
    public synchronized void addNextBlockIndex() throws RemoteException {
        this.nextBlockIndex++;
        System.out.println("index updated to : " + this.nextBlockIndex);
    }

    public void traverse() {
        state = "traveling";
        String room = null;
        List<Percept> percepts = null;
        String color = null;
        int impossibleCount = 0;
        try {
            //System.setErr(new PrintStream("IA_err.txt"));
        } catch (Exception ex) {
        }

        while (true) {
            // stop traversal (prevent from exception)
            try {
                color = colorSequence.get(this.nextBlockIndex);
                System.out.println(agentId + " need: " + color);
            } catch (Exception ex) {
                System.out.println("Finish taking all blocks");
                break;
            }

            try {
                RoomTime temp = null;
                try {
                    //ask for self-memory
                    temp = colorInRoom(color);
                    System.out.println("in room " + temp.getRoom());
                } catch (Exception ex) {
                }

                if (temp == null) {
                    //ask for single bot's memory, default as Bot1
                    queue.clear();
                    askForCount++;
                    if (this.fastestResponceAgent == null) {
                        server.askForColor(this.agentId, color);
                    } else {
                        server.askForColor(this.agentId, color, this.fastestResponceAgent);
                    }
                    //wait for 1s response
                    Thread.sleep(1000);
                    room = queue.peek().getRoom();
                    System.out.println(fastestResponceAgent + " : in room " + room);
                } else {
                    room = temp.getRoom();
                }
                goTo(room);
            } catch (Exception ex) {
                System.err.println("no response");
                ex.printStackTrace();
                try {
                    goTo(places.get(nextDestination++ % ROOMS));
                } catch (Exception ex1) {
                    ex1.printStackTrace();
                }
                room = places.get((nextDestination - 1) % ROOMS);
            }
            if (!isArrived()) {
                continue;
            }

            // get all colors from room
            try {
                percepts = getPercepts();
            } catch (Exception ex) {
                System.err.println("Exception: traverse() - 1 " + agentId);
            }
            // pick & drop color
            try {

                if (!goToBlock(percepts, room)) {
                    //reset
                    impossibleCount++;
                    /*comment out due to boardcasting method
                     server.noSuchColor(room,color);
                     */
                    goTo("FrontDropZone");
                    Thread.sleep(updateDelay);
                } else {
                    impossibleCount = 0;
                }

                if (impossibleCount >= ROOMS) {
                    System.out.println("Impossible");
                    break;
                }

            } catch (Exception ex) {
                System.err.println("Exception: traverse() - 2 " + agentId);
                try {
                    PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("err.txt", true)));
                    ex.printStackTrace(out);
                    out.close();
                } catch (IOException e) {
                    System.err.println("IOException occur, log may not be saved into log.txt");
                }
                break;
            }
        }
        try {
            goTo("FrontDropZone");
        } catch (ActException ex) {
        }
        try {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("log.txt", true)));
            out.println(entityId + " goTo: " + goToCount + " pickUp: " + pickUpCount + " putDown: " + putDownCount + " askFor: " + askForCount + " response: " + responseCount);
            out.close();
        } catch (IOException e) {
            System.err.println("IOException occur, log may not be saved into log.txt");
        }
        try {
            System.out.println("reseting server");
            server.requestReset();
        } catch (Exception ex) {
            System.err.println("fail reseting server");
            ex.printStackTrace();
        }
    }

    public void receiveFromRoom(RoomTime r) throws RemoteException {
        if (r != null) {
            queue.add(r);
        }
    }

    public synchronized void addResponceCount() throws RemoteException {
        responseCount++;
    }

    public synchronized void setFastestResponceAgent(String agentId) throws RemoteException {
        if (fastestResponceAgent == null) {
            this.fastestResponceAgent = agentId;
        }
    }
}
