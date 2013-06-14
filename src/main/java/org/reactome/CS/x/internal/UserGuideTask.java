package org.reactome.CS.x.internal;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;


/**
 * Creates a new menu item under Apps menu section.
 *
 */
public class UserGuideTask extends AbstractTask {

	private CySwingApplication desktopApp;
	private OpenBrowser browser;
	private String  userGuideURL = "http://wiki.reactome.org/index.php/Reactome_FI_Cytoscape_Plugin";

	public UserGuideTask(CySwingApplication desktopApp,
		OpenBrowser browser)
	{
		this.desktopApp = desktopApp;
		this.browser = browser;
		
		
	}


	@Override
	public void run(TaskMonitor arg0) throws Exception
	{
	    browser.openURL(userGuideURL);
	}
}
