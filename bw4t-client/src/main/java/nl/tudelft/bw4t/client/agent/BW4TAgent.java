package nl.tudelft.bw4t.client.agent;

import eis.eis2java.exception.TranslationException;
import eis.eis2java.translation.Translator;
import eis.exceptions.ActException;
import eis.exceptions.EntityException;
import eis.exceptions.PerceiveException;
import eis.iilang.Action;
import eis.iilang.Parameter;
import eis.iilang.Percept;
import java.rmi.RemoteException;
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
import nl.tudelft.bw4t.IAServerInterface;

/**
 * Java agent that can control an entity.
 */
public class BW4TAgent extends Thread implements ActionInterface, IAControllerInterface {

    /** The agent id. */
    protected String agentId;
    /** The entity id */
    protected String entityId;
    
    /** The environment killed. */
    protected boolean environmentKilled;
    
    /** The bw4tenv. */
    private final RemoteEnvironment bw4tenv;
    
    private BotConfig botConfig;
    private EPartnerConfig epartnerConfig;
    private IAServerInterface server;

    /**
     * Create a new BW4TAgent that can be registered to an entity.
     * 
     * @param agentId
     *            , the id of this agent used for registering to an entity.
     * @param env
     *            the remote environment.
     */
    public BW4TAgent(String agentId, RemoteEnvironment env) {
        this.agentId = agentId;
        this.bw4tenv = env;
    }
    
    public BW4TAgent(String agentId, RemoteEnvironment env, IAServerInterface server) {
        this.agentId = agentId;
        this.bw4tenv = env;
        this.server = server;
    }
    
    public void setServer(IAServerInterface server){
        this.server = server;
    }
    
    private final List<String> places = new ArrayList<String>();
    
    private String state = "arrived"; // [ arrived | collided | traveling ]
    
    private int nextDestination = 0;
    
    private int colorSequenceIndex = 0;
    
    private final Set<ViewBlock> visibleBlocks = new HashSet<>();
    
    private final List<String> colorSequence = new LinkedList<>();
    
    private boolean holding = false;
    
    private boolean isActionPerforming = false;
    
    private int updateDelay = 41;
    
