package org.reactome.cytoscape.service;

/**
 * This class creates the necessary CyTable columns
 * in the default tables and formats their homologs
 * across node, edge, and network tables if needed.
 * It also stores static fields for certain CyTable
 * value/column names and creates custom tables for
 * various app functions.
 * @author Eric T Dawson
 * @date July 2013 
 */
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.task.edit.MapTableToNetworkTablesTaskFactory;

public class TableFormatterImpl implements TableFormatter
{

    /**
     * This class contains methods for creating the necessary columns for
     * FI analysis in the default CyTables and instantiating custom CyTables
     * for clustering and other sorts of analyses.
     * @author Eric T Dawson
     */
    //Strings important to multiple classes in the package are cached here.
    private static final String FI_NETWORK_VERSION = "Reactome_FI_Network_Version";
    private static final String SAMPLE_MUTATION_DATA = "SAMPLE_MUTATION_DATA";
    private static final String MCL_ARRAY_DATA = "MCL_ARRAY_DATA";
    private static final String MCL_ARRAY_CLUSTERING = "MCL_ARRAY_CLUSTERING";
    private static final String HOTNET_MODULE = "HOTNET_MODULE";
    private static final String SPECTRAL_PARTITION_CLUSTER = "SPECTRAL_PARTITION_CLUSTERING";
    private CyTableFactory tableFactory;
    private CyTableManager tableManager;
    private CyNetworkTableManager networkTableManager;
    private MapTableToNetworkTablesTaskFactory mapNetworkAttrTF;

    // private final String [] COLUMNS = new String [7];
    // COLUMNS = ["network", "isReactomeFINetwork", FI_NETWORK_VERSION,
    // "DataSetType", "moduleToSampleValue", "Clustering_Type", "isLinker"];

    public TableFormatterImpl(CyTableFactory tableFactory, CyTableManager tableManager, CyNetworkTableManager networkTableManager, MapTableToNetworkTablesTaskFactory mapNetworkAttrTF)
    {
        this.tableFactory = tableFactory;
        this.tableManager = tableManager;
        this.networkTableManager = networkTableManager;
        this.mapNetworkAttrTF = mapNetworkAttrTF;
    }

