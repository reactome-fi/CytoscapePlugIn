/*
 * Created on Mar 11, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.util.StringUtils;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.pgm.Observation;
import org.reactome.pgm.PGMFactor;
import org.reactome.pgm.PGMFactorGraph;
import org.reactome.pgm.PGMVariable;
import org.reactome.r3.util.FileUtility;

/**
 * This class is used to process observation data for a displayed PGMFactorGraph object.
 * @author gwu
 *
 */
public class ObservationDataHelper {
    private CyNetworkView networkView;
    private PGMFactorGraph fg;
    // For quick find variables
    private Map<String, PGMVariable> labelToVar;
    private Map<String, CyNode> labelToNode;
    // In order to assign ids to new variable
    private long maxId;
    private Map<String, List<Long>> geneToDbIds;
    
    /**
     * Default constructor.
     */
    public ObservationDataHelper(PGMFactorGraph fg,
                                 CyNetworkView netView) {
        if (fg == null || netView == null)
            throw new IllegalArgumentException("Factor graph and network view cannot be null!");
        this.fg = fg;
        this.networkView = netView;
        initializeProperties();
    }

    private void initializeProperties() {
        labelToVar = new HashMap<String, PGMVariable>();
        for (PGMVariable var : fg.getVariables()) {
            labelToVar.put(var.getLabel(), var);
        }
        // Get the maximum ids, which should be long, in order to assign to new variables
        List<PGMVariable> variables = new ArrayList<PGMVariable>(fg.getVariables());
        Collections.sort(variables, new Comparator<PGMVariable>() {
            public int compare(PGMVariable var1, PGMVariable var2) {
                Long id1 = new Long(var1.getId());
                Long id2 = new Long(var2.getId());
                return id1.compareTo(id2);
            }
        });
        maxId = new Long(variables.get(variables.size() - 1).getId());
        labelToNode = new HashMap<String, CyNode>();
        TableHelper tableHelper = new TableHelper();
        for (CyNode node : networkView.getModel().getNodeList()) {
            String label = tableHelper.getStoredNodeAttribute(networkView.getModel(), node, "nodeLabel", String.class);
            labelToNode.put(label, node);
        }
    }
    
    public Map<PGMVariable, Map<String, Integer>> loadData(File file,
                                                           ObservationType type,
                                                           double[] thresholdValues) throws Exception {
        if (geneToDbIds == null)
            loadGeneToDdIds();
        if (type == ObservationType.CNV) {
            return loadData(file, 
                            "DNA", 
                            getCNVFactorValues(),
                            thresholdValues);
        }
        else if (type == ObservationType.GENE_EXPRESSION)
            return loadData(file, 
                            "mRNA",
                            getExpressionFactorValues(),
                            thresholdValues);
        return null;
    }
    
    /**
     * Generate a list of Observations from a set of varToSampleToStates.
     * @param varToSampleToStates
     * @return
     */
    public List<Observation> generateObservations(Map<PGMVariable, Map<String, Integer>>... varToSampleToStates) {
        // Get all samples mentioned in the parameters.
        Set<String> samples = new HashSet<String>();
        for (Map<PGMVariable, Map<String, Integer>> varToSampleToState : varToSampleToStates) {
            for (PGMVariable var : varToSampleToState.keySet()) {
                samples.addAll(varToSampleToState.get(var).keySet());
            }
        }
        List<Observation> observations = new ArrayList<Observation>();
        for (String sample : samples) {
            Observation observation = new Observation();
            observation.setSample(sample);
            for (Map<PGMVariable, Map<String, Integer>> varToSampleToState : varToSampleToStates) {
                for (PGMVariable var : varToSampleToState.keySet()) {
                    Map<String, Integer> sampleToState = varToSampleToState.get(var);
                    if (sampleToState.containsKey(sample)) {
                        observation.addObserved(var.getId(), 
                                                sampleToState.get(sample));
                    }
                }
            }
            observations.add(observation);
        }
        return observations;
    }
    
    private List<String> parseSamples(String line) {
        String[] tokens = line.split("\t");
        List<String> samples = new ArrayList<String>();
        for (int i = 1; i < tokens.length; i++)
            samples.add(tokens[i]);
        return samples;
    }

