package org.reactome.cytoscape3;
/**
 * This class creates the necessary CyTable columns
 * in the default tables and formats their homologs
 * across node, edge, and network tables if needed.
 * @author Eric T Dawson
 * @date July 2013 
 */
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;

public class CyTableFormatter
{

    // Key for storing FI network version
    private final String FI_NETWORK_VERSION = "Reactome_FI_Network_Version";
//    private final String [] COLUMNS = new String [7];
//    COLUMNS = ["network", "isReactomeFINetwork", FI_NETWORK_VERSION, "DataSetType", "moduleToSampleValue", "Clustering_Type", "isLinker"];
    
    public CyTableFormatter()
    {
    }
    public void makeAllTablesGSMA(CyNetwork network)
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
            //nodeTable.addVirtualColumn("network.SUID", "SUID", netTable, "network.SUID", true);
            //edgeTable.addVirtualColumn("network.SUID", "SUID", netTable, "network.SUID", true);
            //makeAllTablesGSMA(network);
        }
        //Creates a set of columns in the default network table and creates matching virtual columns
        //within the default edge and node tables upon network creation or network view creation.
        if (netTable.getColumn("isReactomeFINetwork") == null)
        {
            netTable.createColumn("isReactomeFINetwork", Boolean.class, Boolean.FALSE);
        }
        if (netTable.getColumn(FI_NETWORK_VERSION) == null)
        {
            netTable.createColumn(FI_NETWORK_VERSION, Boolean.class, Boolean.FALSE);
            //makeAllTablesGSMA(network);
        }
        if (netTable.getColumn("DataSetType") == null)
        {
            netTable.createColumn("DataSetType", String.class, Boolean.FALSE);
            //makeAllTablesGSMA(network);
        }
        //Check the following with Guanming. fix it.
        if (netTable.getColumn("moduleToSampleValue") == null);
        {
//            netTable.createColumn("moduleToSampleValue", Double.class, Boolean.FALSE);
//            nodeTable.createColumn("moduleToSampleValue", Double.class, Boolean.FALSE);
//            edgeTable.createColumn("moduleToSampleValue", Double.class, Boolean.FALSE);
//            nodeTable.addVirtualColumn("moduleToSampleValue", "moduleToSampleValue", netTable, "moduleToSampleValue", Boolean.FALSE);
//            edgeTable.addVirtualColumn("moduleToSampleValue", "moduleToSampleValue", netTable, "moduleToSampleValue", Boolean.FALSE);
            //makeAllTablesGSMA(network);
        }
        if (netTable.getColumn("Clustering_Type") == null)
        {
            netTable.createColumn("Clustering_Type", String.class, Boolean.FALSE);
        }


        if (nodeTable.getColumn("IsLinker") == null)
        {

            nodeTable.createColumn("IsLinker", Boolean.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("nodeLabel") == null)
        {
            nodeTable.createColumn("nodeLabel", String.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("sampleNumber") == null)
        {
            nodeTable.createColumn("sampleNumber", String.class, Boolean.FALSE);
            //makeAllTablesGSMA(network);
        }
        if (nodeTable.getColumn("commonName") == null)
        {
            nodeTable.createColumn("commonName", String.class, Boolean.FALSE);
            //makeAllTablesGSMA(network);
        }
        if (nodeTable.getColumn("nodeToolTip") == null)
        {
            nodeTable.createColumn("nodeToolTip", String.class, Boolean.FALSE);
            //makeAllTablesGSMA(network);
        }
	
    }
}
