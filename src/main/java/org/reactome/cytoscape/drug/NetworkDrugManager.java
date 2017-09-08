/*
 * Created on Dec 29, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyEdge.Type;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.jdom.Element;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;

import edu.ohsu.bcb.druggability.dataModel.Interaction;

/**
 * This manager is used to handle cancer drug overlay for the FI network view.
 * @author gwu
 *
 */
public class NetworkDrugManager extends DrugTargetInteractionManager {
    private static NetworkDrugManager manager;
    private Map<String, List<Interaction>> geneToInteractions;
    private NetworkInteractionFilter interactionFilter;
    // Track loading to avoid duplication based on SUID to avoid memory leak
    private Set<Long> handledNetworks;
    
    /**
     * Default constructor.
     */
    protected NetworkDrugManager() {
        geneToInteractions = new HashMap<>();
        interactionFilter = new NetworkInteractionFilter();
        handledNetworks = new HashSet<>();
    }
    
    public static NetworkDrugManager getManager() {
        if (manager == null)
            manager = new NetworkDrugManager();
        return manager;
    }
    
    /**
     * Get the Interaction displayed for a drug/target edge.
     * @param edge
     * @return an Interaction displayed by the passed edge.
     */
    public Interaction getInteraction(CyNetwork network,
                                      CyEdge edge) {
        CyTable nodeTable = network.getDefaultNodeTable();
        // Get the gene node and drug node
        String geneName = null;
        String drugName = null;
        CyNode sourceNode = edge.getSource();
        String nodeType = nodeTable.getRow(sourceNode.getSUID()).get("nodeType", String.class);
        if (nodeType != null) {
            String nodeName = nodeTable.getRow(sourceNode.getSUID()).get("name", String.class);
            if (nodeType.equals("Gene"))
                geneName = nodeName;
            else if (nodeType.equals("Drug"))
                drugName = nodeName;
        }
        CyNode targetNode = edge.getTarget();
        nodeType = nodeTable.getRow(targetNode.getSUID()).get("nodeType", String.class);
        if (nodeType != null) {
            String nodeName = nodeTable.getRow(targetNode.getSUID()).get("name", String.class);
            if (nodeType.equals("Gene"))
                geneName = nodeName;
            else if (nodeType.equals("Drug"))
                drugName = nodeName;
        }
        if (geneName == null || drugName == null)
            return null;
        List<Interaction> interactions = geneToInteractions.get(geneName);
        if (interactions == null)
            return null;
        for (Interaction interaction : interactions) {
            String drugName1 = interaction.getIntDrug().getDrugName();
            if (drugName.equals(drugName1))
                return interaction;
        }
        return null;
    }
    
    public void fetchCancerDrugs(CyNetworkView view) throws Exception {
        if (handledNetworks.contains(view.getModel().getSUID())) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cancer drugs have been fetched for the displayed network.\n" + 
                                          "Use \"Filter Cancer Drugs\" to modify overlay.",
                                          "Fetch Cancer Drugs",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        List<CyNode> nodeList = view.getModel().getNodeList(); 
        if (nodeList == null || nodeList.size() == 0)
            return;
        Set<String> genes = new HashSet<String>();
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        for (CyNode node : nodeList) {
            Long nodeSUID = node.getSUID();
            String nodeName = nodeTable.getRow(nodeSUID).get("name", String.class);
            genes.add(nodeName);
        }
        if (genes.size() == 0)
            return; // Nothing to be displayed
        RESTFulFIService service = new RESTFulFIService();
        Element element = service.queryDrugTargetInteractionsForGenes(genes);
        DrugTargetInteractionParser parser = new DrugTargetInteractionParser();
        parser.parse(element);
        List<Interaction> interactions = parser.getInteractions();
        if (interactions == null || interactions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "No cancer drugs can be found for genes displayed in the network.",
                                          "No Cancer Drugs",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        // Need to sort these interactions
        cacheInteractions(genes, interactions);
        // Display these interactions as edges, one of which may be supported by multiple interactions.
        displayInteractions(genes, view);
        handledNetworks.add(view.getModel().getSUID());
    }
    
    private void displayInteractions(Set<String> genes,
                                     CyNetworkView view) {
        Map<String, Set<String>> geneToDrugs = new HashMap<>();
        for (String gene : genes) {
            List<Interaction> interactions = geneToInteractions.get(gene);
            if (interactions.size() == 0)
                continue;
            // Get a set of drugs
            Set<String> drugs = new HashSet<>();
            for (Interaction interaction : interactions) {
                if (interactionFilter.filter(interaction)) {
                    drugs.add(interaction.getIntDrug().getDrugName());
                }
            }
            if (drugs.size() > 0) {
                geneToDrugs.put(gene, drugs);
            }
        }
        if (geneToDrugs.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "No drugs can be displayed. Adjust the filter to show interactions.",
                                          "No Drugs to Display",
                                          JOptionPane.INFORMATION_MESSAGE);
        }
        else {
            FINetworkGenerator helper = new FINetworkGenerator();
            helper.setEdgeType("Drug/Target");
            helper.addFIPartners(geneToDrugs, "Drug", false, view);
        }
    }   
    
