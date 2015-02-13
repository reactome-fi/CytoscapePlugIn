/*
 * Created on Jan 13, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Variable;

/**
 * Inference results for one Variable object across a data set.
 * @author gwu
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class VariableInferenceResults {
    @XmlIDREF
    private Variable variable;
    @XmlJavaTypeAdapter(SampleToValuesAdapter.class)
    private HashMap<String, ArrayList<Double>> sampleToValues;
    @XmlElement(name="priorValues")
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

    public Map<String, ArrayList<Double>> getSampleToValues() {
        return sampleToValues;
    }
    
    public Map<String, List<Double>> getResultsForSamples(Set<String> samples) {
        Map<String, List<Double>> sampleToResults = new HashMap<String, List<Double>>();
        if (sampleToValues != null) {
            for (String sample : sampleToValues.keySet()) {
                if (samples.contains(sample)) {
                    List<Double> probs = sampleToValues.get(sample);
                    sampleToResults.put(sample, probs);
                }
            }
        }
        return sampleToResults;
    }
    
    public void setSampleToValues(Map<String, ArrayList<Double>> sampleToValues) {
        if (sampleToValues instanceof HashMap)
            this.sampleToValues = (HashMap<String, ArrayList<Double>>)this.sampleToValues;
        else
            this.sampleToValues = new HashMap<String, ArrayList<Double>>(sampleToValues);
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
    
    public void addSampleToValue(String sample, ArrayList<Double> values) {
        if (sampleToValues == null)
            sampleToValues = new HashMap<String, ArrayList<Double>>();
        sampleToValues.put(sample, values);
    }
    
    public void addSampleToValue(String sample, double[] values) {
        List<Double> list = PlugInUtilities.convertArrayToList(values);
        if (list instanceof ArrayList)
            addSampleToValue(sample, (ArrayList<Double>)list);
        else
            addSampleToValue(sample, new ArrayList<Double>(list));
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
    
    @XmlAccessorType(XmlAccessType.FIELD)
    static class SampleToValues {
        private String sample;
        private List<Double> values;
        
        public SampleToValues() {
            
        }
    }
    
    @XmlAccessorType(XmlAccessType.FIELD)
    static class SampleToValuesList {
        @XmlElement
        private List<SampleToValues> sampleToValues;
        
        public SampleToValuesList() {
            sampleToValues = new ArrayList<VariableInferenceResults.SampleToValues>();
        }
    }
    
    static class SampleToValuesAdapter extends XmlAdapter<SampleToValuesList, HashMap<String, List<Double>>> {

        public SampleToValuesList marshal(HashMap<String, List<Double>> sampleToValues) throws Exception {
           SampleToValuesList rtn = new SampleToValuesList();
           for (String sample : sampleToValues.keySet()) {
               List<Double> values = sampleToValues.get(sample);
               SampleToValues tmp = new SampleToValues();
               tmp.sample = sample;
               tmp.values = values;
               rtn.sampleToValues.add(tmp);
           }
           return rtn;
        }
       
        public HashMap<String, List<Double>> unmarshal(SampleToValuesList sampleToValuesList) throws Exception {
           HashMap<String, List<Double>> rtn = new HashMap<String, List<Double>>();
           for (SampleToValues sampleToValues : sampleToValuesList.sampleToValues) {
               rtn.put(sampleToValues.sample,
                       sampleToValues.values);
           }
           return rtn;
        }
        
    }
    
}
