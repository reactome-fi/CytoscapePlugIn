package org.reactome.CS.x.internal;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.task.write.SaveSessionTaskFactory;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.util.ListSingleSelection;


public class GSMATask extends AbstractTask
{
    
    @Tunable (description="Reactome FI needs a fresh session to build the FI network.\n\n"
    		+ " Would you like to save your current session?", groups="Session")
    public boolean saveNewSession = true;
    
    
    @Tunable(description="FI Network Version", groups="FI Network Construction")
    public ListSingleSelection<Integer> FI_NETWORK_VERSION = new ListSingleSelection<Integer> (2012, 2009);
    
    @Tunable(description="File Format", xorChildren=true, groups="File")
    public ListSingleSelection<String> FILE_FORMAT = new ListSingleSelection<String>("Gene Set", "Gene/Sample Number Pair", "NCI MAF");
    
    @Tunable(description="File to analyze", params="fileCategory=unspecified;input=true", groups="File")
    public File dataFile;
    
    @Tunable(description="Sample Cutoff", dependsOn="File Format=NCI MAF", groups="File", params="")
    public int SAMPLE_CUTOFF = 2;
    
    @Tunable(description="Fetch FI Annotations (Warning: Slow)", groups="FI Network Construction")
    public boolean FETCH_FI_ANNOTATIONS = false;
    
    @Tunable(description="Use Linker Genes", groups="FI Network Construction")
    public boolean USE_LINKERS = false;
    
    @Tunable(description="Show genes not linked to others", groups="FI Network Construction")
    public boolean SHOW_UNLINKED_GENES = false;

    private CyNetworkManager netManager;

    private TaskManager tm;
    private CySwingApplication desktopApp;

    private FileUtil fileUtil;

    private SaveSessionAsTaskFactory saveSession;
    
    public GSMATask(TaskManager tm, CyNetworkManager netManager,

	    SaveSessionAsTaskFactory saveSession,
	    FileUtil fileUtil,
	    CySwingApplication desktopApp)
    {
	super();
	this.tm = tm;
	this.netManager = netManager;

	this.saveSession = saveSession;
	this.fileUtil = fileUtil;
	this.desktopApp = desktopApp;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception
    {
	System.out.println(System.currentTimeMillis());
	if (saveNewSession)
	{
	    tm.execute(saveSession.createTaskIterator());
	}
	
//	Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();
//	FileChooserFilter mafFilter = new FileChooserFilter("NCI MAF Files", "maf");
//	filters.add(mafFilter);
//	File dataFile = fileUtil.getFile(desktopApp.getJFrame(), "Please Choose A File", FileUtil.LOAD, filters);
//	if (this.dataFile == null)
//	{
//	    return;
//	}
	
	
    }
    
    
//    public boolean getIsNewSession()
//    {
//	return isNewSession;
//    }
//    public void setIsNewSession(boolean newIsNewSession)
//    {
//	isNewSession = newIsNewSession;
//    }

}
