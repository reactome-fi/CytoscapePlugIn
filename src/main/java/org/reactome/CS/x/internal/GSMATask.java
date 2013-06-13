package org.reactome.CS.x.internal;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;


public class GSMATask extends AbstractTask
{
    @Tunable(description="FI Network Version")
    public int FI_NETWORK_VERSION = 2012;
    
    @Tunable(description="File Format")
    public ListSingleSelection<String> FILE_FORMAT = new ListSingleSelection<String>("Gene Set", "Gene/Sample Number Pair", "NCI MAF (Mutation Annotation File");
    
    @Tunable(description="File to analyze")
    public File dataFile;
    
    @Tunable(description="Sample Cutoff")
    public int SAMPLE_CUTOFF = 2;
    
    @Tunable(description="Fetch FI Annotations", groups="FI Network Construction Parameters")
    public boolean FETCH_FI_ANNOTATIONS = false;
    
    @Tunable(description="Use Linker Genes", groups="FI Network Construction Parameters")
    public boolean USE_LINKERS = false;
    
    @Tunable(description="Show genes not linked to others", groups="FI Network Construction Parameters")
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
