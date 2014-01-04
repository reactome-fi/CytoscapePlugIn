/*
 * Created on Jul 18, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.event.ActionEvent;

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
        setMenuGravity(3.0f);
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public void actionPerformed(ActionEvent event) {
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