    private void cacheInteractions(Set<String> genes,
                                   List<Interaction> interactions) {
        for (Interaction interaction : interactions) {
            String target = interaction.getIntTarget().getTargetName();
            List<Interaction> targetInts = geneToInteractions.get(target);
            if (targetInts == null) {
                targetInts = new ArrayList<>();
                geneToInteractions.put(target, targetInts);
            }
            targetInts.add(interaction);
        }
        // Keep track empty interactions
        for (String gene : genes) {
            List<Interaction> geneInts = geneToInteractions.get(gene);
            if (geneInts == null) {
                geneInts = new ArrayList<>();
                geneToInteractions.put(gene, geneInts);
            }
        }
    }
    
    public void filterCancerDrugs(CyNetworkView networkView) {
        if (geneToInteractions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "There is no drug fetched. Fetch drugs first before filtering.",
                                          "No Drug for Filtering",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        interactionFilter.setNetworkView(networkView);
        interactionFilter.showDialog();
    }
    
    public void applyFilter(CyNetworkView networkView) {
        CyNetwork network = networkView.getModel();
        // Get a list of edges to be deleted
        Set<CyEdge> toBeDeletedEdges = new HashSet<>();
        // Get a map from gene to drugs after filtering
        CyTable nodeTable = network.getDefaultNodeTable();
        // New interactions to be displayed
        Map<String, Set<String>> geneToNewDrugs = new HashMap<>();
        for (CyNode node : network.getNodeList()) {
            String nodeName = nodeTable.getRow(node.getSUID()).get("name", String.class);
            List<Interaction> interactions = geneToInteractions.get(nodeName);
            if (interactions == null || interactions.size() == 0)
                continue;
            Set<String> filteredDrugs = new HashSet<>();
            for (Interaction interaction : interactions) {
                if (!interactionFilter.filter(interaction))
                    continue;
                String drug = interaction.getIntDrug().getDrugName();
                filteredDrugs.add(drug);
            }
            // In Drug/Target interactions, drugs are attached as source
            List<CyEdge> edges = network.getAdjacentEdgeList(node, Type.DIRECTED);
            if (edges != null && edges.size() > 0) {
                for (CyEdge edge : edges) {
                    CyNode drugNode = getDrugNode(edge, nodeTable);
                    if (drugNode == null)
                        continue;
                    String drugName = nodeTable.getRow(drugNode.getSUID()).get("name", String.class);
                    if (filteredDrugs.contains(drugName))
                        filteredDrugs.remove(drugName);
                    else
                        toBeDeletedEdges.add(edge);
                }
            }
            if (filteredDrugs.size() > 0)
                geneToNewDrugs.put(nodeName, filteredDrugs);
        }
        // Delete edges first
        network.removeEdges(toBeDeletedEdges);
        // Add new interactions if any
        if (geneToNewDrugs.size() > 0) {
            FINetworkGenerator helper = new FINetworkGenerator();
            helper.setEdgeType("Drug/Target");
            helper.addFIPartners(geneToNewDrugs, "Drug", false, networkView);
        }
        // Remove drugs that don't have any links
        Set<CyNode> drugToBeRemoved = new HashSet<>();
        for (CyNode node : network.getNodeList()) {
            String nodeType = nodeTable.getRow(node.getSUID()).get("nodeType", String.class);
            if (nodeType != null && nodeType.equals("Drug")) {
                List<CyEdge> edges = network.getAdjacentEdgeList(node, Type.ANY);
                if (edges == null || edges.size() == 0)
                    drugToBeRemoved.add(node);
            }
        }
        if (drugToBeRemoved.size() > 0) {
            network.removeNodes(drugToBeRemoved);
            networkView.updateView();
        }
    }
    
    public void removeCancerDrugs(CyNetworkView networkView) {
        CyNetwork network = networkView.getModel();
        CyTable edgeTable = network.getDefaultEdgeTable();
        CyTable nodeTable = network.getDefaultNodeTable();
        List<CyEdge> toBeDeleted = new ArrayList<>();
        List<CyNode> drugNodes = new ArrayList<>();
        for (CyEdge edge : network.getEdgeList()) {
            String edgeType = edgeTable.getRow(edge.getSUID()).get("EDGE_TYPE", String.class);
            if (edgeType != null && edgeType.equals("Drug/Target")) {
                toBeDeleted.add(edge);
                CyNode drugNode = getDrugNode(edge, nodeTable);
                if (drugNode != null)
                    drugNodes.add(drugNode);
            }
        }
        network.removeEdges(toBeDeleted);
        network.removeNodes(drugNodes);
        networkView.updateView();
        handledNetworks.remove(networkView.getModel().getSUID());
    }
    
    private CyNode getDrugNode(CyEdge edge,
                               CyTable nodeTable) {
        CyNode source = edge.getSource();
        String nodeType = nodeTable.getRow(source.getSUID()).get("nodeType", String.class);
        if (nodeType != null && nodeType.equals("Drug"))
            return source;
        CyNode target = edge.getTarget();
        nodeType = nodeTable.getRow(target.getSUID()).get("nodeType", String.class);
        if (nodeType != null && nodeType.equals("Drug"))
            return target;
        return null;
    }
    
}
