package org.reactome.CS.x.internal;

import java.util.Arrays;
import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;


public class GSMATask extends AbstractTask
{
    @Tunable(description="FI Network Version")
    public int FI_NETWORK_VERSION = 2012;
    
    @Tunable(description="File Format")
    public List<String> FILE_FORMAT = Arrays.asList("Gene_Set", "Gene_Sample_Number_Pair", "NCI_MAF");
    
    @Tunable(description="Sample Cutoff")
    public int SAMPLE_CUTOFF = 2;
    
    @Tunable(description="Fetch FI Annotations")
    public boolean FETCH_FI_ANNOTATIONS = false;
    
    @Tunable(description="Use Linker Genes")
    public boolean USE_LINKERS = false;
    
    @Tunable(description="Show genes not linked to others")
    public boolean SHOW_UNLINKED_GENES = false;
    
    public GSMATask()
    {
	super();
	// TODO Auto-generated constructor stub
    }

    @Override
    public void run(TaskMonitor arg0) throws Exception
    {
	System.out.println(FI_NETWORK_VERSION);
	
    }

}
