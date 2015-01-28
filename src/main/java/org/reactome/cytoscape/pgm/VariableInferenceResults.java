/*
 * Created on Jan 13, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Variable;

/**
 * Inference results for one Variable object across a data set.
 * @author gwu
 *
 */
public class VariableInferenceResults {
    private Variable variable;
    private Map<String, List<Double>> sampleToValues;
    private List<Double> priorValues;
    
    /**
     * Default constructor.
     */
    public VariableInferenceResults() {
    }

    public Variable getVariable() {
        return variable;
    }

    public void setVariable(Variable variable) {
        this.variable = variable;
    }

    public Map<String, List<Double>> getSampleToValues() {
        return sampleToValues;
    }

    public void setSampleToValues(Map<String, List<Double>> sampleToValues) {
        this.sampleToValues = sampleToValues;
    }
    
    public void clear() {
        if (sampleToValues != null)
            sampleToValues.clear();
    }

    public List<Double> getPriorValues() {
        return priorValues;
    }

    public void setPriorValues(List<Double> priorValues) {
        this.priorValues = priorValues;
    }
    
    public void setPriorValues(double[] values) {
        setPriorValues(PlugInUtilities.convertArrayToList(values));
    }
    
    public void addSampleToValue(String sample, List<Double> values) {
        if (sampleToValues == null)
            sampleToValues = new HashMap<String, List<Double>>();
        sampleToValues.put(sample, values);
    }
    
    public void addSampleToValue(String sample, double[] values) {
        addSampleToValue(sample, PlugInUtilities.convertArrayToList(values));
    }
    
    public Map<String, List<Double>> getPosteriorValues() {
        Map<String, List<Double>> rtn = new HashMap<String, List<Double>>();
        if (sampleToValues != null) {
            for (String sample : sampleToValues.keySet()) {
                if (!sample.startsWith(NetworkObservationDataHelper.RANDOM_SAMPLE_PREFIX))
                    rtn.put(sample, sampleToValues.get(sample));
            }
        }
        return rtn;
    }
    
    public Map<String, List<Double>> getRandomPosteriorValues() {
        Map<String, List<Double>> rtn = new HashMap<String, List<Double>>();
        if (sampleToValues != null) {
            for (String sample : sampleToValues.keySet()) {
                if (sample.startsWith(NetworkObservationDataHelper.RANDOM_SAMPLE_PREFIX))
                    rtn.put(sample, sampleToValues.get(sample));
            }
        }
        return rtn;
    }
    
}
