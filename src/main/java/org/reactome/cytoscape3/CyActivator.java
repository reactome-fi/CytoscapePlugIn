package org.reactome.cytoscape3;
/**
 * This is the main entry point for
 * the Reactome FI app. In OSGi parlance,
 * it is a bundle activator. For more info on
 * OSGi, check out Richard Hall's OSGi in action
 * and the OSGi R4 specs.
 * @author Eric T Dawson, July 2013
 */
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

    public BundleContext context;
    	public CyActivator()
    	{
    	    super();
    	}
	public void start(BundleContext context) throws Exception {
	    this.context = context;
	    //Grab essential Cytoscape Service References
	    CySwingApplication desktopApp = getService(context, CySwingApplication.class);
		TaskManager taskManager = getService(context, TaskManager.class);
		CyNetworkManager networkManager = getService(context, CyNetworkManager.class);
		CySessionManager sessionManager = getService(context, CySessionManager.class);
		CyNetworkFactory networkFactory = getService(context, CyNetworkFactory.class);
		CyNetworkViewFactory viewFactory = getService(context, CyNetworkViewFactory.class);
		CyNetworkViewManager viewManager = getService(context, CyNetworkViewManager.class);
		SaveSessionAsTaskFactory saveSessionAsTaskFactory = getService(context, SaveSessionAsTaskFactory.class);
		FileUtil fileUtil = getService(context, FileUtil.class);
		OpenBrowser browser = getService(context, OpenBrowser.class);

		//Instantiate Reactome FI App services
		GeneSetMutatationAnalysisAction gsma = new GeneSetMutatationAnalysisAction(taskManager, networkManager,
			saveSessionAsTaskFactory, fileUtil, desktopApp, sessionManager,
			networkFactory, viewFactory, viewManager);
		UserGuideAction uga = new UserGuideAction(desktopApp, browser);
		



		//Register said Reactome FI Services with the OSGi framework.
		registerAllServices(context, gsma, new Properties());
		registerAllServices(context, uga, new Properties());
	}

}
