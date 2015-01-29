/*
 * Created on Mar 11, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;
import org.cytoscape.model.CyNode;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Factor;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.common.DataType;
import org.reactome.factorgraph.common.PGMConfiguration;
import org.reactome.pathway.factorgraph.PathwayPGMConfiguration;
import org.reactome.r3.util.FileUtility;


/**
 * This class is used to process observation data for a displayed FactorGraph object.
 * An object of this class should not be cached for multiple data loading since loaded
 * data is cached for one loading in order to keep the performance.
 * @author gwu
 *
 */
public class ObservationDataHelper {
    // Hope this is a unique random sample prefix
    public static final String RANDOM_SAMPLE_PREFIX = "org.reactome.fi.random_";
    protected FactorGraph fg;
    // For quick find variables
    protected Map<String, Variable> nameToVar;
    // In order to assign ids to new variable
    private int maxId;
    // Kept loaded data for randomization: data has been discretized already
    private GeneSampleDataPoints data;
    
    /**
     * Default constructor is used only for subclassing.
     */
    protected ObservationDataHelper() {
    }
    
    public ObservationDataHelper(FactorGraph fg) {
        if (fg == null)
            throw new IllegalArgumentException("Factor graph cannot be null!");
        this.fg = fg;
        initializeProperties();
    }

    protected void initializeProperties() {
        nameToVar = new HashMap<String, Variable>();
        for (Variable var : fg.getVariables()) {
            if (var.getName() == null)
                continue; // This should not occur
            nameToVar.put(var.getName(), var);
        }
        // Get the maximum ids, which should be long, in order to assign to new variables
        maxId = Integer.MIN_VALUE;
        for (Variable var : fg.getVariables()) {
            if (var.getId().matches("(\\d+)")) { // Make sure used id is an integer
                Integer id = new Integer(var.getId());
                if (id > maxId)
                    maxId = id;
            }
        }
        for (Factor factor : fg.getFactors()) {
            if (factor.getId().matches("(\\d+)")) {
                Integer id = new Integer(factor.getId());
                if (id > maxId)
                    maxId = id;
            }
        }
        data = new GeneSampleDataPoints();
    }
    
