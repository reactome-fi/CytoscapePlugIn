package org.reactome.cytoscape3;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;

public interface TableFormatter
{

    public abstract void makeGeneSetMutationAnalysisTables(CyNetwork network);

    public abstract void makeModuleAnalysisTables(CyNetwork network);

    public abstract void makeHotNetAnalysisTables(CyNetwork network);

    public abstract void makeEnrichmentTables(CyNetwork network, CyTable table);

    public abstract void makeNetPathEnrichmentTables(CyNetwork network);

    public abstract void makeNetCellComponentTables(CyNetwork network);

    public abstract void makeNetBiologicalProcessTable(CyNetwork network);

    public abstract void makeNetMolecularFunctionTables(CyNetwork network);

}