package org.reactome.CS.x.internal;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.task.write.SaveSessionTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {


    	public CyActivator()
    	{
    	    super();
    	}
	public void start(BundleContext context) throws Exception {
	    	CySwingApplication desktopApp = getService(context, CySwingApplication.class);
		CyApplicationManager cyApplicationManager = getService(context, CyApplicationManager.class);
		TaskManager taskManager = getService(context, TaskManager.class);
		CyNetworkManager networkManager = getService(context, CyNetworkManager.class);
		SaveSessionAsTaskFactory saveSessionAsTaskFactory = getService(context, SaveSessionAsTaskFactory.class);
		//FileLoaderTaskFactory fileLoader = getService(context, FileLoaderTaskFactory.class);
		FileUtil fileUtil = getService(context, FileUtil.class);
		
		GSMATaskFactory gsmaTaskFactory = new GSMATaskFactory(taskManager, networkManager,
			saveSessionAsTaskFactory, fileUtil, desktopApp);
		Properties gsmaProps = new Properties();
		gsmaProps.setProperty("preferredMenu", "Apps.ReactomeFIS");
		gsmaProps.setProperty("menuGravity", "1.0");
		gsmaProps.setProperty("title", "Gene Set/Mutation Analysis");
		Properties properties = new Properties();
		
		registerAllServices(context, gsmaTaskFactory, gsmaProps);
	}

}
