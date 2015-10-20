/*
 * Created on Oct 8, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;

/**
 * This singleton class is used to store observations and inference results.
 * This method is similar to these three classes in package org.reactome.cytoscape.pgm:
 * FactorGraphRegistry, FactorGraphInferenceResults and VariableInferenceResults. Most 
 * likely, this class should be merged into those three classes.
 * @author gwu
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class FIPGMResults {
    @XmlTransient
    private static FIPGMResults results;
    // Stored information
    @XmlTransient
    private List<Observation<Number>> observations;
    @XmlTransient
    private List<Observation<Number>> randomObservations;
    private List<SampleToVarToScore> sampleToVarToScore;
    private List<SampleToVarToScore> randomSampleToVarToScore;
    // Just for saving purposes
    private List<Variable> variables;
    
    /**
     * Default constructor should not be called.
     */
    private FIPGMResults() {
    }
    
    public static FIPGMResults getResults() {
        if (results == null)
            results = new FIPGMResults();
        return results;
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

    public Map<String, Map<Variable, Double>> getSampleToVarToScore() {
        return convertListToMap(sampleToVarToScore);
    }
    
    /**
     * Get results for a subset of variables.
     * @param variables
     * @return
     */
    public Map<String, Map<Variable, Double>> getSampleToVarToScore(Collection<Variable> variables) {
        Map<String, Map<Variable, Double>> sampleToVarToScore = convertListToMap(this.sampleToVarToScore);
        return _getSampleToVarToScore(sampleToVarToScore, variables);
    }
    
    public Map<String, Map<Variable, Double>> getRandomSampleToVarToScore(Collection<Variable> variables) {
        Map<String, Map<Variable, Double>> randomSampleToVarToScore = convertListToMap(this.randomSampleToVarToScore);
        return _getSampleToVarToScore(randomSampleToVarToScore, variables);
    }
    
    private Map<String, Map<Variable, Double>> _getSampleToVarToScore(Map<String, Map<Variable, Double>> sampleToVarToScore,
                                                                      Collection<Variable> variables) {
        Map<String, Map<Variable, Double>> rtn = new HashMap<>();
        for (String sample : sampleToVarToScore.keySet()) {
            Map<Variable, Double> varToScore = sampleToVarToScore.get(sample);
            Map<Variable, Double> varToScore1 = new HashMap<>();
            for (Variable var : variables) {
                Double score = varToScore.get(var);
                if (score == null)
                    continue;
                varToScore1.put(var, score);
            }
            if (varToScore1.size() > 0)
                rtn.put(sample, varToScore1);
        }
        return rtn;
    }

    public void setSampleToVarToScore(Map<String, Map<Variable, Double>> sampleToVarToScore) {
        this.sampleToVarToScore = convertMapToList(sampleToVarToScore);
    }

    public Map<String, Map<Variable, Double>> getRandomSampleToVarToScore() {
        return convertListToMap(randomSampleToVarToScore);
    }

    public void setRandomSampleToVarToScore(Map<String, Map<Variable, Double>> randomSampleToVarToScore) {
        this.randomSampleToVarToScore = convertMapToList(randomSampleToVarToScore);
    }
    
    /**
     * For JAXB purpose.
     * @return
     */
    private List<SampleToVarToScore> convertMapToList(Map<String, Map<Variable, Double>> sampleToVarToScore) {
        if (sampleToVarToScore == null || sampleToVarToScore.size() == 0)
            return null;
        List<SampleToVarToScore> rtn = new ArrayList<>();
        for (String sample : sampleToVarToScore.keySet()) {
            SampleToVarToScore sampleToVarToScores = new SampleToVarToScore();
            sampleToVarToScores.sample = sample;
            rtn.add(sampleToVarToScores);
            Map<Variable, Double> varToScore = sampleToVarToScore.get(sample);
            List<VarToScore> list = new ArrayList<>();
            sampleToVarToScores.varToScoreList = list;
            for (Variable var : varToScore.keySet()) {
                Double score = varToScore.get(var);
                VarToScore varToScoreObj = new VarToScore();
                varToScoreObj.var = var;
                varToScoreObj.score = score;
                list.add(varToScoreObj);
            }
        }
        return rtn;
    }
    
    private Map<String, Map<Variable, Double>> convertListToMap(List<SampleToVarToScore> sampleToVarToScores) {
        if (sampleToVarToScores == null || sampleToVarToScores.size() == 0)
            return null;
        Map<String, Map<Variable, Double>> rtn = new HashMap<>();
        for (SampleToVarToScore sampleToVarToScore : sampleToVarToScores) {
            Map<Variable, Double> varToScore = new HashMap<>();
            for (VarToScore varToScore1 : sampleToVarToScore.varToScoreList) {
                varToScore.put(varToScore1.var,
                               varToScore1.score);
            }
            rtn.put(sampleToVarToScore.sample,
                    varToScore);
        }
        return rtn;
    }
    
    /**
     * Save this analysis results.
     * @param fileName
     * @throws Exception
     */
    public void saveResults(File file) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(FIPGMResults.class);
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        // Need to fix this problem first so that variables can be handled in a central place
        this.variables = getAllVariables();
        jaxbMarshaller.marshal(this, file);
    }
    
    private List<Variable> getAllVariables() {
        Set<Variable> variables = new HashSet<>();
        SampleToVarToScore sampleToVarToScore1 = sampleToVarToScore.get(0);
        for (VarToScore varToScore : sampleToVarToScore1.varToScoreList)
            variables.add(varToScore.var);
        return new ArrayList<Variable>(variables);
    }
    
    /**
     * Load the analysis results saved in a file.
     * @param fileName
     * @throws Exception
     */
    public void loadResults(File file) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(FIPGMResults.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        FIPGMResults loadedResults = (FIPGMResults) unmarshaller.unmarshal(file);
        // Assign this loadedResults to the singleton.
        results = loadedResults;
    }
    
    @XmlAccessorType(XmlAccessType.FIELD)
    static class VarToScore {
        @XmlIDREF
        private Variable var;
        private Double score;
        
        public VarToScore() {
        }
    }
    
    @XmlAccessorType(XmlAccessType.FIELD)
    static class SampleToVarToScore {
        private String sample;
        private List<VarToScore> varToScoreList;
        
        public SampleToVarToScore() {
        }
    }
    
}
