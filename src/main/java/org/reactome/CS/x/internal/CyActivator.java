package org.reactome.CS.x.internal;

import java.util.Properties;




import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CySessionManager;

import org.cytoscape.task.write.SaveSessionAsTaskFactory;

import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.work.TaskManager;
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
		
		SaveSessionAsTaskFactory saveSessionAsTaskFactory = getService(context, SaveSessionAsTaskFactory.class);

		FileUtil fileUtil = getService(context, FileUtil.class);
		
		OpenBrowser browser = getService(context, OpenBrowser.class);
//		HttpClient httpClient = getService(context, HttpClient.class);
		


		
		GSMATaskFactory gsmaTaskFactory = new GSMATaskFactory(taskManager, networkManager,
			saveSessionAsTaskFactory, fileUtil, desktopApp, sessionManager);
		Properties properties = new Properties();
		Properties gsmaProps = new Properties();
		gsmaProps.setProperty("preferredMenu", "Apps.ReactomeFI");
		gsmaProps.setProperty("menuGravity", "1.0");
		gsmaProps.setProperty("title", "Gene Set/Mutation Analysis");
		
		UserGuideTaskFactory userGuide = new UserGuideTaskFactory(desktopApp, browser);
		Properties userGuideProps = new Properties();
		userGuideProps.setProperty("title", "User Guide");
		userGuideProps.setProperty("menuGravity", "3.0");
		userGuideProps.setProperty("preferredMenu", "Apps.ReactomeFI");
		
		CyTableFormatterListener cyTableFormatter = new CyTableFormatterListener();
		


		registerAllServices(context, gsmaTaskFactory, gsmaProps);
		registerAllServices(context, userGuide, userGuideProps);
		registerService(context, cyTableFormatter, NetworkAddedListener.class, new Properties());
	}

}