    @SuppressWarnings("unchecked")
    public boolean performLoadData(File dnaFile,
                                   double[] dnaThresholdValues,
                                   File geneExpFile,
                                   double[] geneExpThresholdValues,
                                   File sampleInfoFile, // If this file is not null, two-cases analysis should be performed
                                   ProgressPane progressPane) throws Exception {
        if (progressPane != null) {
            progressPane.setTitle("Load Observation Data");
            progressPane.setIndeterminate(true);
        }
        Map<Variable, Map<String, Integer>> dnaVarToSampleToState = null;
        if (dnaFile != null) {
            if (progressPane != null)
                progressPane.setText("Loading CNV data...");
            dnaVarToSampleToState = loadData(dnaFile, 
                                             DataType.CNV,
                                             dnaThresholdValues);
        }
        Map<Variable, Map<String, Integer>> geneExpVarToSampleToState = null;
        if (geneExpFile != null) {
            progressPane.setText("Loading mRNA expression data...");
            geneExpVarToSampleToState = loadData(geneExpFile,
                                                 DataType.mRNA_EXP,
                                                 geneExpThresholdValues);
        }
        if (dnaVarToSampleToState == null && geneExpVarToSampleToState == null) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot load observation data. Inference cannot be performed.",
                                          "No Observation Data",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        Map<String, String> sampleToType = null;
        if (sampleInfoFile != null) {
            progressPane.setText("Loading sample type info...");
            sampleToType = loadSampleToType(sampleInfoFile);
        }
        if (progressPane != null)
            progressPane.setText("Generating observations...");
        List<Observation> observations = null;
        if (dnaVarToSampleToState != null && geneExpVarToSampleToState != null)
            observations = generateObservations(dnaVarToSampleToState,
                                                geneExpVarToSampleToState);
        else if (dnaVarToSampleToState != null)
            observations = generateObservations(dnaVarToSampleToState);
        else if (geneExpVarToSampleToState != null)
            observations = generateObservations(geneExpVarToSampleToState);
        if (observations.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "No observation can be created. Inference cannot be performed.",
                                          "Empty Observation",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (sampleToType != null && sampleToType.size() > 0) {
            boolean correct = attachTypesToObservations(sampleToType, observations);
            if (!correct)
                return false;
        }
        FactorGraphRegistry.getRegistry().setObservations(fg, observations);
        // Even though we want to perform two-case analysis, we still need to generate
        // random samples for p-values and FDRs calculations regrading individual samples
        // and objects in the pathway.
        if (progressPane != null)
            progressPane.setText("Generating random data...");
        List<Observation> randomData = null;
        if (dnaVarToSampleToState != null && geneExpVarToSampleToState != null)
            randomData = generateRandomObservations(dnaVarToSampleToState,
                                                    geneExpVarToSampleToState);
        else if (dnaVarToSampleToState != null)
            randomData = generateRandomObservations(dnaVarToSampleToState);
        else if (geneExpVarToSampleToState != null)
            randomData = generateRandomObservations(geneExpVarToSampleToState);
        FactorGraphRegistry.getRegistry().setRandomObservations(fg, randomData);
        return true;
    }

    private boolean attachTypesToObservations(Map<String, String> sampleToType,
                                           List<Observation> observations) {
        if (sampleToType == null || sampleToType.size() == 0)
            return true; // Nothing to be done.
        // Attach sample type information to observation as annotation
        Map<String, Integer> typeToCount = new HashMap<String, Integer>();
        for (Observation observation : observations) {
            String type = sampleToType.get(observation.getName());
            if (type != null) {
                observation.setAnnoation(type);
                Integer count = typeToCount.get(type);
                if (count == null)
                    typeToCount.put(type, 1);
                else
                    typeToCount.put(type, ++count);
            }
        }
        // Two types only
        if (typeToCount.size() != 2) { // This should not occure
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Only two sample types are needed for two cases analysis. Your data has " + typeToCount.size() + " type(s).",
                                          "Wrong Number Of Sample Types",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // Check type counts. At least 3 samples are needed.
        StringBuilder builder = new StringBuilder();
        for (String type : typeToCount.keySet()) {
            Integer count = typeToCount.get(type);
            if (count <= 3) {
                if (builder.length() > 0)
                    builder.append("; ");
                builder.append(type + ": " + count);
            }
        }
        if (builder.length() > 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "At least 3 samples are needed for each sample type. Not enough sample: \n" +
                                           builder.toString(),
                                          "Not Enough Sample",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    private Map<String, String> loadSampleToType(File sampleFile) throws IOException {
        FileUtility fu = new FileUtility();
        Map<String, String> sampleToType = new HashMap<String, String>();
        fu.setInput(sampleFile.getAbsolutePath());
        String line = null;
        while ((line = fu.readLine()) != null) {
            String[] tokens = line.split("\t");
            sampleToType.put(tokens[0], tokens[1]);
        }
        fu.close();
        return sampleToType;
    }
    
    private Map<Variable, Map<String, Integer>> loadData(File file,
                                                         DataType type,
                                                         double[] thresholdValues) throws Exception {
        if (type == DataType.CNV) {
            return loadData(file, 
                            "DNA", 
                            getFactorValues(type),
                            thresholdValues);
        }
        else if (type == DataType.mRNA_EXP)
            return loadData(file, 
                            "mRNA",
                            getFactorValues(type),
                            thresholdValues);
        return null;
    }
    
    /**
     * Generate a list of Observations from a set of varToSampleToStates.
     * @param varToSampleToStates
     * @return
     */
    public List<Observation> generateObservations(Map<Variable, Map<String, Integer>>... varToSampleToStates) {
        // Get all samples mentioned in the parameters.
        Set<String> samples = new HashSet<String>();
        for (Map<Variable, Map<String, Integer>> varToSampleToState : varToSampleToStates) {
            for (Variable var : varToSampleToState.keySet()) {
                samples.addAll(varToSampleToState.get(var).keySet());
            }
        }
        List<Observation> observations = new ArrayList<Observation>();
        for (String sample : samples) {
            Observation observation = new Observation();
            observation.setName(sample);
            for (Map<Variable, Map<String, Integer>> varToSampleToState : varToSampleToStates) {
                for (Variable var : varToSampleToState.keySet()) {
                    Map<String, Integer> sampleToState = varToSampleToState.get(var);
                    if (sampleToState.containsKey(sample)) {
                        observation.addAssignment(var,
                                                  sampleToState.get(sample));
                    }
                }
            }
            observations.add(observation);
        }
        return observations;
    }
    
    /**
     * Generate a random observation data set for a passed list of variables in the map.
     * @param varToSampleToStates
     * @return
     */
    public List<Observation> generateRandomObservations(Map<Variable, Map<String, Integer>>... varToSampleToStates) {
        Set<String> genes = new HashSet<String>();
        for (Map<Variable, Map<String, Integer>> varToSampleToState : varToSampleToStates) {
            for (Variable var : varToSampleToState.keySet()) {
                String label = var.getName();
                String[] tokens = label.split("_");
                genes.add(tokens[0]);
            }
        }
        List<Observation> rtn = new ArrayList<Observation>();
        GeneSampleDataPoints randomData = data.generateRandomData(genes, 1000);
        List<String> samples = randomData.getAllSamples();
        for (String sample : samples) {
            Observation observation = new Observation();
            rtn.add(observation);
            observation.setName(sample);
            for (Map<Variable, Map<String, Integer>> varToSampleToState : varToSampleToStates) {
                for (Variable var : varToSampleToState.keySet()) {
                    String label  = var.getName();
                    String[] tokens = label.split("_");
                    String gene = tokens[0];
                    Integer state = randomData.getState(gene, sample, tokens[1]);
                    if (state == null)
                        continue;
                    observation.addAssignment(var, state);
                }
            }
        }
        return rtn;
    }
    
    protected List<String> parseSamples(String line) {
        String[] tokens = line.split("\t");
        List<String> samples = new ArrayList<String>();
        for (int i = 1; i < tokens.length; i++)
            samples.add(tokens[i]);
        return samples;
    }

    protected Map<Variable, Map<String, Integer>> loadData(File file, 
                                                           String nodeType, 
                                                           List<Double> factorValues,
                                                           double[] thresholdValues) throws IOException {
        FileUtility fu = new FileUtility();
        fu.setInput(file.getAbsolutePath());
        // First line should be header
        String line = fu.readLine();
        List<String> samples = parseSamples(line);
        int index = 0;
        // Keep these mappings for layout after an updateView.
        // Otherwise, a null exception will be thrown because there
        // is no view for newly added CyNode
        final Map<CyNode, CyNode> varNodeToFactorNode = new HashMap<CyNode, CyNode>();
        final Map<CyNode, CyNode> factorNodeToObsNode = new HashMap<CyNode, CyNode>();
        Map<Variable, Map<String, Integer>> variableToSampleToState = new HashMap<Variable, Map<String,Integer>>();
        while ((line = fu.readLine()) != null) {
            // Cache data for randomization purpose
            parseData(line, 
                      samples,
                      nodeType, 
                      thresholdValues);
            index = line.indexOf("\t");
            String gene = line.substring(0, index);
            String varName = gene + "_" + nodeType;
            // Check if a Variable node exists
            Variable var = nameToVar.get(varName);
            if (var == null)
                continue; // Nothing to be done
            // Just use the first DB_ID as its label
            Variable obsVar = createObsVariable(gene, 
                                                nodeType);
            Factor factor = createObsFactor(obsVar, 
                                            var, 
                                            factorValues);
            Map<String, Integer> sampleToState = data.getSampleToState(gene,
                                                                       nodeType);
            variableToSampleToState.put(obsVar, sampleToState);
        }
        fu.close();
        fg.validatVariables();
        return variableToSampleToState;
    }
    
    protected void parseData(String line,
                             List<String> samples,
                             String nodeType,
                             double[] thresholdValues) {
        String[] tokens = line.split("\t");
        String gene = tokens[0];
        double value = 0.0d;
        for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].length() == 0 || tokens[i].toLowerCase().equals("na"))
                continue;
            String sample = samples.get(i - 1);
            value = Double.parseDouble(tokens[i]);
            int state = discretize(value, thresholdValues);
            data.addGeneSampleDataPoint(gene, 
                                        sample,
                                        nodeType,
                                        (byte)state);
        }
    }
    
    private int discretize(double value,
                           double[] thresholdValues) {
        // A simple discretizing method
        if (value >= thresholdValues[thresholdValues.length - 1]) {
            return thresholdValues.length;
        }
        for (int j = 0; j < thresholdValues.length; j++) {
            if (value < thresholdValues[j]) {
                return j;
            }
        }
        return 0;
    }
    
    protected Factor createObsFactor(Variable obsVar,
                                   Variable hiddenVar,
                                   List<Double> factorValues) {
        Factor factor = new Factor();
        factor.setId(++maxId + "");
        List<Variable> varList = new ArrayList<Variable>();
        varList.add(obsVar);
        varList.add(hiddenVar);
        factor.setVariables(varList);
        factor.setValues(factorValues);
        String factorName = hiddenVar.getName() + "->" + obsVar.getName();
        factor.setName(factorName);
        fg.addFactor(factor);
        return factor;
    }
    
    protected Variable createObsVariable(String gene,
                                       String type) {
        Variable obsVar = new Variable(PathwayPGMConfiguration.getConfig().getNumberOfStates());
        obsVar.setId(++maxId + "");
        String label = gene + "_" + type + "_obs";
        obsVar.setName(label);
        return obsVar;
    }
    
    private List<Double> getFactorValues(DataType dataType) {
        PGMConfiguration config = PathwayPGMConfiguration.getConfig();
        Map<DataType, double[]> typeToFactorValue = config.getTypeToFactorValues();
        double[] values = typeToFactorValue.get(dataType);
        return PlugInUtilities.convertArrayToList(values);
    }
    
    protected Map<String, Integer> getSampleToState(String gene, String nodeType) {
        return data.getSampleToState(gene, nodeType);
    }
    
    /**
     * The following three simple classes are used to hold loaded gene to sample to states to avoid
     * to use a complicated parameterized map.
     * @author gwu
     *
     */
    private class GeneSampleDataPoints {
        private Map<String, SampleDataPoints> geneToSampleDataPoints;
        
        public GeneSampleDataPoints() {
        }
        
        public Map<String, Integer> getSampleToState(String gene, 
                                                     String type) {
            Map<String, Integer> sampleToState = new HashMap<String, Integer>();
            if (geneToSampleDataPoints != null) {
                SampleDataPoints sampleDataPoints = geneToSampleDataPoints.get(gene);
                if (sampleDataPoints != null && sampleDataPoints.sampleToDataPoints != null) {
                    for (String sample : sampleDataPoints.sampleToDataPoints.keySet()) {
                        DataPoints dataPoints = sampleDataPoints.sampleToDataPoints.get(sample);
                        if (dataPoints == null || dataPoints.getState(type) == null)
                            continue;
                        sampleToState.put(sample, (int)dataPoints.getState(type));
                    }
                }
            }
            return sampleToState;
        }
        
        public Integer getState(String gene,
                                String sample,
                                String type) {
            SampleDataPoints sampleDataPoints = geneToSampleDataPoints.get(gene);
            if (sampleDataPoints == null)
                return null;
            DataPoints dataPoints = sampleDataPoints.sampleToDataPoints.get(sample);
            if (dataPoints == null)
                return null;
            Byte state = dataPoints.getState(type);
            if (state == null)
                return null;
            return new Integer(state);
        }
        
        /**
         * Do a random sampling. The implementation of this method doesn't keep the integrity of data in a
         * sample. In other words, the data in a random sample is mixed from multiple samples.
         * @return
         */
        public GeneSampleDataPoints generateRandomData(Collection<String> checkGenes,
                                                       int sampleCount) {
            GeneSampleDataPoints rtn = new GeneSampleDataPoints();
            List<String> geneList = new ArrayList<String>(geneToSampleDataPoints.keySet());
            List<String> sampleList = getAllSamples();
            RandomData randomizer = new RandomDataImpl();
            Map<String, SampleDataPoints> randomGeneMap = new HashMap<String, ObservationDataHelper.SampleDataPoints>();
            rtn.geneToSampleDataPoints = randomGeneMap;
            int randomGeneIndex = 0;
            int randomSampleIndex = 0;
            for (String gene : checkGenes) {
                SampleDataPoints randomSampleDataPoints = new SampleDataPoints();
                Map<String, DataPoints> randomSampleMap = new HashMap<String, ObservationDataHelper.DataPoints>();
                randomSampleDataPoints.sampleToDataPoints = randomSampleMap;
                randomGeneMap.put(gene, randomSampleDataPoints);
                for (int i = 0; i < sampleCount; i++) {
                    String sample = RANDOM_SAMPLE_PREFIX + i;
                    // Pick a random sample
                    randomSampleIndex = randomizer.nextInt(0, sampleList.size() - 1);
                    String sample1 = sampleList.get(randomSampleIndex);
                    // Pick a random gene
                    randomGeneIndex = randomizer.nextInt(0, geneList.size() - 1);
                    String gene1 = geneList.get(randomGeneIndex);
                    // Based on random gene and sample, pick up a DataPoints object, which should be random.
                    SampleDataPoints sampleDataPoint1 = geneToSampleDataPoints.get(gene1);
                    DataPoints dataPoint1 = sampleDataPoint1.sampleToDataPoints.get(sample1);
                    randomSampleMap.put(sample, dataPoint1);
                }
            }
            return rtn;
        }

        private List<String> getAllSamples() {
            // Get all samples
            Set<String> samples = new HashSet<String>();
            for (String gene : geneToSampleDataPoints.keySet()) {
                SampleDataPoints sampleDataPoints = geneToSampleDataPoints.get(gene);
                samples.addAll(sampleDataPoints.sampleToDataPoints.keySet());
            }
            List<String> sampleList = new ArrayList<String>(samples);
            return sampleList;
        }
        
        public void addGeneSampleDataPoint(String gene,
                                           String sample,
                                           String type,
                                           Byte state) {
            if (geneToSampleDataPoints == null)
                geneToSampleDataPoints = new HashMap<String, ObservationDataHelper.SampleDataPoints>();
            SampleDataPoints sampleDataPoints = geneToSampleDataPoints.get(gene);
            if (sampleDataPoints == null) {
                sampleDataPoints = new SampleDataPoints();
                geneToSampleDataPoints.put(gene, sampleDataPoints);
            }
            sampleDataPoints.addSampleDataPoint(sample, type, state);
        }
    }
    
    private class SampleDataPoints {
        private Map<String, DataPoints> sampleToDataPoints;
        
        public SampleDataPoints() {
        }
        
        public void addSampleDataPoint(String sample,
                                       String type,
                                       Byte state) {
            if (sampleToDataPoints == null)
                sampleToDataPoints = new HashMap<String, ObservationDataHelper.DataPoints>();
            DataPoints dataPoints = sampleToDataPoints.get(sample);
            if (dataPoints == null) {
                dataPoints = new DataPoints();
                sampleToDataPoints.put(sample, dataPoints);
            }
            dataPoints.addTypeToValue(type, state);
        }
    }
    
    /**
     * A simple data structure to store a gene based data.
     * @author gwu
     *
     */
    private class DataPoints {
        // Type should be DNA or mRNA etc.
        private Map<String, Byte> typeToState;
        
        public DataPoints() {
        }
        
        public void addTypeToValue(String type, Byte state) {
            if (typeToState == null)
                typeToState = new HashMap<String, Byte>();
            typeToState.put(type, state);
        }
        
        public Byte getState(String type) {
            if (typeToState == null)
                return null;
            return typeToState.get(type);
        }
    }
    
}
