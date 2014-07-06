package nl.tudelft.bw4t.client.controller.percept.processors;

import eis.iilang.Identifier;
import eis.iilang.Parameter;

import java.util.List;

import nl.tudelft.bw4t.client.controller.ClientMapController;

public class PlaceProcessor implements PerceptProcessor {

    @Override
    public void process(List<Parameter> parameters, ClientMapController clientMapController) {
        String place = ((Identifier) parameters.get(0)).getValue();
        if (place.startsWith("Room"))
            clientMapController.getClientController().getHumanAgent().getPlaces().add(place);
    }

}
