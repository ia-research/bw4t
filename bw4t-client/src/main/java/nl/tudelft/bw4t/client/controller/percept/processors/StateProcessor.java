package nl.tudelft.bw4t.client.controller.percept.processors;

import eis.iilang.Identifier;
import eis.iilang.Parameter;

import java.util.List;

import nl.tudelft.bw4t.client.controller.ClientMapController;

public class StateProcessor implements PerceptProcessor {

    @Override
    public void process(List<Parameter> parameters, ClientMapController clientMapController) {
        String state = ((Identifier) parameters.get(0)).getValue();
        clientMapController.getClientController().getHumanAgent().setState(state);
    }

}
