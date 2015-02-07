/*
 * Created on Mar 11, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.gk.util.ProgressPane;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Factor;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.common.DataType;
import org.reactome.factorgraph.common.ObservationFileLoader;
import org.reactome.factorgraph.common.ObservationFileLoader.ObservationData;
import org.reactome.factorgraph.common.ObservationRandomizer;
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
    protected int maxId;
    
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
    }
    
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
        // We will use a built-in ObservationFileLoader to perform data load
        ObservationFileLoader dataLoader = new ObservationFileLoader();
        dataLoader.setPGMConfiguration(PathwayPGMConfiguration.getConfig());
        List<ObservationData> observationData = loadData(dnaFile,
                                                         dnaThresholdValues,
                                                         geneExpFile,
                                                         geneExpThresholdValues,
                                                         progressPane,
                                                         dataLoader);
        List<Observation> observations = dataLoader.getObservations();
        if (observationData.size() == 0 || observations.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot load observation data. Inference cannot be performed.",
                                          "No Observation Data",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        // Need to re-validate FactorGraph since new factors have been added
        // during data loading
        fg.validatVariables();
        Map<String, String> sampleToType = null;
        if (sampleInfoFile != null) {
            progressPane.setText("Loading sample type info...");
            sampleToType = loadSampleToType(sampleInfoFile);
        }
        if (sampleToType != null && sampleToType.size() > 0) {
            boolean correct = filterObservationsForTypes(sampleToType, observations);
            if (!correct)
                return false;
        }
        FactorGraphRegistry.getRegistry().setObservations(fg, observations);
        // Even though we want to perform two-case analysis, we still need to generate
        // random samples for p-values and FDRs calculations regarding individual samples
        // and objects in the pathway. But it can be turned off by using a flag.
        if (sampleInfoFile == null) {
            if (progressPane != null)
                progressPane.setText("Generating random data...");
            ObservationRandomizer randomizer = new ObservationRandomizer();
            randomizer.setNumberOfPermutation(100);
            randomizer.setRandomSamplePrefix(RANDOM_SAMPLE_PREFIX);
            List<Observation> randomData = randomizer.randomize(observations,
                                                                observationData);
            FactorGraphRegistry.getRegistry().setRandomObservations(fg, randomData);
        }
        
        return true;
    }

    private List<ObservationData> loadData(File dnaFile,
                                           double[] dnaThresholdValues,
                                           File geneExpFile,
                                           double[] geneExpThresholdValues,
                                           ProgressPane progressPane,
                                           ObservationFileLoader dataLoader) throws IOException {
        List<ObservationData> observationData = null;
        // If both dnaFile and geneExpFile are null, we should use the cached data
        if (dnaFile == null && geneExpFile == null) {
            observationData = FactorGraphRegistry.getRegistry().getAllLoadedData();
        }
        else {
            // Check if data has been loaded already
            observationData = new ArrayList<ObservationFileLoader.ObservationData>();
            if (dnaFile != null) {
                if (progressPane != null)
                    progressPane.setText("Loading CNV data...");
                ObservationData data = FactorGraphRegistry.getRegistry().getLoadedData(dnaFile, dnaThresholdValues);
                if (data == null) {
                    Map<String, Map<String, Integer>> dnaSampleToGeneToState = dataLoader.loadObservationData(dnaFile.getAbsolutePath(),
                                                                                                              DataType.CNV,
                                                                                                              dnaThresholdValues);
                    data = new ObservationData();
                    data.setDataType(DataType.CNV);
                    data.setSampleToGeneToValue(dnaSampleToGeneToState);
                    FactorGraphRegistry.getRegistry().cacheLoadedData(dnaFile, dnaThresholdValues, data);
                }
                observationData.add(data);
            }
            if (geneExpFile != null) {
                progressPane.setText("Loading mRNA expression data...");
                ObservationData data = FactorGraphRegistry.getRegistry().getLoadedData(geneExpFile, geneExpThresholdValues);
                if (data == null) {
                    Map<String, Map<String, Integer>> geneExpSampleToGeneToState = dataLoader.loadObservationData(geneExpFile.getAbsolutePath(),
                                                                                                                  DataType.mRNA_EXP,
                                                                                                                  geneExpThresholdValues);
                    data = new ObservationData();
                    data.setDataType(DataType.mRNA_EXP);
                    data.setSampleToGeneToValue(geneExpSampleToGeneToState);
                    FactorGraphRegistry.getRegistry().cacheLoadedData(geneExpFile, geneExpThresholdValues, data);
                }
                observationData.add(data);
            }
        }
        Map<String, Variable> nameToVar = getNameToVarInFactorGraph();
        for (ObservationData data : observationData) {
            dataLoader.addObservation(data.getSampleToGeneToValue(),
                                      data.getDataType(),
                                      nameToVar,
                                      fg.getFactors());
        }
        return observationData;
    }

    private Map<String, Variable> getNameToVarInFactorGraph() {
        Map<String, Variable> nameToVar = new HashMap<String, Variable>();
        for (Variable var : fg.getVariables())
            nameToVar.put(var.getName(), var);
        return nameToVar;
    }

    private boolean filterObservationsForTypes(Map<String, String> sampleToType,
                                               List<Observation> observations) {
        if (sampleToType == null || sampleToType.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot find any samples in the sample information file.",
                                          "Wrong Sample Info File",
                                          JOptionPane.ERROR_MESSAGE);
            return false; // Nothing to be done.
        }
        // Filter observations and attach sample information.
        Map<String, Integer> typeToCount = new HashMap<String, Integer>();
        for (Iterator<Observation> it = observations.iterator(); it.hasNext();) {
            Observation observation = it.next();
            String type = sampleToType.get(observation.getName());
            if (type == null) {
                it.remove(); // If there is no type available for this Observation, just remove it.
            }
            else {
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
        //TODO cache the loaded sampleToType information
//        Map<String, String> sampleToType = FactorGraphRegistry.getRegistry().getLoadedSampleToType();
//        if (sampleToType != null)
//            return sampleToType;
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
    
    protected List<String> parseSamples(String line) {
        String[] tokens = line.split("\t");
        List<String> samples = new ArrayList<String>();
        for (int i = 1; i < tokens.length; i++)
            samples.add(tokens[i]);
        return samples;
    }

    protected List<Double> getFactorValues(DataType dataType) {
        PGMConfiguration config = PathwayPGMConfiguration.getConfig();
        Map<DataType, double[]> typeToFactorValue = config.getTypeToFactorValues();
        double[] values = typeToFactorValue.get(dataType);
        return PlugInUtilities.convertArrayToList(values);
    }
    
}
