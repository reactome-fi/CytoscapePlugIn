package org.reactome.CS.x.internal;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;

public class CyTableFormatterListener  implements NetworkAddedListener
{

    // Key for storing FI network version
    private final String FI_NETWORK_VERSION = "Reactome_FI_Network_Version";
    
    
    
    //fix it. It may be necessary to make an identical view table to the network table.
    
    
    
    public CyTableFormatterListener()
    {
    }
    public void makeTableGSMA(CyNetwork network)
    {
	
	CyTable netTable = network.getDefaultNetworkTable();
	CyTable nodeTable = network.getDefaultNodeTable();
	CyTable edgeTable = network.getDefaultEdgeTable();
	

	//From Jason's email. Make sure that the network SUID ends up in the properly linked table
	//in the edge/node table to ensure that network properties are distributed to nodes and edges.
	if (nodeTable.getColumn("network") == null)
	{
	    nodeTable.createColumn("network", Long.class, true);
	    edgeTable.createColumn("network", Long.class, true);
	    nodeTable.addVirtualColumn("network.SUID", "SUID", netTable, "network.SUID", true);
	    edgeTable.addVirtualColumn("network.SUID", "SUID", netTable, "network.SUID", true);
	    
	}
	//Creates a set of columns in the default network table and creates matching virtual columns
	//within the default edge and node tables upon network creation or network view creation.
	if (netTable.getColumn("isReactomeFINetwork") == null)
	{
	    netTable.createColumn("isReactomeFINetwork", Boolean.class, Boolean.FALSE);
	    nodeTable.createColumn("isReactomeFINetwork", Boolean.class, Boolean.FALSE);
	    edgeTable.createColumn("isReactomeFINetwork", Boolean.class, Boolean.FALSE);
	    //May need to iterate over rows to fill columns. fixt it.
	    nodeTable.addVirtualColumn("isReactomeFINetwork", "isReactomeFINEtwork", netTable, "isReactomeFINetwork", Boolean.FALSE);
	    edgeTable.addVirtualColumn("isReactomeFINetwork", "isReactomeFINEtwork", netTable, "isReactomeFINetwork", Boolean.FALSE);
	}
	if (netTable.getColumn(FI_NETWORK_VERSION) == null)
	{
		netTable.createColumn(FI_NETWORK_VERSION, Boolean.class, Boolean.FALSE);
		nodeTable.createColumn(FI_NETWORK_VERSION, Boolean.class, Boolean.FALSE);
		edgeTable.createColumn(FI_NETWORK_VERSION, Boolean.class, Boolean.FALSE);
		nodeTable.addVirtualColumn(FI_NETWORK_VERSION, FI_NETWORK_VERSION, netTable, FI_NETWORK_VERSION, Boolean.FALSE);
		edgeTable.addVirtualColumn(FI_NETWORK_VERSION, FI_NETWORK_VERSION, netTable, FI_NETWORK_VERSION, Boolean.FALSE);
		
	}
	if (netTable.getColumn("DataSetType") == null)
	{
	    	netTable.createColumn("DataSetType", String.class, Boolean.FALSE);
	    	nodeTable.createColumn("DataSetType", String.class, Boolean.FALSE);
		edgeTable.createColumn("DataSetType", String.class, Boolean.FALSE);
		nodeTable.addVirtualColumn("DataSetType", "DataSetType", netTable, "DataSetType", Boolean.FALSE);
		edgeTable.addVirtualColumn("DataSetType", "DataSetType", netTable, "DataSetType", Boolean.FALSE);
	}
	//Check the following with Guanming. fix it.
	if (netTable.getColumn("moduleToSampleValue") == null);
	{
		netTable.createColumn("moduleToSampleValue", Double.class, Boolean.FALSE);
		nodeTable.createColumn("moduleToSampleValue", Double.class, Boolean.FALSE);
		edgeTable.createColumn("moduleToSampleValue", Double.class, Boolean.FALSE);
		nodeTable.addVirtualColumn("moduleToSampleValue", "moduleToSampleValue", netTable, "moduleToSampleValue", Boolean.FALSE);
		edgeTable.addVirtualColumn("moduleToSampleValue", "moduleToSampleValue", netTable, "moduleToSampleValue", Boolean.FALSE);
	}
	if (netTable.getColumn("Clustering_Type") == null)
	{
	    	netTable.createColumn("Clustering_Type", String.class, Boolean.FALSE);
	    	nodeTable.createColumn("Clustering_Type", String.class, Boolean.FALSE);
		edgeTable.createColumn("Clustering_Type", String.class, Boolean.FALSE);
		nodeTable.addVirtualColumn("Clustering_Type", "Clustering_Type", netTable, "Clustering_Type", Boolean.FALSE);
		edgeTable.addVirtualColumn("Clustering_Type", "Clustering_Type", netTable, "Clustering_Type", Boolean.FALSE);
	}
	if (netTable.getColumn("IsLinker") == null)
	{
	    	netTable.createColumn("IsLinker", Boolean.class, Boolean.FALSE);
	    	nodeTable.createColumn("IsLinker", Boolean.class, Boolean.FALSE);
		edgeTable.createColumn("IsLinker", Boolean.class, Boolean.FALSE);
		nodeTable.addVirtualColumn("IsLinker", "IsLinker", netTable, "IsLinker", Boolean.FALSE);
		edgeTable.addVirtualColumn("IsLinker", "IsLinker", netTable, "IsLinker", Boolean.FALSE);
	}


	
    }

    public void handleEvent(NetworkAddedEvent e)
    {
	//Check is GSMA or Microarray and make table accordingly
	makeTableGSMA(e.getNetwork());
	
    }


}
