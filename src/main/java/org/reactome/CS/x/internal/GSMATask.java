package org.reactome.CS.x.internal;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySession;
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
    private CySessionManager sessionManager;
    
    public GSMATask(TaskManager tm, CyNetworkManager netManager,

	    SaveSessionAsTaskFactory saveSession,
	    FileUtil fileUtil,
	    CySwingApplication desktopApp,
	    CySessionManager sessionManager)
    {
	super();
	this.tm = tm;
	this.netManager = netManager;

	this.saveSession = saveSession;
	this.fileUtil = fileUtil;
	this.desktopApp = desktopApp;
	this.sessionManager = sessionManager;
    }
//Use an appropriate reader manager to get contents of file
    //CyNetworkReaderManager
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception
    {
	
	HttpClient client = new HttpClient();
	GetMethod method = new GetMethod("http://www.google.ca");

	int statusCode = client.executeMethod(method);
	System.out.println(statusCode);
	System.out.println(System.currentTimeMillis());
	if (createNewSession(netManager, sessionManager))
	{
	    System.out.println("Session saved");
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
    
    protected boolean createNewSession(CyNetworkManager networkManager, CySessionManager sessionManager)
    {
	int networkCount = networkManager.getNetworkSet().size();
		if (networkCount == 0)
		    return true;
	String msg = new String( "A new session is needed for using Reactome FI plugin.\n"
		     + "Do you want to save your session?");
	int reply = JOptionPane.showConfirmDialog(this.desktopApp.getJFrame(),
		msg, "Save Session?", JOptionPane.YES_NO_CANCEL_OPTION);
	if (reply == JOptionPane.CANCEL_OPTION)
	    return false;
	else if (reply == JOptionPane.NO_OPTION)
	{
	    CySession.Builder builder = new CySession.Builder();
	    sessionManager.setCurrentSession(builder.build(), null);
	}
	else
	{
	    tm.execute(saveSession.createTaskIterator());
	    if (sessionManager.getCurrentSession() == null)
		return true;
	    CySession.Builder builder = new CySession.Builder();
	    sessionManager.setCurrentSession(builder.build(), null);
	}
	return true;
	    
    }


}
