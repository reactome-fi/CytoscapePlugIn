package org.reactome.CS.x.internal;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;



public class GSMATaskFactory extends AbstractTaskFactory
{
    private final CyNetworkManager netManager;
    private final TaskManager tm;
    private final SaveSessionAsTaskFactory saveAsFactory;
    private FileUtil fileUtil;
    private CySwingApplication desktopApp;
    public GSMATaskFactory( TaskManager tm ,CyNetworkManager netManager,
	    SaveSessionAsTaskFactory saveAsFactory, FileUtil fileUtil,
	    CySwingApplication desktopApp)
    {
	this.netManager = netManager;
	this.tm = tm;
	this.saveAsFactory=saveAsFactory;
	this.fileUtil = fileUtil;
	this.desktopApp = desktopApp;
    }

    @Override
    public TaskIterator createTaskIterator()
    {
	// TODO Auto-generated method stub
	return new TaskIterator(new GSMATask(tm, netManager, saveAsFactory, fileUtil, desktopApp));
    }

}
