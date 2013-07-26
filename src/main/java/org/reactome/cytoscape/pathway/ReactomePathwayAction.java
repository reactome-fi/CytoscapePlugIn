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

/**
 * This customized CyAction is used to load a pathway diagram from Reactome via a RESTful API.
 * @author gwu
 */
public class ReactomePathwayAction extends AbstractCyAction {
    
    public ReactomePathwayAction() {
        super("Reactome Pathways");
        setPreferredMenu("Apps.Reactome FI");
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public void actionPerformed(ActionEvent event) {
        TaskManager tm = PlugInObjectManager.getManager().getTaskManager();
        if (tm == null)
            return;
        PathwayHierarchyLoadTask task = new PathwayHierarchyLoadTask();
        tm.execute(new TaskIterator(task));
    }
    
}
