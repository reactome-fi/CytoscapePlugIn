package org.reactome.cytoscape3;
import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;


public class UserGuideAction extends AbstractCyAction
{

    private CySwingApplication desktopApp;
    private OpenBrowser browser;
    private String userGuideURL = "http://wiki.reactome.org/index.php/Reactome_FI_Cytoscape_Plugin";

    public UserGuideAction(CySwingApplication desktopApp, OpenBrowser browser)
    {
	super("User Guide");
	this.desktopApp = desktopApp;
	this.browser = browser;
	setPreferredMenu("Apps.ReactomeFI");
	// TODO Auto-generated constructor stub
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
	// TODO Auto-generated method stub
	this.desktopApp = desktopApp;
	this.browser = browser;
	browser.openURL(userGuideURL);
//	ActionDialogs gui = new ActionDialogs("UGA");
//	gui.setLocationRelativeTo(desktopApp.getJFrame());
//	gui.setModal(true);
//	gui.setVisible(true);
    }

}
