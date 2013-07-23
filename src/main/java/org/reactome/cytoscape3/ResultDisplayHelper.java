package org.reactome.cytoscape3;

import java.util.Map;
import java.util.Set;

import javax.swing.SwingConstants;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;




public class ResultDisplayHelper
{
private static ResultDisplayHelper helper;
    
    private ResultDisplayHelper() {
    }
    
    public static ResultDisplayHelper getHelper() {
        if (helper == null)
            helper = new ResultDisplayHelper();
        return helper;
    }
    
    /**
     * A helper method to show network modules.
     * @param nodeToCluster
     * @param view
     */
    protected void showModuleInTab(Map<String, Integer> nodeToCluster,
            Map<String, Set<String>> nodeToSampleSet,
            Double modularity,
            CyNetworkView view)
    {
        CySwingApplication desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
//        String title = "Network Module Browser";
//        int moduleBrowserIndex = tableBrowserPane.;
    }
}
