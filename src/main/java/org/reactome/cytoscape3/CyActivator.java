package org.reactome.cytoscape3;

import java.util.Properties;




//import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.AddedNodesListener;
//import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;

import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {


    	public CyActivator()
    	{
    	    super();
    	}
	public void start(BundleContext context) throws Exception {
	    
	    	CySwingApplication desktopApp = getService(context, CySwingApplication.class);
		//CyApplicationManager cyApplicationManager = getService(context, CyApplicationManager.class);
		TaskManager taskManager = getService(context, TaskManager.class);
		CyNetworkManager networkManager = getService(context, CyNetworkManager.class);
		CySessionManager sessionManager = getService(context, CySessionManager.class);
		CyNetworkFactory networkFactory = getService(context, CyNetworkFactory.class);
		CyNetworkViewFactory viewFactory = getService(context, CyNetworkViewFactory.class);
		CyNetworkViewManager viewManager = getService(context, CyNetworkViewManager.class);
		SaveSessionAsTaskFactory saveSessionAsTaskFactory = getService(context, SaveSessionAsTaskFactory.class);

		FileUtil fileUtil = getService(context, FileUtil.class);
		
		OpenBrowser browser = getService(context, OpenBrowser.class);

		GeneSetMutatationAnalysisAction gsma = new GeneSetMutatationAnalysisAction(taskManager, networkManager,
			saveSessionAsTaskFactory, fileUtil, desktopApp, sessionManager,
			networkFactory, viewFactory, viewManager);
		UserGuideAction uga = new UserGuideAction(desktopApp, browser);
		
		CyTableFormatterListener cyTableFormatter = new CyTableFormatterListener();



		registerAllServices(context, gsma, new Properties());
		registerAllServices(context, uga, new Properties());
		registerService(context, cyTableFormatter, AddedNodesListener.class, new Properties());
	}

}
