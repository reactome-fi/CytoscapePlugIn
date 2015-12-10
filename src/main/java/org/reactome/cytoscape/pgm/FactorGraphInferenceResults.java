/*
 * Created on Jan 13, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Test;
import org.reactome.factorgraph.ContinuousVariable;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.common.DataType;
import org.reactome.pathway.factorgraph.IPACalculator;

/**
 * Inference results for a FactorGraph object for the whole data set.
 * @author gwu
 *
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FactorGraphInferenceResults {
    private FactorGraph factorGraph;
    // Use list instead of map for XML serialization.
    private List<VariableInferenceResults> varResults;
    // A flag indicating this result should be processed based on two cases
    private boolean usedForTwoCases;
    // Map samples to types
    private Map<String, String> sampleToType;
    // Add a label for this result
    private Long pathwayDiagramId;
    // Observations used
    @XmlElement(name="observation")
    private List<Observation<Number>> observations;
    @XmlElement(name="randomObservation")
    private List<Observation<Number>> randomObservations;
    
    /**
     * Default constructor.
     */
    public FactorGraphInferenceResults() {
        varResults = new ArrayList<VariableInferenceResults>();
    }

    public List<Observation<Number>> getObservations() {
        return observations;
    }

    public void setObservations(List<Observation<Number>> observations) {
        this.observations = observations;
    }

    public List<Observation<Number>> getRandomObservations() {
        return randomObservations;
    }

    public void setRandomObservations(List<Observation<Number>> randomObservations) {
        this.randomObservations = randomObservations;
    }

    public Long getPathwayDiagramId() {
        return pathwayDiagramId;
    }

    public void setPathwayDiagramId(Long pathwayDiagramId) {
        this.pathwayDiagramId = pathwayDiagramId;
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
        Map<Variable, VariableInferenceResults> varToResults = new HashMap<Variable, VariableInferenceResults>();
        for (VariableInferenceResults varResult : varResults)
            varToResults.put(varResult.getVariable(), varResult);
        return varToResults;
    }
    
    public VariableInferenceResults getVariableInferenceResults(Variable var) {
        for (VariableInferenceResults varResult : varResults) {
            if (varResult.getVariable() == var)
                return varResult;
        }
        return null;
    }
    
    /**
     * Get inference results for a collection variables.
     * @param variables
     * @return
     */
    public List<VariableInferenceResults> getVariableInferenceResults(Collection<Variable> variables) {
        List<VariableInferenceResults> rtn = new ArrayList<VariableInferenceResults>();
        for (VariableInferenceResults varResult : varResults) {
            if (variables.contains(varResult.getVariable()))
                rtn.add(varResult);
        }
        return rtn;
    }

    public void setVarToResults(Map<Variable, VariableInferenceResults> varToResults) {
        varResults.clear();
        if (varToResults != null) {
            for (VariableInferenceResults varResult : varToResults.values())
                varResults.add(varResult);
        }
    }
    
    public void addVarToResults(VariableInferenceResults varResult) {
        varResults.add(varResult);
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
        if (varResults != null)
            varResults.setPriorValues(var.getBelief());
    }
    
    private void storeInferenceResults(String sample, Variable var) {
        VariableInferenceResults varResults = getVarResults(var);
        if (varResults != null)
            varResults.addSampleToValue(sample, var.getBelief());
    }
    
    /**
     * A helper method to get a result for a specified variable. If no result exists,
     * an empty object will be created.
     * @param var
     * @return
     */
    private VariableInferenceResults getVarResults(Variable var) {
        // We cannot perform inference for a ContinuousVariable
        if (var instanceof ContinuousVariable)
            return null;
        VariableInferenceResults varResult = getVariableInferenceResults(var);
        if (varResult == null) {
            varResult = new VariableInferenceResults();
            varResult.setVariable(var);
            varResults.add(varResult);
        }
        return varResult;
    }
    
    /**
     * Get the set of samples touched by this object.
     * @return
     */
    public Set<String> getSamples() {
        Set<String> samples = new HashSet<String>();
        for (VariableInferenceResults varResults : varResults) {
            samples.addAll(varResults.getPosteriorValues().keySet());
        }
        return samples;
    }
    
    public boolean hasPosteriorResults() {
        if (varResults == null || varResults.size() == 0)
            return false;
        VariableInferenceResults varResult = varResults.get(0);
        if (varResult.getPosteriorValues() == null || varResult.getPosteriorValues().size() == 0)
            return false;
        return true;
    }
    
    public void clear() {
        varResults.clear();
    }
    
    /**
     * A kind of utility method.
     * @param varResults
     * @return
     */
    public Map<Variable, List<Double>> generateRandomIPAs(List<VariableInferenceResults> varResults) {
        Map<Variable, List<Double>> varToRandomIPAs = new HashMap<Variable, List<Double>>();
        for (VariableInferenceResults varResult : varResults) {
            List<Double> ipas = new ArrayList<Double>();
            varToRandomIPAs.put(varResult.getVariable(),
                                ipas);
            Map<String, List<Double>> randomPosts = varResult.getRandomPosteriorValues();
            for (String sample : randomPosts.keySet()) {
                double ipa = IPACalculator.calculateIPA(varResult.getPriorValues(),
                                                        randomPosts.get(sample));
                ipas.add(ipa);
            }
            Collections.sort(ipas);
        }
        return varToRandomIPAs;
    }
    
    @Test
    public void testStoredResults() throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(FactorGraphInferenceResults.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        File file = new File("/Users/gwu/Documents/EclipseWorkspace/caBigR3/results/paradigm/twoCases/hnsc/CytoscapeAppResults/M_G1Transition_MAX_BRCA.xml");
        FactorGraphInferenceResults results = (FactorGraphInferenceResults) unmarshaller.unmarshal(file);
        Map<Variable, VariableInferenceResults> varToResults = results.getVarToResults();
        for (Variable var : varToResults.keySet()) {
            if (var.getName().endsWith("_" + DataType.mRNA_EXP)) {
                System.out.println(var.getName());
                Map<String, List<Double>> sampleToResults = varToResults.get(var).getPosteriorValues();
                for (String sample : sampleToResults.keySet()) {
                    List<Double> values = sampleToResults.get(sample);
                    System.out.println(sample + "\t" + values);
                }
                break;
            }
        }
    }
    
}
