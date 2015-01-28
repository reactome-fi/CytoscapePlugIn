/*
 * Created on Mar 11, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.io.File;
import java.io.IOException;
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
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.factorgraph.Factor;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Variable;
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
    }
    
    @Override
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
            cacheData(line, 
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
}
