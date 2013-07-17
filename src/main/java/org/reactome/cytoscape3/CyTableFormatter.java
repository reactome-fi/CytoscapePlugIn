package org.reactome.cytoscape3;

/**
 * This class creates the necessary CyTable columns
 * in the default tables and formats their homologs
 * across node, edge, and network tables if needed.
 * It also stores static fields for certain CyTable
 * value/column names.
 * @author Eric T Dawson
 * @date July 2013 
 */
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;

public class CyTableFormatter
{

    //
    private static final String FI_NETWORK_VERSION = "Reactome_FI_Network_Version";
    private static final String SAMPLE_MUTATION_DATA = "SAMPLE_MUTATION_DATA";
    private static final String MCL_ARRAY_DATA = "MCL_ARRAY_DATA";
    private static final String MCL_ARRAY_CLUSTERING = "MCL_ARRAY_CLUSTERING";
    private static final String HOTNET_MODULE = "HOTNET_MODULE";
    private static final String SPECTRAL_PARTITION_CLUSTER = "SPECTRAL_PARTITION_CLUSTERING";
    private CyTableFactory tableFactory;

    // private final String [] COLUMNS = new String [7];
    // COLUMNS = ["network", "isReactomeFINetwork", FI_NETWORK_VERSION,
    // "DataSetType", "moduleToSampleValue", "Clustering_Type", "isLinker"];

    public CyTableFormatter(CyTableFactory tableFactory)
    {
        this.tableFactory = tableFactory;
    }

    public static String getFINetworkVersion()
    {
        return FI_NETWORK_VERSION;
    }

    public static String getSampleMutationData()
    {
        return SAMPLE_MUTATION_DATA;
    }

    public static String getMCLArrayClustering()
    {
        return MCL_ARRAY_DATA;
    }

    public static String getSpectralPartitionCluster()
    {
        return SPECTRAL_PARTITION_CLUSTER;
    }

    public void makeAllTablesGSMA(CyNetwork network)
    {

        CyTable netTable = network.getDefaultNetworkTable();
        CyTable nodeTable = network.getDefaultNodeTable();
        CyTable edgeTable = network.getDefaultEdgeTable();

        // From Jason's email. Make sure that the network SUID ends up in the
        // properly linked table
        // in the edge/node table to ensure that network properties are
        // distributed to nodes and edges.
        // if (nodeTable.getColumn("network") == null)
        // {
        // //nodeTable.createColumn("network", Long.class, true);
        // //edgeTable.createColumn("network", Long.class, true);
        // //nodeTable.addVirtualColumn("network.SUID", "SUID", netTable,
        // "network.SUID", true);
        // //edgeTable.addVirtualColumn("network.SUID", "SUID", netTable,
        // "network.SUID", true);
        // //makeAllTablesGSMA(network);
        // }
        // Creates a set of columns in the default network table and creates
        // matching virtual columns
        // within the default edge and node tables upon network creation or
        // network view creation.
        if (netTable.getColumn("isReactomeFINetwork") == null)
        {
            netTable.createColumn("isReactomeFINetwork", Boolean.class,
                    Boolean.FALSE);
        }
        if (netTable.getColumn(FI_NETWORK_VERSION) == null)
        {
            netTable.createColumn(FI_NETWORK_VERSION, String.class,
                    Boolean.FALSE);
        }
        if (netTable.getColumn("dataSetType") == null)
        {
            netTable.createColumn("dataSetType", String.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("module") == null)
        {
            ;
        }
        {
            // netTable.createColumn("moduleToSampleValue", Double.class,
            // Boolean.FALSE);
            nodeTable.createColumn("module", Double.class, Boolean.FALSE);
        }
        if (netTable.getColumn("clustering_Type") == null)
        {
            netTable.createColumn("clustering_Type", String.class,
                    Boolean.FALSE);
        }
        if (nodeTable.getColumn("samples") == null)
        {
            nodeTable.createColumn("samples", String.class, Boolean.FALSE);
        }

        if (nodeTable.getColumn("isLinker") == null)
        {
            nodeTable.createColumn("isLinker", Boolean.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("nodeLabel") == null)
        {
            nodeTable.createColumn("nodeLabel", String.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("sampleNumber") == null)
        {
            nodeTable
                    .createColumn("sampleNumber", Integer.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("commonName") == null)
        {
            nodeTable.createColumn("commonName", String.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("nodeToolTip") == null)
        {
            nodeTable.createColumn("nodeToolTip", String.class, Boolean.FALSE);
        }
        if (edgeTable.getColumn("name") == null)
        {
            edgeTable.createColumn("name", String.class, Boolean.FALSE);
        }

    }

    public void makeModuleAnalysisColumns(CyNetwork network)
    {
        CyTable netTable = network.getDefaultNetworkTable();
        CyTable nodeTable = network.getDefaultNodeTable();
        if (netTable.getColumn("clusteringType") == null)
        {
            netTable.createColumn("clusteringType", Integer.class,
                    Boolean.FALSE);
        }
        if (nodeTable.getColumn("module") == null)
        {
            nodeTable.createColumn("module", Integer.class, Boolean.FALSE);
        }
        CyTable moduleTable = tableFactory.createTable("Network Module",
                "module", Integer.class, Boolean.TRUE, Boolean.FALSE);
        if (moduleTable.getColumn("nodes in module") == null)
        {
            moduleTable.createColumn("nodes in module", Integer.class,
                    Boolean.FALSE);
        }
        if (moduleTable.getColumn("node percentage") == null)
        {
            moduleTable.createColumn("node percentage", Integer.class,
                    Boolean.FALSE);
        }

    }
}
