package nl.tudelft.bw4t.client.gui.listeners;

import eis.exceptions.ActException;

import java.awt.event.ActionEvent;

import nl.tudelft.bw4t.client.controller.ClientController;

import org.apache.log4j.Logger;

public class TraverseAndGetBlocksActionListener extends AbstractClientActionListener {
    /** Logger to report error messages to. */
    private static final Logger LOGGER = Logger.getLogger(TraverseAndGetBlocksActionListener.class);

    /**
     * @param controller - The {@link ClientController} to listen to and interact with.
     */
    public TraverseAndGetBlocksActionListener(ClientController controller) {
        super(controller);
    }

    @Override
    protected void actionWithHumanAgent(ActionEvent arg0) {
        System.out.println("TraverseAndGetBlocksActionListener - actionWithHumanAgent");
        new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println("TraverseAndGetBlocksActionListener - traverseAndGetBlocks");
                    getController().getHumanAgent().traverseAndGetBlocks();
                } catch (ActException e1) {
                    // Also catch NoServerException. Nothing we can do really.
                    LOGGER.error(e1); 
                }
            }
        }.start();
    }

    @Override
    protected void actionWithGoalAgent(ActionEvent arg0) {}
}
