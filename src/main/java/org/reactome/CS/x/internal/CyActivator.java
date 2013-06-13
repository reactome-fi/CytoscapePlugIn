package org.reactome.CS.x.internal;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.task.AbstractNetworkTaskFactory;
import org.osgi.framework.BundleContext;

public class CyActivator extends AbstractCyActivator {

    	public CyActivator()
    	{
    	    super();
    	}
	public void start(BundleContext context) throws Exception {
		
		CyApplicationManager cyApplicationManager = getService(context, CyApplicationManager.class);
		
		GSMATaskFactory gsmaTaskFactory = new GSMATaskFactory();
		Properties gsmaProps = new Properties();
		gsmaProps.setProperty("preferredMenu", "Apps.ReactomeFIS");
		gsmaProps.setProperty("menuGravity", "1.0");
		gsmaProps.setProperty("title", "Gene Set/Mutation Analysis");
		Properties properties = new Properties();
		
		registerAllServices(context, gsmaTaskFactory, gsmaProps);
	}

}
