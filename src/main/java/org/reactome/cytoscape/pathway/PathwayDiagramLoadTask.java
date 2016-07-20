/*
 * Created on Jul 16, 2013
 *
 */
package org.reactome.cytoscape.pathway;

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

/**
 * This class is used to load pathway diagram from a Reactome database via RESTful API.
 * @author gwu
 *
 */
public class PathwayDiagramLoadTask extends AbstractTask {
    // DB_ID to be used for opening pathway diagram
    private Long pathwayId;
    // Pathway name for highlight
    private String pathwayName;
    
    public PathwayDiagramLoadTask() {
    }
    
    public void setPathwayId(Long dbId) {
        this.pathwayId = dbId;
    }
    
    public void setPathwayName(String name) {
        this.pathwayName = name;
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
        String text = ReactomeRESTfulService.getService().pathwayDiagram(pathwayId);
        taskMonitor.setProgress(0.50d);
        taskMonitor.setStatusMessage("Open pathway diagram...");
        //        System.out.println(text);
        final PathwayInternalFrame pathwayFrame = createPathwayFrame(text);
        pathwayFrame.setPathwayId(pathwayId);
        final JDesktopPane desktop = PlugInObjectManager.getManager().getPathwayDesktop();
        if (desktop == null) {
            // Cannot do anything
            taskMonitor.setStatusMessage("Cannot find a desktop container for showing pathway diagram!");
            taskMonitor.setProgress(1.0d);
            return; // Nowhere to show pathway!
        }
        desktop.add(pathwayFrame);
        PathwayDiagramRegistry.getRegistry().showPathwayDiagramFrame(pathwayFrame);
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
        PathwayEnrichmentHighlighter hiliter = PathwayEnrichmentHighlighter.getHighlighter();
        hiliter.highlightPathway(pathwayFrame, pathwayName);
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
