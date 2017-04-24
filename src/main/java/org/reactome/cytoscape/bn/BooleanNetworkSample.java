/*
 * Created on Apr 21, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.util.Map;

import org.reactome.booleannetwork.BooleanVariable;

/**
 * Used to model one simulation and its initial condition.
 * @author gwu
 *
 */
public class BooleanNetworkSample {
    private Map<BooleanVariable, Number> stimulationVarToValue;
    private Map<BooleanVariable, Number> respondVarToValue;
    
    /**
     * Default constructor.
     */
    public BooleanNetworkSample() {
    }

    public Map<BooleanVariable, Number> getStimulationVarToValue() {
        return stimulationVarToValue;
    }

    public void setStimulationVarToValue(Map<BooleanVariable, Number> stimulationVarToValue) {
        this.stimulationVarToValue = stimulationVarToValue;
    }
    
    public Map<BooleanVariable, Number> getRespondVarToValue() {
        return respondVarToValue;
    }

    public void setRespondVarToValue(Map<BooleanVariable, Number> respondVarToValue) {
        this.respondVarToValue = respondVarToValue;
    }
    
    
    
}
