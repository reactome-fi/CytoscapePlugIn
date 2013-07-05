package org.reactome.cytoscape3;

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
	CyNetwork network = networkFactory.createNetwork();
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
        /**
         * CyTableManager
         * 
         */
        return edge;
    }
    private CyNode getNode(String name,
	    		Map<String, CyNode>nameToNode,
	    		CyNetwork network){
	CyNode node = nameToNode.get(name);
	if (node != null)
	    return node;
	node = createNode(network,
			name, "Gene", name);
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
	nodeTable.getRow(node).set("NodeType", type);
	nodeTable.getRow(node).set("NodeLabel", label);
	nodeTable.getRow(node).set("CommonName", label);
	nodeTable.getRow(node).set("NodeToolTip", tooltip);
	return node;

    }
    
    public CyNetwork constructFINetwork(Set<String> fis, String title)
    {
	return constructFINetwork(null, fis, title);
    }
}
