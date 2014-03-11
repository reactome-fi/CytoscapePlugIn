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
    
    public void loadData(File file,
                         ObservationType type) throws Exception {
        if (geneToDbIds == null)
            loadGeneToDdIds();
        if (type == ObservationType.CNV) {
            loadData(file, 
                     "DNA", 
                     getCNVFactorValues());
        }
        else if (type == ObservationType.GENE_EXPRESSION)
            loadData(file, 
                     "mRNA",
                     getExpressionFactorValues());
    }

    private void loadData(File file, 
                          String nodeType, 
                          List<Double> factorValues) throws IOException {
        FileUtility fu = new FileUtility();
        fu.setInput(file.getAbsolutePath());
        // First line should be header
        String line = fu.readLine();
        int index = 0;
        // A helper object to create Nodes and Edges
        FINetworkGenerator fiHelper = new FINetworkGenerator();
        // A helper set for layout
        Set<CyNode> partner = new HashSet<CyNode>();
        CyNetwork network = networkView.getModel();
        CyTable nodeTable = network.getDefaultNodeTable();
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
            CyNode node = fiHelper.createNode(network,
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
                                                  node,
                                                  factorNode,
                                                  "FI");
                CyNode varNode = labelToNode.get(dbId + "_" + nodeType);
                edge = fiHelper.createEdge(network, 
                                           varNode, 
                                           factorNode,
                                           "FI");
                partner.clear();
                partner.add(factorNode);
                fiHelper.jiggleLayout(varNode,
                                      partner,
                                      networkView);
                partner.clear();
                partner.add(node);
                fiHelper.jiggleLayout(factorNode, 
                                      partner, 
                                      networkView);
            }
            nodeTable.getRow(node.getSUID()).set("sourceIds", StringUtils.join(",", dbIds));
        }
        fu.close();
        fg.validatVariables();
//        networkView.updateView();
        // Need to recall visual style in order to make newly added nodes to have
        // correct visual styles.
        FIVisualStyle visStyler = new FactorGraphVisualStyle();
        visStyler.setVisualStyle(networkView);
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
