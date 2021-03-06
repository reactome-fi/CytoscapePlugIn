package org.reactome.cytoscape.service;

/**
 * This class generates a network based upon
 * FI interactions from a given input file
 * and the Reactome database.
 * @author Eric T Dawson & Guanming Wu
 */
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.funcInt.FIAnnotation;
import org.reactome.funcInt.Interaction;

public class FINetworkGenerator implements NetworkGenerator {
    private String edgeType = "FI"; // Default
    private String nodeType = "Gene"; // Default
    // To control how direction is created
    private boolean directionInEdgeName;
    
    public FINetworkGenerator() {
    }
    
    public String getEdgeType() {
        return edgeType;
    }

    public boolean isDirectionInEdgeName() {
        return directionInEdgeName;
    }

    public void setDirectionInEdgeName(boolean directionInEdgeName) {
        this.directionInEdgeName = directionInEdgeName;
    }

    public void setEdgeType(String edgeType) {
        this.edgeType = edgeType;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * Create a CyNetwork object based on the passed interaction set, fis. If nodes is provided
     * and some nodes are not used in the fis set, they will be displayed as un-linked nodes in
     * the generated CyNetwork.
     */
    public CyNetwork constructFINetwork(Set<String> nodes, Set<String> fis) {
        return constructFINetwork(nodes, fis, "FI");
    }
    
    /**
     * Create a CyNetwork object based on the passed interaction set, fis. If nodes is provided
     * and some nodes are not used in the fis set, they will be displayed as un-linked nodes in
     * the generated CyNetwork.
     */
    public CyNetwork constructFINetwork(Set<String> nodes, 
                                        Set<String> fis,
                                        String edgeType) {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        // Construct an empty network.
        CyNetwork network = PlugInUtilities.createNetwork();
        ServiceReference reference = context.getServiceReference(TableFormatter.class.getName());
        TableFormatter tableFormatter = (TableFormatter) context.getService(reference);
        tableFormatter.makeBasicTableColumns(network);
        tableFormatter = null;
        context.ungetService(reference);
        // Generate a source, edge and target for each FI interaction
        int index = 0;
        Map<String, CyNode> name2Node = new HashMap<String, CyNode>();
        for (String fi : fis) {
            index = fi.indexOf("\t");
            String name1 = fi.substring(0, index);
            String name2 = fi.substring(index + 1);
            CyNode node1 = getNode(name1, name2Node, network);
            CyNode node2 = getNode(name2, name2Node, network);
            createEdge(network, node1, node2, edgeType);
        }
        // Put nodes that are not linked to other genes in the network.
        if (nodes != null) {
            Set<String> copy = new HashSet<String>(nodes);
            copy.removeAll(name2Node.keySet());
            for (String name : copy)
            {
                CyNode node = getNode(name, name2Node, network);
            }
        }
        return network;
    }

    /**
     * A helper method to create a CyEdge for two nodes. The two nodes
     * are arranged as source and target alphabetically for annotations.
     * @param network
     * @param node1
     * @param node2
     * @param type
     * @return
     */
    public CyEdge createEdge(CyNetwork network, 
                             CyNode node1, 
                             CyNode node2,
                             String type) {
        CyTable nodeTable = network.getDefaultNodeTable();
        CyTable edgeTable = network.getDefaultEdgeTable();
        
        String node1Name = nodeTable.getRow(node1.getSUID()).get("name", String.class);
        String node2Name = nodeTable.getRow(node2.getSUID()).get("name", String.class);
        CyEdge edge = null;
        if (directionInEdgeName || node1Name.compareTo(node2Name) < 0) {
            // Add the edge to the network
            edge = network.addEdge(node1, node2, true);
            edgeTable.getRow(edge.getSUID()).set("name", node1Name + " (" + type + ") " + node2Name);
        }
        else {
            edge = network.addEdge(node2, node1, true);
            edgeTable.getRow(edge.getSUID()).set("name", node2Name + " (" + type + ") " + node1Name);
        }
        edgeTable.getRow(edge.getSUID()).set("EDGE_TYPE", type);
        return edge;
    }
    
    /**
     * Grabs a node from the hashmap based on its name and creates a new node if one doesn't exist.
     * @param name The name of the node to be returned.
     * @param nameToNode A hashmap of nodes with their names as keys.
     * @param network The CyNetwork to which the node belongs.
     * @return node The node with the given string name.
     */
    private CyNode getNode(String name, 
                           Map<String, CyNode> nameToNode,
                           CyNetwork network) {
        // Retrieve a node's name from the hashmap.
        // If it exists, return it. Otherwise, create
        // the node.
        CyNode node = nameToNode.get(name);
        if (node != null) return node;
        node = createNode(network, name, nodeType, name);
        CyTable nodeTable = network.getDefaultNodeTable();
        Long nodeSUID = node.getSUID();
        nodeTable.getRow(nodeSUID).set("name", name);
        nameToNode.put(name, node);
        return node;
    }
    
    private CyNode getNodeByName(String name, CyNetwork network) {
        CyTable nodeTable = network.getDefaultNodeTable();
        for (CyNode node : network.getNodeList()) {
            if(name.equals(nodeTable.getRow(node.getSUID()).get("name", String.class)))
                return node;
        }
        return null;
    }

    public CyNode createNode(CyNetwork network, 
                             String label,
                             String type,
                             String tooltip) {
        return createNode(network, label, label, type, tooltip);
    }
    
    public CyNode createNode(CyNetwork network,
                             String name,
                             String label,
                             String type,
                             String tooltip) {
        CyNode node = network.addNode();
        Long nodeSUID = node.getSUID();
        // Add node labels, tooltips, common names, etc.
        CyTable nodeTable = network.getDefaultNodeTable();
        nodeTable.getRow(nodeSUID).set("nodeType", type);
        nodeTable.getRow(nodeSUID).set("name", name);
        nodeTable.getRow(nodeSUID).set("nodeLabel", label);
        nodeTable.getRow(nodeSUID).set("commonName", label);
        nodeTable.getRow(nodeSUID).set("nodeToolTip", tooltip);
        return node;
    }

    /**
     * Construct a CyNetwork from the passed set of interactions, fis.
     * @param fis
     * @return
     */
    public CyNetwork constructFINetwork(Set<String> fis) {
        // This method is just a convenience method which
        // overloads constructFINetwork.
        return constructFINetwork(null, fis);
    }
    
    /**
     * Construct a CyNetwork from a collection of Interaction objects.
     * @param fis
     * @return
     */
    public CyNetwork constructFINetwork(Collection<Interaction> interactions) {
        Set<String> fis = new HashSet<String>();
        for (Interaction interaction : interactions) {
            String name1 = interaction.getFirstProtein().getShortName();
            String name2 = interaction.getSecondProtein().getShortName();
            fis.add(name1 + "\t" + name2);
        }
        return constructFINetwork(fis);
    }
    
    private void addFIPartners(CyNode targetNode, 
                               Set<String> partners,
                               String partnerNodeType,
                               boolean isLinker,
                               CyNetworkView view) {
        CyNetwork network = view.getModel();
        Map<CyNode, Set<CyNode>> oldToNew = new HashMap<CyNode, Set<CyNode>>();
        int index = 0;
        Set<CyNode> partnerNodes = new HashSet<CyNode>();
        TableHelper tableHelper = new TableHelper();
        for (String partner : partners) {
            CyNode partnerNode = getNodeByName(partner, network);
            if (partnerNode == null) {
                partnerNode = createNode(network, partner, partnerNodeType, partner);
                tableHelper.storeNodeAttribute(network,
                                               partnerNode,
                                               "isLinker",
                                               isLinker);
                partnerNodes.add(partnerNode);
                tableHelper.setNodeSelected(network, 
                                            partnerNode,
                                            true);
            }
            createEdge(network, targetNode, partnerNode, edgeType);
        }
        jiggleLayout(targetNode, 
                     partnerNodes,
                     view);
    }
    
    public void addFIPartners(Map<String, Set<String>> targetToPartners,
                              String partnerNodeType,
                              boolean isLinker,
                              CyNetworkView view) {
        for (String target : targetToPartners.keySet()) {
            Set<String> partners = targetToPartners.get(target);
            addFIPartners(getNodeByName(target, view.getModel()),
                          partners, 
                          partnerNodeType,
                          isLinker,
                          view);
        }
        view.updateView();
        // For some reason, we need to re-apply the visual style to make display correct.
        // This is a little weird!
        try {
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            ServiceReference servRef = context
                    .getServiceReference(FIVisualStyle.class.getName());
            FIVisualStyle visStyler = (FIVisualStyle) context.getService(servRef);
            visStyler.setVisualStyle(view);
            context.ungetService(servRef);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "The visual style could not be applied.",
                                          "Visual Style Error", 
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void addFIPartners(String target,
                              Set<String> partners,
                              String partnerNodeType,
                              boolean isLinker,
                              CyNetworkView view) {
        Map<String, Set<String>> targetToPartners = new HashMap<>();
        targetToPartners.put(target, partners);
        addFIPartners(targetToPartners, partnerNodeType, isLinker, view);
    }
    
    public void addFIPartners(String target,
                              Set<String> partners,
                              CyNetworkView view) {
        addFIPartners(target,
                      partners,
                      "Gene",
                      true, 
                      view);
    }

    public void jiggleLayout(CyNode anchor, Set<CyNode> partners, CyNetworkView view) {
        View<CyNode> nodeView = view.getNodeView(anchor);
        if (nodeView == null)
            return; // Nothing can be done
        // Just a sanity check since NodeView should be used in the following statements
        
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        String center = nodeTable.getRow(anchor.getSUID()).get("name", String.class);
        List<String> partnerNames = new ArrayList<String>();
        for (CyNode node : partners) {
            String name = nodeTable.getRow(node.getSUID()).get("name", String.class);
            partnerNames.add(name);
        }
        JiggleLayout layout = new JiggleLayout();
        Map<String, double[]> nameToCoord = layout.jiggleLayout(center, partnerNames);
        
        // Need to extract new coordinates
        double[] coords = nameToCoord.get(center);

        double dx = view.getNodeView(anchor).getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION) - coords[0];
        double dy = view.getNodeView(anchor).getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION) - coords[1];

        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference servRef = context.getServiceReference(CyEventHelper.class.getName());
        CyEventHelper eventHelper = (CyEventHelper) context.getService(servRef);
        
        for(CyNode node : partners) {
            String name = nodeTable.getRow(node.getSUID()).get("name", String.class);
            coords = nameToCoord.get(name);
            double x = coords[0] + dx;
            double y = coords[1] + dy;
            eventHelper.flushPayloadEvents();
            nodeView = view.getNodeView(node);
            if (nodeView == null)
                continue;
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
        }
        
        context.ungetService(servRef);
    }

    private void addFIs(Set<String> fis, CyNetwork network) {
        CyTable nodeTable = network.getDefaultNodeTable();
        List<CyNode> oldNodes = new ArrayList<CyNode>();
        //Copy all of the nodes in the network in to the list (deep copy).
        for (CyNode node : network.getNodeList())
        {
            oldNodes.add(node);
        }
        Map<CyNode, Set<CyNode>> oldToNew = new HashMap<CyNode, Set<CyNode>>();
        int index = 0;
        for (String fi : fis) {
            //Add the interacting nodes.
            index = fi.indexOf("\t");
            String gene1 = fi.substring(0, index);
            CyNode node1 = searchCyNode(gene1, network);
            String gene2 = fi.substring(index + 1);
            CyNode node2 = searchCyNode(gene2, network);
            // This method is used to add a FI between two existing nodes..
            // If any node doesn't exist, don't create any edge.
            if (node1 == null || node2 == null)
                continue;
            //Add the edge between the nodes.
            createEdge(network, node1, node2, "FI");

            //Create a map for use in enhancing the layout using JIGGLE
            CyNode oldNode = null;
            CyNode newNode = null;
            if (oldNodes.contains(node1) && !oldNodes.contains(node2))
            {
                oldNode = node1;
                newNode = node2;
            }
            else if (!oldNodes.contains(node1) && oldNodes.contains(node2))
            {
                oldNode = node2;
                newNode = node1;
            }
            if (oldNode != null && newNode != null)
            {
                Set<CyNode> newNodes = oldToNew.get(oldNode);
                if (newNodes == null) {
                    newNodes = new HashSet<CyNode>();
                    oldToNew.put(oldNode, newNodes);
                }
                newNodes.add(newNode);
            }
        }
    }
    
    public void addFIs(Set<String> fis, CyNetworkView view)
    {
        addFIs(fis, view.getModel());
        view.updateView();
    }
    
    /**
     * Search an added CyNode based on its name.
     * @param name
     * @param network
     * @return
     */
    private CyNode searchCyNode(String name, CyNetwork network) {
        CyTable nodeTable = network.getDefaultNodeTable();
        CyNode fiNode = null;
        for (CyNode node : network.getNodeList()) {
            Long tmpSUID = node.getSUID();
            String tmp = nodeTable.getRow(tmpSUID).get("name", String.class);
            if (name.equals(tmp)) {
                fiNode = node;
                nodeTable.getRow(tmpSUID).set("nodeLabel", name);
                nodeTable.getRow(tmpSUID).set("nodeToolTip", name);
                return node;
            }
        }
        return null;
    }
    
    public boolean annotateFIs(CyNetworkView view) {
        TableHelper tableHelper = new TableHelper();
        List<CyEdge> edges = null;
        List<CyEdge> annotatedEdges = new ArrayList<CyEdge>();
        List<CyEdge> unannotatedEdges = new ArrayList<CyEdge>();
        for (View<CyEdge> edgeView : view.getEdgeViews()) {
            CyEdge edge = edgeView.getModel();
            if (tableHelper.hasEdgeAttribute(view, edge, "FI Direction", String.class))
                annotatedEdges.add(edge);
            else
                unannotatedEdges.add(edge);
        }
        JFrame parentFrame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        if (!annotatedEdges.isEmpty() && !unannotatedEdges.isEmpty()) {
            int reply = JOptionPane.showConfirmDialog(parentFrame, "Some FIs have already been annotated. Would you like to annotate\n" +
                    "only those FIs without annotations? Choosing \"No\" will annotate all FIs.",
                    "FI Annotation", JOptionPane.YES_NO_CANCEL_OPTION);
            if (reply == JOptionPane.CANCEL_OPTION) {
                return false;
            }
            if (reply == JOptionPane.YES_OPTION)
                edges = unannotatedEdges;
            else {
                edges = unannotatedEdges;
                edges.addAll(annotatedEdges);
            }
        }
        else if (annotatedEdges.size() > 0) {
            int reply = JOptionPane.showConfirmDialog(parentFrame, "All FIs have been annotated. Would you like to re-annotate them?",
                                                      "FI Annotation", JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.NO_OPTION) { 
                return false;
            }
            edges = annotatedEdges;
        }
        else if (unannotatedEdges.size() > 0)
            edges = unannotatedEdges;
        
        if (edges == null || edges.size() == 0) {
            JOptionPane.showMessageDialog(parentFrame,
                                          "No FIs need to be annotated.",
                                          "FI Annotation", 
                                          JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        
        try {
            Map<String, String> edgeToAnnotation = new HashMap<String, String>();
            Map<String, String> edgeToDirection = new HashMap<String, String>();
            Map<String, Double> edgeToScore = new HashMap<String, Double>();
            RESTFulFIService service = new RESTFulFIService(view);
            Map<String, FIAnnotation> edgeIdToAnnotation = service.annotate(edges, view);
            for (String edgeId : edgeIdToAnnotation.keySet()) {
                FIAnnotation annotation = edgeIdToAnnotation.get(edgeId);
                edgeToAnnotation.put(edgeId, annotation.getAnnotation());
                edgeToDirection.put(edgeId, annotation.getDirection());
                edgeToScore.put(edgeId, annotation.getScore());
            }
            tableHelper.storeEdgeAttributesByName(view, "FI Annotation", edgeToAnnotation);
            tableHelper.storeEdgeAttributesByName(view, "FI Direction", edgeToDirection);
            tableHelper.storeEdgeAttributesByName(view, "FI Score", edgeToScore);
            return true;
        } 
        catch (Exception t) {
            PlugInUtilities.showErrorMessage("Error in annotating FIs",
                                             "FI Annotation failed. Please try again.");
            t.printStackTrace();
            return false;
        }
    }

}
