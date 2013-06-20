package org.reactome.CS.x.internal;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.task.write.SaveSessionTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;



public class GSMATaskFactory extends AbstractTaskFactory
{
    private final CyNetworkManager netManager;
    private final TaskManager tm;
    private FileUtil fileUtil;
    private CySwingApplication desktopApp;
    private SaveSessionAsTaskFactory saveSession;
    private CySessionManager sessionManager;
    public GSMATaskFactory( TaskManager tm ,CyNetworkManager netManager,
	    SaveSessionAsTaskFactory saveSession,
	    FileUtil fileUtil,
	    CySwingApplication desktopApp,
	    CySessionManager sessionManager)
    {
	this.netManager = netManager;
	this.tm = tm;

	this.saveSession = saveSession;
	this.fileUtil = fileUtil;
	this.desktopApp = desktopApp;
	this.sessionManager = sessionManager;
    }

    @Override
    public TaskIterator createTaskIterator()
    {
	// TODO Auto-generated method stub
	return new TaskIterator(new GSMATask(tm, netManager,
		saveSession, fileUtil, desktopApp, sessionManager));
    }

}
