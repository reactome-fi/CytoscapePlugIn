/*
 * Created on Jan 13, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Variable;

/**
 * Inference results for a FactorGraph object for the whole data set.
 * @author gwu
 *
 */
public class FactorGraphInferenceResults {
    private FactorGraph factorGraph;
    private Map<Variable, VariableInferenceResults> varToResults;
    // A flag indicating this result should be processed based on two cases
    private boolean usedForTwoCases;
    // Map samples to types
    private Map<String, String> sampleToType;
    
    /**
     * Default constructor.
     */
    public FactorGraphInferenceResults() {
        varToResults = new HashMap<Variable, VariableInferenceResults>();
    }

    public Map<String, String> getSampleToType() {
        return sampleToType;
    }

    public void setSampleToType(Map<String, String> sampleToType) {
        this.sampleToType = sampleToType;
    }

    public boolean isUsedForTwoCases() {
        return usedForTwoCases;
    }

    public void setUsedForTwoCases(boolean usedForTwoCases) {
        this.usedForTwoCases = usedForTwoCases;
    }

    public FactorGraph getFactorGraph() {
        return factorGraph;
    }

    public void setFactorGraph(FactorGraph factorGraph) {
        this.factorGraph = factorGraph;
    }

    public Map<Variable, VariableInferenceResults> getVarToResults() {
        return varToResults;
    }
    
    public VariableInferenceResults getVariableInferenceResults(Variable var) {
        return varToResults.get(var);
    }
    
    /**
     * Get inference results for a collection variables.
     * @param variables
     * @return
     */
    public List<VariableInferenceResults> getVariableInferenceResults(Collection<Variable> variables) {
        List<VariableInferenceResults> rtn = new ArrayList<VariableInferenceResults>();
        if (varToResults != null) {
            for (Variable var : variables) {
                VariableInferenceResults varResults = varToResults.get(var);
                if (varResults != null)
                    rtn.add(varResults);
            }
        }
        return rtn;
    }

    public void setVarToResults(Map<Variable, VariableInferenceResults> varToResults) {
        if (varToResults == null)
            this.varToResults.clear(); // We don't want to keep a null object here. 
        else
            this.varToResults = varToResults;
    }
    
    public void addVarToResults(VariableInferenceResults varResults) {
        varToResults.put(varResults.getVariable(), varResults);
    }
    
    /**
     * Call this method after an inference is done on the wrapped FactorGraph object to store the results.
     */
    public void storeInferenceResults(String sample) {
        if (sample == null) {
            for (Variable var : factorGraph.getVariables()) {
                setVariablePrior(var);
            }
        }
        else {
            for (Variable var : factorGraph.getVariables()) {
                storeInferenceResults(sample, var);
            }
        }
    }
    
    private void setVariablePrior(Variable var) {
        VariableInferenceResults varResults = getVarResults(var);
        varResults.setPriorValues(var.getBelief());
    }
    
    private void storeInferenceResults(String sample, Variable var) {
        VariableInferenceResults varResults = getVarResults(var);
        varResults.addSampleToValue(sample, var.getBelief());
    }
    
    private VariableInferenceResults getVarResults(Variable var) {
        VariableInferenceResults varResults = varToResults.get(var);
        if (varResults == null) {
            varResults = new VariableInferenceResults();
            varResults.setVariable(var);
            varToResults.put(var, varResults);
        }
        return varResults;
    }
    
    /**
     * Get the set of samples touched by this object.
     * @return
     */
    public Set<String> getSamples() {
        Set<String> samples = new HashSet<String>();
        if (varToResults != null) {
            for (VariableInferenceResults varResults : varToResults.values()) {
                samples.addAll(varResults.getPosteriorValues().keySet());
            }
        }
        return samples;
    }
    
    public boolean hasPosteriorResults() {
        if (varToResults == null || varToResults.size() == 0)
            return false;
        VariableInferenceResults varResult = varToResults.values().iterator().next();
        if (varResult.getPosteriorValues() == null || varResult.getPosteriorValues().size() == 0)
            return false;
        return true;
    }
    
    public void clear() {
        varToResults.clear();
    }
    
}
