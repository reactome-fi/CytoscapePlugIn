package org.reactome.cytoscape3;
/**
 * This class generates a network based upon
 * FI interactions from a given input file
 * and the Reactome database.
 * @author Eric T Dawson
 */
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyTable;

public class CyNetworkGenerator
{
    private CyNetworkFactory networkFactory;
    public CyNetworkGenerator(CyNetworkFactory networkFactory)
    {
	this.networkFactory = networkFactory;
    }
    
    public CyNetwork constructFINetwork(Set<String> nodes,
	    				Set<String> fis,
	    				String title)
    {
        //Construct an empty network.
        CyNetwork network = networkFactory.createNetwork();
        CyTableFormatter cyTableFormatter = new CyTableFormatter();
        cyTableFormatter.makeAllTablesGSMA(network);
        //Generate a source, edge and target for each FI interaction
        //retrieved from the Reactome database.
        int index = 0;
        Map <String, CyNode> name2Node = new HashMap<String, CyNode>();
        for (String fi : fis)
        {
            index = fi.indexOf("\t");
            String name1 = fi.substring(0, index);
            String name2 = fi.substring(index + 1);
            CyNode node1 = getNode(name1, name2Node, network);
            CyNode node2 = getNode(name2, name2Node, network);
            CyEdge edge = createEdge(network, node1, node2, "FI");
        }
	
        return network;
    }
    
    private CyEdge createEdge(CyNetwork network, CyNode node1, CyNode node2,
                                String type)
    {
        //Add the edge to the network
        CyEdge edge = network.addEdge(node1, node2, true);
        //Add the edge attributes to the network CyTables
        return edge;
    }
    
    private CyNode getNode(String name,
	    		Map<String, CyNode>nameToNode,
	    		CyNetwork network)
    {
        //Retrieve a node's name from the hashmap.
        //If it exists, return it. Otherwise, create
        //the node.
        CyNode node = nameToNode.get(name);
        if (node != null)
            return node;
        node = createNode(network,
                name, "Gene", name);
        CyTable nodeTable = network.getDefaultNodeTable();
        Long nodeSUID = node.getSUID();
        nodeTable.getRow(nodeSUID).set("name", name);
        nameToNode.put(name, node);
        return node;
    }
    
    private CyNode createNode(CyNetwork network, String label,
	    			String type, String tooltip)
    {
	CyNode node = network.addNode();
	Long nodeSUID = node.getSUID();
	//Add node labels, tooltips, common names, etc.
	CyTable nodeTable = network.getDefaultNodeTable();
	//nodeTable.getRow(nodeSUID).set("NodeType", type);
	nodeTable.getRow(nodeSUID).set("nodeLabel", label);
	nodeTable.getRow(nodeSUID).set("commonName", label);
	nodeTable.getRow(nodeSUID).set("nodeToolTip", tooltip);
	return node;

    }
    
    public CyNetwork constructFINetwork(Set<String> fis, String title)
    {
        //This method is just a convenience method which
        //overloads constructFINetwork.
        return constructFINetwork(null, fis, title);
    }
}