    protected Map<String, Integer> nameToIndex = new HashMap<>();
    protected Map<String, Set<String>> memory = new HashMap<>();
    protected static String[] rooms = new String[]{"RoomA1", "RoomA2", "RoomA3",
        "RoomB1", "RoomB2", "RoomB3",
        "RoomC1", "RoomC2", "RoomC3"};
    public static final int ROOMS = rooms.length;
    protected long[] timeStamp = new long[ROOMS];
    protected int next = 0;
    protected PriorityQueue<RoomTime> queue;
    
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
        while (true) {
            try {
                Thread.sleep(updateDelay);
            } catch (Exception ex) {}
            
            if (state.equals("arrived")) {
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
            } catch (Exception ex) {}
            
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
                for (ViewBlock b: visibleBlocks) {
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
            } catch (Exception ex) {}
            
            // stop traversal
            if (colorSequenceIndex >= colorSequence.size())
                break;
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
            } catch (Exception ex) {}
        }
    }
    
    /**
     * Register an entity to this agent.
     *
     * @param entityId            , the Id of the entity
     */
    public void registerEntity(String entityId) {
        this.entityId = entityId;
    }

    /**
     * Run the reasoning process of this agent.
     */
    @Override
    public void run() {
        try {
            Thread.sleep(5000); // for initialization
            
            if (!environmentKilled) {
                //traverseAndGetBlocks();
                //System.out.println(agentId + ":" + this.server.getCurrentColor());
                traverse();
            }
        } catch (Exception ex) {}
        /*if (environmentKilled) {
            return;
        }*/
    }
    
    /**
     * Gets all agent with this type.
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
     * @param receiver            , a receiver (can be either all or the id of another agent)
     * @param message            , the translated message (as String)
     * @throws ActException               , if an attempt to perform an action has failed.
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
     * Whether this agent can pick up another box based on their
     * gripper capacity and the amount of boxes they're already
     * holding. 
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
     * @param gui
     *         The GUI controlling the bot that needs to be checked
     * @return
     *         Returns true if the bot can pick up another object
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
        for (int i = 0; i < rooms.length; i++) {
            try {
                if (memory.get(rooms[i]).contains(color)) {
                    RoomTime temp = new RoomTime(rooms[i], Calendar.getInstance().getTimeInMillis() - timeStamp[nameToIndex.get(rooms[i])]);
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
        System.out.println("in addToMemroy for "+room);
        System.out.println("testing percepts");
        for(Percept p : percepts){
            System.out.println(p.toProlog());
        }
        if (nameToIndex.containsKey(room)) {
            System.out.println("Updating Timestamp");
            timeStamp[nameToIndex.get(room)] = Calendar.getInstance().getTimeInMillis();
        } else {
            System.out.println("Creating new Timestamp");
            timeStamp[next] = Calendar.getInstance().getTimeInMillis();
            nameToIndex.put(room, next++);
        }
        if (memory.keySet().contains(room)) {
            System.out.println("clearing memory of "+room);
            memory.get(room).clear();
        } else {
            System.out.println("Creating new memory for "+room);
            memory.put(room, new HashSet<String>());
        }
        for (Percept p : percepts) {
            //System.out.println(p.toProlog());
            String color = p.toProlog().substring(p.toProlog().indexOf(',') + 1, p.toProlog().indexOf(")"));
            System.out.println("adding color "+color+" to memory");
            memory.get(room).add(color);
        }
        System.out.println("Finish adding");
    }

    public void removeFromMemory(String room, String color) throws RemoteException {
        memory.get(room).remove(color);
    }

    public boolean goToBlock() throws Exception {
        for (ViewBlock b: visibleBlocks) {
            if (b.getColor().getName().equalsIgnoreCase(colorSequence.get(colorSequenceIndex))) {
                goToBlock(b.getObjectId());

                isArrived();
                pickUp();
                isHolding();

                do {
                    goTo("DropZone");
                } while (!isArrived()/* && ias.getCurrent() < ias.getColors().length*/);

                putDown();
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
    
    public void askAllBotForColor(String color) throws RemoteException{
        server.askForColor(this, color);
    }
    
    public void traverse() {
        String room=null;
        List<Percept> percepts = null;
        String color=null;
        int impossibleCount=0;
        try {
            //System.setErr(new PrintStream("IA_err.txt"));
        } catch (Exception ex) {
        }

        while (true) {
            // stop traversal (prevent from exception)
            try {
                color = colorSequence.get(colorSequenceIndex);
                System.out.println(agentId + " need: " + color);
            } catch (Exception ex) {
                System.out.println("Finish taking all blocks");
                break;
            }
            
            try{
                RoomTime temp=null;
                try{
                    //ask for self-memory
                    temp = colorInRoom(color);
                    System.out.println("in room "+temp.getRoom());
                }catch(Exception ex){}
                
                if(temp==null) {
                    //ask for single bot's memory, default as Bot1
                    queue.clear();
                    server.askForColor(this,color, "Human_1");
                    //wait for 1s responce from bot Human_1
                    Thread.sleep(1000);
                    room=queue.peek().getRoom();
                    System.out.println("Bot1: in room "+room);
                    //ask for other bots' memory
                    /*
                    queue.clear();
                    server.askForColor(this,color);
                    //wait for 3s responce
                    Thread.sleep(3000);
                    room=queue.peek().getRoom();
                    */
                }else {
                    room=temp.getRoom();
                }
                goTo(room);
            }catch(Exception ex){
                try {
                    goTo(rooms[nextDestination++ % rooms.length]);
                } catch (ActException ex1) {}
                room=rooms[(nextDestination-1)%rooms.length];
            }
            if (!isArrived()) {
                try {
                    //retry
                    goTo(room);
                } catch (ActException ex) {}
                if(!isArrived())
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
                
                if(!goToBlock()){
                    //reset
                    impossibleCount++;
                    /*comment out due to boardcasting method
                        server.noSuchColor(room,color);
                    */
                    goTo("FrontDropZone");
                    Thread.sleep(updateDelay);
                }else{
                    impossibleCount=0;
                }
                
                if(impossibleCount>=rooms.length){
                    System.out.println("Impossible");
                    break;
                }
                /*
                 // stop traversal
                 if (ias.getCurrent() >= ias.getColors().length)
                 break;
                 */
            } catch (Exception ex) {
                System.err.println("Exception: traverse() - 2 " + agentId);
            }finally{
                System.out.println("adding to memory");
                addToMemory(percepts, room);
                System.out.println("finish add to memory");
                try {
                    Thread.sleep(updateDelay);
                } catch (Exception ex) {}
            }
        }
        try {
            goTo("FrontDropZone");
        } catch (ActException ex) {}
    }
    
    public void receiveFromRoom(RoomTime r) throws RemoteException{
        queue.add(r);
    }
}