    private Map<PGMVariable, Map<String, Integer>> loadData(File file, 
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
        Map<PGMVariable, Map<String, Integer>> variableToSampleToState = new HashMap<PGMVariable, Map<String,Integer>>();
        while ((line = fu.readLine()) != null) {
            index = line.indexOf("\t");
            String gene = line.substring(0, index);
            List<Long> dbIds = geneToDbIds.get(gene);
            if (dbIds == null || dbIds.size() == 0)
                continue;
            // Just use the first DB_ID as its label
            PGMVariable obsVar = createObsVariable(dbIds.get(0), 
                                                   nodeType);
            // Add this observation variable into the network.
            CyNode obsNode = fiHelper.createNode(network,
                                                 obsVar.getLabel(), 
                                                 "observation", 
                                                 obsVar.getLabel());
            for (Long dbId : dbIds) {
                PGMVariable var = labelToVar.get(dbId + "_" + nodeType);
                if (var == null)
                    continue;
                PGMFactor factor = createObsFactor(obsVar, 
                                                   var, 
                                                   factorValues);
                CyNode factorNode = fiHelper.createNode(network,
                                                        factor.getLabel(), 
                                                        "factor",
                                                        factor.getLabel());
                // Don't want to show lable for factor node. So
                // a simple fix
                nodeTable.getRow(factorNode.getSUID()).set("nodeLabel", null);
                CyEdge edge = fiHelper.createEdge(network,
                                                  obsNode,
                                                  factorNode,
                                                  "FI");
                CyNode varNode = labelToNode.get(dbId + "_" + nodeType);
                edge = fiHelper.createEdge(network, 
                                           varNode, 
                                           factorNode,
                                           "FI");
                varNodeToFactorNode.put(varNode, factorNode);
                factorNodeToObsNode.put(factorNode, obsNode);
            }
            nodeTable.getRow(obsNode.getSUID()).set("sourceIds", StringUtils.join(",", dbIds));
            parseData(line, 
                      samples,
                      obsVar,
                      variableToSampleToState,
                      thresholdValues);
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
    
    private void parseData(String line,
                           List<String> samples,
                           PGMVariable variable,
                           Map<PGMVariable, Map<String, Integer>> variableToSampleToState,
                           double[] thresholdValues) {
        Map<String, Integer> sampleToState = new HashMap<String, Integer>();
        String[] tokens = line.split("\t");
        for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].length() == 0 || tokens[i].toLowerCase().equals("na"))
                continue;
            double value = new Double(tokens[i]);
            // A simple discretizing method
            if (value >= thresholdValues[thresholdValues.length - 1]) {
                sampleToState.put(samples.get(i - 1),
                                  thresholdValues.length);
            }
            else {
                for (int j = 0; j < thresholdValues.length; j++) {
                    if (value < thresholdValues[j]) {
                        sampleToState.put(samples.get(i - 1),
                                          j);
                        break;
                    }
                }
            }
        }
        variableToSampleToState.put(variable, sampleToState);
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
    
    private PGMFactor createObsFactor(PGMVariable obsVar,
                                      PGMVariable hiddenVar,
                                      List<Double> factorValues) {
        PGMFactor factor = new PGMFactor();
        factor.addVariable(obsVar);
        factor.addVariable(hiddenVar);
        obsVar.setStates(hiddenVar.getStates());
        String factorLabel = hiddenVar.getLabel() + "," + obsVar.getLabel();
        factor.setLabel(factorLabel);
        factor.setName(factorLabel);
        for (Double value : factorValues)
            factor.addValue(value);
        fg.addFactor(factor);
        return factor;
    }
    
    private PGMVariable createObsVariable(Long dbId,
                                          String type) {
        PGMVariable obsVar = new PGMVariable();
        obsVar.setId(++maxId + "");
        String label = dbId + "_" + type + "_obs";
        obsVar.setLabel(label);
        obsVar.setName(label);
        return obsVar;
    }
    
    private List<Double> getExpressionFactorValues() {
        String values = PlugInObjectManager.getManager().getProperties().getProperty("expressionFactorValues");
        return getFactorValues(values);
    }

    private List<Double> getFactorValues(String values) {
        String[] tokens = values.split(",");
        List<Double> rtn = new ArrayList<Double>();
        for (String token : tokens)
            rtn.add(new Double(token));
        return rtn;
    }
    
    private List<Double> getCNVFactorValues() {
        String values = PlugInObjectManager.getManager().getProperties().getProperty("cnvFactorValues");
        return getFactorValues(values);
    }
    
    private void loadGeneToDdIds() throws Exception {
        Set<Long> dbIds = new HashSet<Long>();
        int index = 0;
        for (PGMVariable variable : fg.getVariables()) {
            String label = variable.getLabel();
            if (label.matches("\\d+_DNA") || label.matches("\\d+_mRNA")) {
                index = label.indexOf("_");
                dbIds.add(new Long(label.substring(0, index)));
            }
        }
        Set<Long> ewasIds = dbIds;
        RESTFulFIService restfulAPI = new RESTFulFIService();
        geneToDbIds = restfulAPI.getGeneToEWASIds(ewasIds);
    }
    
}
