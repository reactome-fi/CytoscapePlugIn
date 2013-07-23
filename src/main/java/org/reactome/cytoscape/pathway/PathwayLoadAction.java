/*
 * Created on Jul 18, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This customized CyAction is used to load a pathway diagram from Reactome via a RESTful API.
 * @author gwu
 */
public class PathwayLoadAction extends AbstractCyAction {
    
    public PathwayLoadAction() {
        super("Load Pathway Diagram");
        setPreferredMenu("Apps.Reactome FI");
    }
    
    @Override
    @SuppressWarnings("rawtypes")
    public void actionPerformed(ActionEvent event) {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        if (context == null)
            return;
        ServiceReference ref = context.getServiceReference(TaskManager.class.getName());
        if (ref == null)
            return;
        TaskManager tm = (TaskManager) context.getService(ref);
        if (tm == null)
            return;
        PathwayLoadTask task = new PathwayLoadTask();
        tm.execute(new TaskIterator(task));
        context.ungetService(ref);
    }
    
}
