/*
 * Created on Jul 18, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.application.swing.CySwingApplication;
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
    private String reactomeRestfulURL;
    
    public PathwayLoadAction() {
        super("Load Pathway Diagram");
        setPreferredMenu("Apps.Reactome FI");
    }
    
    public void setBundleContext(BundleContext context) {
        this.context = context;
    }
    
    public void setReactomeRestfulURL(String url) {
        this.reactomeRestfulURL = url;
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
        PathwayLoadTask task = new PathwayLoadTask();
        task.setReactomeRestfulURL(reactomeRestfulURL);
        JDesktopPane desktop = getCytoscapeDesktop();
        task.setDesktopPane(desktop);
        tm.execute(new TaskIterator(task));
        context.ungetService(ref);
    }
    
    private JDesktopPane getCytoscapeDesktop() {
        ServiceReference ref = context.getServiceReference(CySwingApplication.class.getName());
        if (ref == null)
            return null;
        CySwingApplication application = (CySwingApplication) context.getService(ref);
        JFrame frame = application.getJFrame();
        // Use this loop to find JDesktopPane
        Set<Component> children = new HashSet<Component>();
        for (Component comp : frame.getComponents())
            children.add(comp);
        Set<Component> next = new HashSet<Component>();
        while (children.size() > 0) {
            for (Component comp : children) {
                if (comp instanceof JDesktopPane)
                    return (JDesktopPane) comp;
                if (comp instanceof Container) {
                    Container container = (Container) comp;
                    if (container.getComponentCount() > 0) {
                        for (Component comp1 : container.getComponents())
                            next.add(comp1);
                    }
                }
            }
            children.clear();
            children.addAll(next);
            next.clear();
            
        }
        return null;
    }    
}
