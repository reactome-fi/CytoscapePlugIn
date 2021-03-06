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

import javax.swing.SwingUtilities;

import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.Factor;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.common.DataType;
import org.reactome.r3.util.FileUtility;


/**
 * A customized ObservationDataHelper used if a FactorGraph is displayed in the Cytoscape desktop.
 * @author gwu
 *
 */
public class NetworkObservationDataHelper extends ObservationDataHelper {
    private CyNetworkView networkView;
    // For quick find variables
    private Map<String, CyNode> nameToNode;
    // Kept loaded data for randomization: data has been discretized already
    private GeneSampleDataPoints data;
    
    /**
     * Default constructor.
     */
    public NetworkObservationDataHelper(FactorGraph fg,
                                        CyNetworkView netView) {
        if (fg == null || netView == null)
            throw new IllegalArgumentException("Factor graph and network view cannot be null!");
        this.fg = fg;
        this.networkView = netView;
        initializeProperties();
    }
    
    @Override
    protected void initializeProperties() {
        super.initializeProperties();
        nameToNode = new HashMap<String, CyNode>();
        TableHelper tableHelper = new TableHelper();
        for (CyNode node : networkView.getModel().getNodeList()) {
            String label = tableHelper.getStoredNodeAttribute(networkView.getModel(), node, "nodeLabel", String.class);
            nameToNode.put(label, node);
        }
        data = new GeneSampleDataPoints();
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
        // A helper object to create Nodes and Edges
        final FINetworkGenerator fiHelper = new FINetworkGenerator();
        CyNetwork network = networkView.getModel();
        CyTable nodeTable = network.getDefaultNodeTable();
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
            // Add this observation variable into the network.
            CyNode obsNode = fiHelper.createNode(network,
                                                 obsVar.getId(),
                                                 obsVar.getName(), 
                                                 "observation", 
                                                 obsVar.getName());
            Factor factor = createObsFactor(obsVar, 
                                            var, 
                                            factorValues);
            CyNode factorNode = fiHelper.createNode(network,
                                                    factor.getId(),
                                                    factor.getName(), 
                                                    "factor",
                                                    factor.getName());
            // Don't want to show label for factor node. So
            // a simple fix
            nodeTable.getRow(factorNode.getSUID()).set("nodeLabel", null);
            CyEdge edge = fiHelper.createEdge(network,
                                              obsNode,
                                              factorNode,
                    "FI");
            CyNode varNode = nameToNode.get(varName);
            edge = fiHelper.createEdge(network, 
                                       varNode, 
                                       factorNode,
                    "FI");
            varNodeToFactorNode.put(varNode, factorNode);
            factorNodeToObsNode.put(factorNode, obsNode);
            Map<String, Integer> sampleToState = getSampleToState(gene,
                                                                  nodeType);
            variableToSampleToState.put(obsVar, sampleToState);
        }
        fu.close();
        fg.validatVariables();
        networkView.updateView();
        // Use a swing thread so that updateView can be done first in order to get
        // node view with coordinates. Otherwise, a null exception is going to be thrown.
        // Now do a layout
        // The order is important
        SwingUtilities.invokeLater(new Thread() {
            public void run() {
                layout(varNodeToFactorNode, fiHelper);
                layout(factorNodeToObsNode, fiHelper);        
                // Need to recall visual style in order to make newly added nodes to have
                // correct visual styles.
                FIVisualStyle visStyler = new FactorGraphVisualStyle();
                visStyler.setVisualStyle(networkView);
            }
        });
        return variableToSampleToState;
    }
    
    private void layout(Map<CyNode, CyNode> anchorToPartner,
                        FINetworkGenerator fiHelper) {
        Set<CyNode> parnterNodes = new HashSet<CyNode>();
        for (CyNode anchor : anchorToPartner.keySet()) {
            CyNode partner = anchorToPartner.get(anchor);
            parnterNodes.clear();
            parnterNodes.add(partner);
            fiHelper.jiggleLayout(anchor, parnterNodes, networkView);
        }
    } 
    
    private Map<Variable, Map<String, Integer>> loadData(File file, DataType type, double[] thresholdValues)
            throws Exception {
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
    
    protected void parseData(String line, List<String> samples, String nodeType,
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
    
    private int discretize(double value, double[] thresholdValues) {
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
    
    protected Factor createObsFactor(Variable obsVar, Variable hiddenVar, List<Double> factorValues) {
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
    
    protected Variable createObsVariable(String gene, String type) {
        Variable obsVar = new Variable(PlugInObjectManager.getManager().getPathwayPGMConfig().getNumberOfStates());
        obsVar.setId(++maxId + "");
        String label = gene + "_" + type + "_obs";
        obsVar.setName(label);
        return obsVar;
    }
    
    /**
     * Generate a random observation data set for a passed list of variables in the map.
     * @param varToSampleToStates
     * @return
     */
    private List<Observation> generateRandomObservations(Map<Variable, Map<String, Integer>>... varToSampleToStates) {
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
            Map<String, SampleDataPoints> randomGeneMap = new HashMap<String, SampleDataPoints>();
            rtn.geneToSampleDataPoints = randomGeneMap;
            int randomGeneIndex = 0;
            int randomSampleIndex = 0;
            for (String gene : checkGenes) {
                SampleDataPoints randomSampleDataPoints = new SampleDataPoints();
                Map<String, DataPoints> randomSampleMap = new HashMap<String, DataPoints>();
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
                geneToSampleDataPoints = new HashMap<String, SampleDataPoints>();
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
                sampleToDataPoints = new HashMap<String, DataPoints>();
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