    public static String getHotNetModule()
    {
        return HOTNET_MODULE;
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

    private void makeLinkerGenesColumn(CyNetwork network)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        if (nodeTable.getColumn("isLinker") == null)
        {
            nodeTable.createColumn("isLinker", Boolean.class, Boolean.FALSE);
        }

    }
    private void makeSamplesColumn(CyNetwork network)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        if (nodeTable.getColumn("samples") == null)
        {
            nodeTable.createColumn("samples", String.class, Boolean.FALSE);
        }
    }
    private void makeSampleNumberColumn(CyNetwork network)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        if (nodeTable.getColumn("sampleNumber") == null)
        {
            nodeTable
            .createColumn("sampleNumber", Integer.class, Boolean.FALSE);
        }
    }
    private void makeNodeTypeColumn(CyNetwork network)
    {
        CyTable nodeTable = network.getDefaultNodeTable();
        if (nodeTable.getColumn("nodeType") == null)
        {
            nodeTable.createColumn("nodeType", String.class, Boolean.FALSE);
        }
    }
    public void makeBasicTableColumns(CyNetwork network)
    {
        CyTable netTable = network.getDefaultNetworkTable();
        CyTable nodeTable = network.getDefaultNodeTable();
        CyTable edgeTable = network.getDefaultEdgeTable();
        //netTable.setSavePolicy(SavePolicy.SESSION_FILE);
        //nodeTable.setSavePolicy(SavePolicy.SESSION_FILE);
        //edgeTable.setSavePolicy(SavePolicy.SESSION_FILE);
        
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
        
        if (netTable.getColumn("clustering_Type") == null)
        {
            netTable.createColumn("clustering_Type", String.class,
                    Boolean.FALSE);
        }
        if (netTable.getColumn("name") == null)
        {
            netTable.createColumn("name", String.class,
                    Boolean.FALSE);
        }
        if (edgeTable.getColumn("name") == null)
        {
            edgeTable.createColumn("name", String.class, Boolean.FALSE);
        }
        if (edgeTable.getColumn("EDGE_TYPE") == null)
        {
            edgeTable.createColumn("EDGE_TYPE", String.class, Boolean.FALSE);
        }
        if (edgeTable.getColumn("FI Annotation") == null)
        {
            edgeTable.createColumn("FI Annotation", String.class, Boolean.FALSE);
        }
        if (edgeTable.getColumn("FI Direction") == null)
        {
            edgeTable.createColumn("FI Direction", String.class, Boolean.FALSE);
        }
        if (edgeTable.getColumn("FI Score") == null)
        {
            edgeTable.createColumn("FI Score", Double.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("module") == null)
        {
            nodeTable.createColumn("module", Integer.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("commonName") == null)
        {
            nodeTable.createColumn("commonName", String.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("name") == null)
        {
            nodeTable.createColumn("name", String.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("nodeToolTip") == null)
        {
            nodeTable.createColumn("nodeToolTip", String.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("nodeLabel") == null)
        {
            nodeTable.createColumn("nodeLabel", String.class, Boolean.FALSE);
        }
        if (nodeTable.getColumn("nodeType") == null)
        {
            nodeTable.createColumn("nodeType", String.class, Boolean.FALSE);
        }
    }
    /**
     * Creates the necessary columns in the default tables for Gene Set / Mutation Analysis
     * @param network The network created from the input file by querying the FI database.
     */
    @Override
    public void makeGeneSetMutationAnalysisTables(CyNetwork network)
    {
        //Make the basic network attribute table
        makeBasicTableColumns(network);
        //Make the columns for Sample number, Linker Genes, and Samples.
        //Linker genes and samples are unique to Gene Set/Mutation Analysis.
        makeSampleNumberColumn(network);
        makeLinkerGenesColumn(network);
        makeSamplesColumn(network);
    }

    /**
     * Creates the necessary columns for network clustering in the default node table,
     * the default network table, and in the newly created module table.
     * @param network The network model of a given network view.
     */
    @Override
    public void makeModuleAnalysisTables(CyNetwork network)
    {

        //Creates the attributes tables for Module Analysis (view context menu)
        CyTable netTable = network.getDefaultNetworkTable();
        CyTable nodeTable = network.getDefaultNodeTable();
        if (netTable.getColumn("clustering_Type") == null)
        {
            netTable.createColumn("clustering_Type", String.class,
                    Boolean.FALSE);
        }
        if (nodeTable.getColumn("module") == null)
        {
            nodeTable.createColumn("module", Integer.class, Boolean.FALSE);
        }
        boolean found = false;
        CyTable moduleTable = null;
        Set<CyTable> tables = tableManager.getAllTables(Boolean.TRUE);
        for (CyTable table : tables)
        {
            if (table.getTitle().equals("Network Modules"))
            {
                moduleTable = table;
                found = true;
                break;
            }
        }
        if (found == false)
        {
            moduleTable = tableFactory.createTable("Network Modules",
                    "module", Integer.class, Boolean.FALSE, Boolean.FALSE);
            tableManager.addTable(moduleTable);
            if (moduleTable.getColumn("module") == null)
            {
                moduleTable.createColumn("module", Integer.class, Boolean.FALSE);
            }
            if (moduleTable.getColumn("Nodes in Module") == null)
            {
                moduleTable.createColumn("Nodes in Module", Integer.class,
                        Boolean.FALSE);
            }
            if (moduleTable.getColumn("Node Percentage") == null)
            {
                moduleTable.createColumn("Node Percentage", Integer.class,
                        Boolean.FALSE);
            }
            if (moduleTable.getColumn("Samples in Module") == null)
            {
                moduleTable.createColumn("Samples in Module", Integer.class, Boolean.FALSE);
            }
            if (moduleTable.getColumn("Sample Percentage") == null)
            {
                moduleTable.createColumn("Sample Percentage", Double.class, Boolean.FALSE);
            }
            if (moduleTable.getColumn("Node List") == null)
            {
                moduleTable.createColumn("Node List", String.class, Boolean.FALSE);
            }
            tableManager.addTable(moduleTable);
            //mapNetworkAttrTF.createTaskIterator(moduleTable);
            networkTableManager.setTable(network, CyNetwork.class, "Modules", moduleTable);
        }
        
    }
    /**
     * Creates the columns for HotNet analysis in the default node table and in the HotNet module-linked table.
     * @param network The network generated from the provided file.
     */
    @Override
    public void makeHotNetAnalysisTables(CyNetwork network)
    {
        //Make the basic attribute tables and the table for Samples.
        makeBasicTableColumns(network);
        makeSamplesColumn(network);
        //Creates the attribute table specifically for HotNet Analysis data
        boolean found = false;
        Set<CyTable> tables = tableManager.getAllTables(Boolean.TRUE);
        for (CyTable table : tables)
        {
            if (table.getTitle().equals("HotNet Module Browser"))
            {
                found = true;
                break;
            }
        }
        if (!found)
        {
            CyTable hotNetTable = tableFactory.createTable("HotNet Module", "module", Integer.class, Boolean.FALSE, Boolean.FALSE);
            if (hotNetTable.getColumn("module") == null)
            {
                hotNetTable.createColumn("module", Integer.class, Boolean.FALSE);
            }
            if (hotNetTable.getColumn("Nodes in Module") == null)
            {
                hotNetTable.createColumn("Nodes in Module", Integer.class, Boolean.FALSE);
            }
            if (hotNetTable.getColumn("Node Percentage") == null)
            {
                hotNetTable.createColumn("Node Percentage", Integer.class,
                        Boolean.FALSE);
            }
            if (hotNetTable.getColumn("Samples in Module") == null)
            {
                hotNetTable.createColumn("Samples in Module", Integer.class, Boolean.FALSE);
            }
            if (hotNetTable.getColumn("Sample Percentage") == null)
            {
                hotNetTable.createColumn("Sample Percentage", Double.class, Boolean.FALSE);
            }
            if (hotNetTable.getColumn("pvalue") == null)
            {
                hotNetTable.createColumn("pvalue", Double.class, Boolean.FALSE);
            }
            if (hotNetTable.getColumn("FDR") == null)
            {
                hotNetTable.createColumn("FDR", Double.class, Boolean.FALSE);
            }
            if (hotNetTable.getColumn("Node List") == null)
            {
                hotNetTable.createColumn("Node List", String.class, Boolean.FALSE);
            }
            tableManager.addTable(hotNetTable);
        }
    }
    public void makeMicroarrayAnalysisTable(CyNetwork network)
    {
        makeBasicTableColumns(network);
        makeNodeTypeColumn(network);
        boolean found = false;
        Set<CyTable> tables = tableManager.getAllTables(Boolean.TRUE);
        for (CyTable table : tables)
        {
            if (table.getTitle().equals("MCL Module Browser"))
            {
                found = true;
                break;
            }
        }
        if (!found)
        {
            CyTable mclTable = tableFactory.createTable("MCL Module Browser", "module", Integer.class, Boolean.FALSE, Boolean.TRUE);
            if (mclTable.getColumn("module") == null)
            {
                mclTable.createColumn("module", Integer.class, Boolean.FALSE);
            }
            if (mclTable.getColumn("nodes in Module") == null)
            {
                mclTable.createColumn("nodes in Module", Integer.class, Boolean.FALSE);
            }
            if (mclTable.getColumn("node Percentage") == null)
            {
                mclTable.createColumn("node Percentage", Double.class, Boolean.FALSE);
            }
            if (mclTable.getColumn("average Correlation") == null)
            {
                mclTable.createColumn("average Correlation", Double.class, Boolean.FALSE);
            }
            if (mclTable.getColumn("node List") == null)
            {
                mclTable.createColumn("node List", String.class, Boolean.FALSE);
            }
        }

    }
    /**
     * Creates the columns in a given table for a specific type of enrichment/GO analysis.
     * @param network The network being analyzed
     * @param table The table for the analysis to be performed.
     */
    @Override
    public void makeEnrichmentTables(CyNetwork network, CyTable table)
    {

        if (table.getColumn("Gene Set") == null)
        {
            table.createColumn("Gene Set", String.class, Boolean.FALSE);
        }
        if (table.getColumn("Ratio of Protein in Gene Set") == null)
        {
            table.createColumn("Ratio of Protein in Gene Set", Double.class, Boolean.FALSE);
        }
        if (table.getColumn("Number of Protein in Gene Set") == null)
        {
            table.createColumn("Number of Protein in Gene Set", Integer.class, Boolean.FALSE);
        }
        if (table.getColumn("Protein from Network") == null)
        {
            table.createColumn("Protein from Network", Integer.class, Boolean.FALSE);
        }
        if (table.getColumn("P-Value") == null)
        {
            table.createColumn("P-Value", Double.class, Boolean.FALSE);
        }
        if (table.getColumn("FDR") == null)
        {
            table.createColumn("FDR", Double.class, Boolean.FALSE);
        }
        if (table.getColumn("Nodes") == null)
        {
            table.createColumn("Nodes", String.class, Boolean.FALSE);
        }
    }
    /**
     * Creates the pathways needed for network pathway enrichment analysis
     * @param network The FI network to be analyzed.
     */
    @Override
    public void makeNetPathEnrichmentTables(CyNetwork network)
    {
        CyTable netPathEnrichmentTable = tableFactory.createTable("Pathways in Network", "Gene Set", String.class, Boolean.FALSE, Boolean.FALSE);
        makeEnrichmentTables(network, netPathEnrichmentTable);
        tableManager.addTable(netPathEnrichmentTable);
    }
    /**
     * Creates the table necessary for network cellular component analysis.
     * @param network The FI network for analysis.
     */
    @Override
    public void makeNetCellComponentTables(CyNetwork network)
    {
        CyTable netCellComponentTable = tableFactory.createTable("GO CC in Network", "Gene Set", String.class, Boolean.FALSE, Boolean.FALSE);
        makeEnrichmentTables(network, netCellComponentTable);
        tableManager.addTable(netCellComponentTable);
    }
    /**
     * Creates the table and necessary columns for analyzing network GO biological processes.
     * @param network the network being analyzed.
     */
    @Override
    public void makeNetBiologicalProcessTable(CyNetwork network)
    {
        CyTable netBiologicalProcessTable = tableFactory.createTable("GO BP in Network", "Gene Set", String.class, Boolean.FALSE, Boolean.FALSE);
        makeEnrichmentTables(network, netBiologicalProcessTable);
        tableManager.addTable(netBiologicalProcessTable);
    }
    /**
     * Creates the Necessary table and columns for network GO molecular function.
     * @param network the CyNetwork being analyzed.
     */
    @Override
    public void makeNetMolecularFunctionTables(CyNetwork network)
    {
        CyTable netMolecularFunctionTable = tableFactory.createTable("GO MF in Network", "Gene Set", String.class, Boolean.FALSE, Boolean.FALSE);
        netMolecularFunctionTable.setTitle("GO MF in Network");
        makeEnrichmentTables(network, netMolecularFunctionTable);
        tableManager.addTable(netMolecularFunctionTable);
    }

    @Override
    public void makeModulePathwayAnalysisTables(CyNetwork network)
    {
        // TODO Auto-generated method stub
        CyTable table = tableFactory.createTable("ModulePathwayTable", "module", Integer.class, Boolean.FALSE, Boolean.FALSE);
    }

    @Override
    public void makeModuleCellComponentTables(CyNetwork network)
    {
        // TODO Auto-generated method stub
        CyTable table = tableFactory.createTable("ModuleCellTable", "module", Integer.class, Boolean.FALSE, Boolean.FALSE);
    }

    @Override
    public void makeModuleMolecularFunctionTables(CyNetwork network)
    {
        // TODO Auto-generated method stub
        CyTable table = tableFactory.createTable("ModuleMolecularTable", "module", Integer.class, Boolean.FALSE, Boolean.FALSE);
    }

    @Override
    public void makeModuleBiologicalProcessTables(CyNetwork network)
    {
        // TODO Auto-generated method stub
        CyTable table = tableFactory.createTable("ModuleBioTable", "module", Integer.class, Boolean.FALSE, Boolean.FALSE);
    }

    @Override
    public void makeModuleMolecularFunctionTable(CyNetwork network)
    {
        // TODO Auto-generated method stub
        //CyTable table = tableFactory.createTable("Module")
    }

    @Override
    public void makeModuleSurvivalAnalysisTables(CyNetwork network)
    {
        // TODO Auto-generated method stub

    }
}
