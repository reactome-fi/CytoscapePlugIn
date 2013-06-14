package org.reactome.CS.x.internal;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;



public class UserGuideTaskFactory extends AbstractTaskFactory
{
    private CySwingApplication desktopApp;
    private OpenBrowser browser;
    public UserGuideTaskFactory(CySwingApplication desktopApp, OpenBrowser browser)
    {
	this.desktopApp=desktopApp;
	this.browser=browser;
    }

    @Override
    public TaskIterator createTaskIterator()
    {
	// TODO Auto-generated method stub
	return new TaskIterator(new UserGuideTask(desktopApp, browser));
    }

}
