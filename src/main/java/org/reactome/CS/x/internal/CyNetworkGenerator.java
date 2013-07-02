package org.reactome.CS.x.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
	    
	}
	return network;
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
	// TODO Auto-generated method stub
	return null;
    }
}
