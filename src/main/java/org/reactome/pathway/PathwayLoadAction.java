/*
 * Created on Jul 18, 2013
 *
 */
package org.reactome.pathway;

import java.awt.event.ActionEvent;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * This customized CyAction is used to load a pathway diagram from Reactome via a RESTful API.
 * @author gwu
 */
public class PathwayLoadAction extends AbstractCyAction {
    private BundleContext context;
    
    public PathwayLoadAction() {
        super("Load Pathway Diagram");
        setPreferredMenu("Apps.Reactome FI");
    }
    
    public void setBundleContext(BundleContext context) {
        this.context = context;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void actionPerformed(ActionEvent event) {
        if (context == null)
            return;
        ServiceReference ref = context.getServiceReference(TaskManager.class.getName());
        if (ref == null)
            return;
        TaskManager tm = (TaskManager) context.getService(ref);
        if (tm == null)
            return;
        tm.execute(new TaskIterator(new PathwayLoadTask()));
        context.ungetService(ref);
    }
    
}
