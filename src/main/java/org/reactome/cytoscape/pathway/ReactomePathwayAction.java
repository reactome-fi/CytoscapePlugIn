/*
 * Created on Jul 18, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This customized CyAction is used to load a pathway diagram from Reactome via a RESTful API.
 * @author gwu
 */
public class ReactomePathwayAction extends AbstractCyAction {
    
    public ReactomePathwayAction() {
        super("Reactome Pathways");
        setPreferredMenu("Apps.Reactome FI");
        setMenuGravity(30.0f);
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public void actionPerformed(ActionEvent event) {
        // Check if Reactome pathways have been loaded
        if (PlugInObjectManager.getManager().isPathwaysLoaded()) {
            int reply = JOptionPane.showConfirmDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                      "Reactome Pathways have been loaded already.\nDo you want to re-load them?", 
                                                      "Reload Reactome Pathways?", 
                                                      JOptionPane.YES_NO_OPTION);
            if (reply == JOptionPane.NO_OPTION)
                return; // No need to do anything
        }
        // Need a new session
        if (!PlugInUtilities.createNewSession())
            return;
        // Make sure the latest version of RESTful API is used since the latest 
        // version of pathways are configured in the Reactome RESTful API
        // that is used for pathway loading
        PlugInObjectManager manager = PlugInObjectManager.getManager();
        String fiVersion = manager.getLatestFINetworkVersion();
        manager.setFiNetworkVersion(fiVersion);
        // Actual loading
        TaskManager tm = manager.getTaskManager();
        if (tm == null)
            return;
        PathwayHierarchyLoadTask task = new PathwayHierarchyLoadTask();
        tm.execute(new TaskIterator(task));
    }
    
}
