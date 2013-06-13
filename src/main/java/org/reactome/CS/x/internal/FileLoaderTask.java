package org.reactome.CS.x.internal;

import java.util.HashSet;

import org.cytoscape.io.DataCategory;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;

public class FileLoaderTask extends AbstractTask
{
    private HashSet<String> EXTENSIONS = new HashSet<String>();
    private HashSet<String> CONTENT_TYPES = new HashSet<String>();
    String DESCRIPTION = "";
    DataCategory category = DataCategory.NETWORK;
    public FileLoaderTask(String format)
    {
	if (format.equals("NCI MAF"))
	{
	    this.EXTENSIONS.add("maf");
	    this.CONTENT_TYPES.add("txt");
	    this.DESCRIPTION = "Filter for reading in NCI MAF files.";
	    
	}
	if (format.equals("Gene/Sample Number Pair"))
	{
	    
	}
	if (format.equals("Gene Set"))
	{
	    
	}
    }

    @Override
    public void run(TaskMonitor arg0) throws Exception
    {
	// TODO Auto-generated method stub
	
    }

}
