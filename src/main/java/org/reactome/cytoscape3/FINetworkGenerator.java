package org.reactome.cytoscape3;

/**
 * This class generates a network based upon
 * FI interactions from a given input file
 * and the Reactome database.
 * @author Eric T Dawson
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import jiggle.*;

import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.NetworkGenerator;
import org.reactome.cytoscape.service.TableFormatter;

public class FINetworkGenerator implements NetworkGenerator
{
    private CyNetworkFactory networkFactory;
//    private CyTableFactory tableFactory;
//    private CyTableManager tableManager;
    private ServiceReference tableFormatterServRef;
    private TableFormatterImpl tableFormatter;
    private ServiceReference networkFactoryRef;
    private ServiceReference tableManagerRef;
    private ServiceReference tableFactoryRef;

    public FINetworkGenerator(CyNetworkFactory networkFactory,
            CyTableFactory tableFactory, CyTableManager tableManager)
    {
        this.networkFactory = networkFactory;
//        this.tableFactory = tableFactory;
//        this.tableManager = tableManager;
    }
    public FINetworkGenerator()
    {
    }
    private void getTableFormatter()
    {
        try
        {
            BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
            ServiceReference servRef = context.getServiceReference(TableFormatter.class.getName());
            if (servRef != null)
            {
                this.tableFormatterServRef = servRef;
                this.tableFormatter = (TableFormatterImpl) context.getService(servRef);
            }
            else
                throw new Exception();
        }
        catch (Throwable t)
        {
            JOptionPane.showMessageDialog(null, "The table formatter could not be retrieved.", "Table Formatting Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    private void releaseTableFormatter()
    {
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        context.ungetService(tableFormatterServRef);
    }
    private void getCyServices()
    {
        Map<ServiceReference,  Object> netFactoryRefToObj =  PlugInScopeObjectManager.getManager().getServiceReferenceObject(CyNetworkFactory.class.getName());
        ServiceReference servRef = (ServiceReference) netFactoryRefToObj.keySet().toArray()[0];
        CyNetworkFactory netFactory = (CyNetworkFactory) netFactoryRefToObj.get(servRef);
        this.networkFactory = netFactory;
        this.networkFactoryRef = servRef;

        Map<ServiceReference,  Object> tableManagerRefToObj =  PlugInScopeObjectManager.getManager().getServiceReferenceObject(CyTableManager.class.getName());
        servRef = (ServiceReference) tableManagerRefToObj.keySet().toArray()[0];
        CyTableManager tableManager = (CyTableManager) tableManagerRefToObj.get(servRef);
//        this.tableManager = tableManager;
        this.tableManagerRef = servRef;

        Map<ServiceReference,  Object> tableFactoryRefToObj =  PlugInScopeObjectManager.getManager().getServiceReferenceObject(CyTableFactory.class.getName());
        servRef = (ServiceReference) tableFactoryRefToObj.keySet().toArray()[0];
        CyTableFactory tableFactory = (CyTableFactory) tableFactoryRefToObj.get(servRef);
//        this.tableFactory = tableFactory;
        this.tableFactoryRef = servRef;

        getTableFormatter();
    }

    private void releaseCyServices()
    {
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        if (networkFactoryRef != null)
            context.ungetService(networkFactoryRef);
        if (tableManagerRef != null)
            context.ungetService(tableManagerRef);
        if (tableFactoryRef != null)
            context.ungetService(tableFactoryRef);
        if (tableFormatterServRef != null)
            releaseTableFormatter();
    }
    public CyNetwork constructFINetwork(Set<String> nodes, Set<String> fis)
    {
        getCyServices();
        // Construct an empty network.
        CyNetwork network = networkFactory.createNetwork();
        tableFormatter.makeBasicTableColumns(network);
        // Generate a source, edge and target for each FI interaction
        // retrieved from the Reactome database.
        int index = 0;
        Map<String, CyNode> name2Node = new HashMap<String, CyNode>();
        for (String fi : fis)
        {
            index = fi.indexOf("\t");
            String name1 = fi.substring(0, index);
            String name2 = fi.substring(index + 1);
            CyNode node1 = getNode(name1, name2Node, network);
            CyNode node2 = getNode(name2, name2Node, network);
            // CyEdge edge =
            createEdge(network, node1, node2, "FI");
        }
        // Put nodes that are not linked to other genes in the network.
        if (nodes != null)
        {
            Set<String> copy = new HashSet<String>(nodes);
            copy.removeAll(name2Node.keySet());
            for (String name : copy)
            {
                CyNode node = getNode(name, name2Node, network);
            }
        }
        releaseCyServices();
        return network;
    }

    private CyEdge createEdge(CyNetwork network, CyNode node1, CyNode node2,
            String type)
    {
        // Add the edge to the network
        CyEdge edge = network.addEdge(node1, node2, true);
        //Add the node attributes.
        CyTable edgeTable = network.getDefaultEdgeTable();
        CyTable nodeTable = network.getDefaultNodeTable();
        String node1Name = nodeTable.getRow(node1.getSUID()).get("name", String.class);
        String node2Name = nodeTable.getRow(node2.getSUID()).get("name", String.class);
        edgeTable.getRow(edge.getSUID()).set("name", node1Name + " " + type + " " + node2Name);
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
    private CyNode getNode(String name, Map<String, CyNode> nameToNode,
            CyNetwork network)
    {
        // Retrieve a node's name from the hashmap.
        // If it exists, return it. Otherwise, create
        // the node.
        CyNode node = nameToNode.get(name);
        if (node != null) return node;
        node = createNode(network, name, "Gene", name);
        CyTable nodeTable = network.getDefaultNodeTable();
        Long nodeSUID = node.getSUID();
        nodeTable.getRow(nodeSUID).set("name", name);
        nameToNode.put(name, node);
        return node;
    }

    private CyNode createNode(CyNetwork network, String label, String type,
            String tooltip)
    {
        CyNode node = network.addNode();
        Long nodeSUID = node.getSUID();
        // Add node labels, tooltips, common names, etc.
        CyTable nodeTable = network.getDefaultNodeTable();
        nodeTable.getRow(nodeSUID).set("nodeType", type);
        nodeTable.getRow(nodeSUID).set("name", label);
        nodeTable.getRow(nodeSUID).set("nodeLabel", label);
        nodeTable.getRow(nodeSUID).set("commonName", label);
        nodeTable.getRow(nodeSUID).set("nodeToolTip", tooltip);
        return node;

    }

    public CyNetwork constructFINetwork(Set<String> fis)
    {
        // This method is just a convenience method which
        // overloads constructFINetwork.
        return constructFINetwork(null, fis);
    }
    
    public void addFIPartners(CyNode targetNode, Set<String> partners, CyNetworkView view)
    {
        CyNetwork network = view.getModel();
        Map<CyNode, Set<CyNode>> oldToNew = new HashMap<CyNode, Set<CyNode>>();
        int index = 0;
        Set<CyNode> partnerNodes = new HashSet<CyNode>();
        for (String partner : partners)
        {
            CyNode partnerNode = NodeActionCollection.getNodeByName(partner, network);
            if (partnerNode == null)
            {
                partnerNode = createNode(network, partner, "Gene", partner);
                CyTable nodeTable = network.getDefaultNodeTable();
                nodeTable.getRow(partnerNode.getSUID()).set("isLinker", true);
                partnerNodes.add(partnerNode);
            }
            createEdge(network, targetNode, partnerNode, "FI");
        }
        jiggleLayout(targetNode, partnerNodes, view);
        NetworkActionCollection.selectNodesFromList(network, partnerNodes, true);

    }
    
    public void addFIPartners(String target, Set<String> partners, CyNetworkView view)
    {
        addFIPartners(NodeActionCollection.getNodeByName(target, view.getModel()), partners, view);
        view.updateView();
        try
        {
            BundleContext context = PlugInScopeObjectManager.getManager()
                    .getBundleContext();
            ServiceReference servRef = context
                    .getServiceReference(FIVisualStyle.class.getName());
            FIVisualStyleImpl visStyler = (FIVisualStyleImpl) context
                    .getService(servRef);
            visStyler.setVisualStyle(view);
        }
        catch (Throwable t)
        {
            JOptionPane.showMessageDialog(null,
                    "The visual style could not be applied.",
                    "Visual Style Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void jiggleLayout(CyNode anchor, Set<CyNode> partners, CyNetworkView view)
    {
        jiggle.Graph graph = new jiggle.Graph();
        //Initialize the center of the graph.
        Vertex center = graph.insertVertex();
        initializeJiggleVertex(center);
        List<Vertex> vertices = new ArrayList<Vertex>();
        for (int i1 = 0; i1 < partners.size(); i1++)
        {
            Vertex v = graph.insertVertex();
            initializeJiggleVertex(v);
            graph.insertEdge(v, center);
            vertices.add(v);
        }
        jiggle.Graph g = graph;
        int d = g.getDimensions();
        double k = 25;
        SpringLaw springLaw = new QuadraticSpringLaw(g, k);
        // Use strong repulsion
        VertexVertexRepulsionLaw vvRepulsionLaw = new HybridVertexVertexRepulsionLaw(g, 3 * k);
        //vvRepulsionLaw.setBarnesHutTheta(0.9d);
        VertexEdgeRepulsionLaw veRepulsionLaw = new InverseSquareVertexEdgeRepulsionLaw(g, k);
        ForceModel fm = new ForceModel(g);
        fm.addForceLaw (springLaw);
        fm.addForceLaw (vvRepulsionLaw);
        // Using a force repulsion law hurts performance unfortunately.
        fm.addForceLaw(veRepulsionLaw);
        double acc = 0.5d;
        double rt = 0.2d;
        FirstOrderOptimizationProcedure opt = new ConjugateGradients(g, fm, acc, rt);
        opt.setConstrained(true);
        // Do a layout for 100 iterations
        for (int i = 0; i < 40; i++)
            opt.improveGraph();
        // Grab the position of the anchor.
        double[] coords = center.getCoords();
        double dx = view.getNodeView(anchor).getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION) - coords[0];
        double dy = view.getNodeView(anchor).getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION) - coords[1];
        int i = 0;
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        ServiceReference servRef = context.getServiceReference(CyEventHelper.class.getName());
        CyEventHelper eventHelper = (CyEventHelper) context.getService(servRef);
        
        for(CyNode node : partners)
        {
            Vertex v = vertices.get(i);
            i ++;
            double x = v.getCoords()[0] + dx;
            double y = v.getCoords()[1] + dy;
            eventHelper.flushPayloadEvents();
            View<CyNode> nodeView = view.getNodeView(node);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, x);
            nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, y);
        }
    }
    protected void initializeJiggleVertex(Vertex v)
    {
        // Assign a random position
        double[] pos = v.getCoords();
        pos[0] = 500 * Math.random();
        pos[1] = 500 * Math.random();
        // Assign a fixed size
        double[] size = v.getSize();
        size[0] = (int) 50;
        size[1] = (int) 50;
    }

    private void addFIs(Set<String> fis, CyNetwork network)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        List<CyNode> oldNodes = new ArrayList<CyNode>();
        //Copy all of the nodes in the network in to the list (deep copy).
        for (CyNode node : network.getNodeList())
        {
            oldNodes.add(node);
        }
        Map<CyNode, Set<CyNode>> oldToNew = new HashMap<CyNode, Set<CyNode>>();
        int index = 0;
        for (String fi : fis)
        {
            //Add the interacting nodes.
            index = fi.indexOf("\t");
            String gene1 = fi.substring(0, index);
            CyNode node1 = addFINode(gene1, network);
            String gene2 = fi.substring(index + 1);
            CyNode node2 = addFINode(gene2, network);
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
    private CyNode addFINode(String name, CyNetwork network)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        TableHelper tableHelper = new TableHelper();
        boolean found = false;
        CyNode fiNode = null;
        for (CyNode node : network.getNodeList())
        {
            Long tmpSUID = node.getSUID();
            String tmp = nodeTable.getRow(tmpSUID).get("name", String.class);
            if (name.equals(tmp))
            {
                fiNode = node;
                found = true;
                nodeTable.getRow(tmpSUID).set("nodeLabel", name);
                nodeTable.getRow(tmpSUID).set("nodeToolTip", name);
                break;
            }
        }
        if (!found)
        {
            fiNode = network.addNode();
            nodeTable.getRow(fiNode.getSUID()).set("nodeLabel", name);
            nodeTable.getRow(fiNode.getSUID()).set("nodeToolTip", name);
        }
        return fiNode;
    }

}
