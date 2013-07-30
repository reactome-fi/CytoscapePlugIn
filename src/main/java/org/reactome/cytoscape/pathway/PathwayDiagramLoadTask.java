/*
 * Created on Jul 16, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.beans.PropertyVetoException;

import javax.swing.JDesktopPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.gk.render.RenderablePathway;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This class is used to load pathway diagram from a Reactome database via RESTful API.
 * @author gwu
 *
 */
public class PathwayDiagramLoadTask extends AbstractTask {
    // DB_ID to be used for opening pathway diagram
    private Long pathwayId;
    
    public PathwayDiagramLoadTask() {
    }
    
    public void setPathwayId(Long dbId) {
        this.pathwayId = dbId;
    }
    
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Load Pathway");
        if (pathwayId == null) {
            taskMonitor.setStatusMessage("No pathway id is specifcied!");
            taskMonitor.setProgress(1.0d);
            return; // Nothing to be displayed!
        }
        taskMonitor.setProgress(0);
        taskMonitor.setStatusMessage("Loading pathway diagram...");
        // This is just for test by query pathway diagram for Cell Cycle Checkpoints 
        String text = ReactomeRESTfulService.getService().pathwayDiagram(pathwayId);
        taskMonitor.setProgress(0.50d);
        taskMonitor.setStatusMessage("Open pathway diagram...");
//        System.out.println(text);
        PathwayInternalFrame pathwayFrame = createPathwayFrame(text);
        JDesktopPane desktop = PlugInUtilities.getCytoscapeDesktop();
        desktop.add(pathwayFrame);
        try {
            pathwayFrame.setSelected(true);
            pathwayFrame.toFront();
        }
        catch(PropertyVetoException e) {
            e.printStackTrace(); // Should not throw an exception
        }
        taskMonitor.setProgress(1.0d);
    }
    
    /**
     * A helper method to create a PathwayInternalFrame. 
     * @param xml
     * @return
     * @throws Exception
     */
    private PathwayInternalFrame createPathwayFrame(String xml) throws Exception {
        PathwayInternalFrame pathwayFrame = new PathwayInternalFrame("Pathway Diagram", true, true, true, true);
        pathwayFrame.setPathwayDiagramInXML(xml);
        RenderablePathway pathway = (RenderablePathway) pathwayFrame.getDisplayedPathway();
        PathwayDiagramRegistry.getRegistry().register(pathway.getReactomeDiagramId(),
                                                      pathwayFrame);
        pathwayFrame.setTitle(pathway.getDisplayName());
        InternalFrameListener frameLister = createFrameListener();
        pathwayFrame.addInternalFrameListener(frameLister);
        pathwayFrame.setSize(600, 450);
        pathwayFrame.setVisible(true);
        return pathwayFrame;
    }
    
    /**
     * A helper method to create an InternalFrameListener that should be added to JInternalFrame for a pathway.
     * @return
     */
    private InternalFrameListener createFrameListener() {
        InternalFrameListener listener = new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent event) {
                BundleContext context = PlugInObjectManager.getManager().getBundleContext();
                ServiceReference ref = context.getServiceReference(CyAppAdapter.class.getName());
                if (ref == null)
                    return;
                CyAppAdapter appAdapter = (CyAppAdapter) context.getService(ref);
                CyApplicationManager manager = appAdapter.getCyApplicationManager();
                manager.setCurrentNetworkView(null);
                context.ungetService(ref);
            }
        };
        return listener;
    }
    
}
