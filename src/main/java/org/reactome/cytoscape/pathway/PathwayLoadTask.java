/*
 * Created on Jul 16, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.BorderLayout;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.gk.graphEditor.PathwayEditor;
import org.gk.persistence.DiagramGKBReader;
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
public class PathwayLoadTask extends AbstractTask {
    
    public PathwayLoadTask() {
    }
    
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Loading Pathway");
        taskMonitor.setProgress(0);
        taskMonitor.setStatusMessage("Load pathway diagram...");
        // This is just for test by query pathway diagram for Cell Cycle Checkpoints 
        Long dbId = 69620L;
        String reactomeRestfulURL = PlugInObjectManager.getManager().getProperties().getProperty("ReactomeRESTfulAPI");
        String url = reactomeRestfulURL + "pathwayDiagram/69620/xml";
        String text = PlugInUtilities.callHttpInText(url, PlugInUtilities.HTTP_GET, "");
//        System.out.println(text);
        PathwayEditor pathwayEditor = createPathwayEditor(text);
        JInternalFrame pathwayFrame = createInteranalFrame(pathwayEditor);
        JDesktopPane desktop = PlugInUtilities.getCytoscapeDesktop();
        desktop.add(pathwayFrame);
    }
    
    /**
     * A helper method to create a JInternalFrame to display pathway diagram from Reactome.
     * @param pathwayEditor
     * @return
     */
    private JInternalFrame createInteranalFrame(PathwayEditor pathwayEditor) {
        JInternalFrame pathwayFrame = new JInternalFrame("Pathway: Cell Cycle Checkpoints", true, true, true, true);
        InternalFrameListener frameLister = createFrameListener();
        pathwayFrame.addInternalFrameListener(frameLister);
        pathwayFrame.setSize(600, 450);
        JScrollPane jsp = new JScrollPane();
        jsp.setViewportView(pathwayEditor);
//        jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
//        jsp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        pathwayFrame.getContentPane().add(jsp, 
                                          BorderLayout.CENTER);
        pathwayFrame.setVisible(true);
        return pathwayFrame;
    }
    
    /**
     * A helper method to create a PathwayEditor. 
     * @param xml
     * @return
     * @throws Exception
     */
    private PathwayEditor createPathwayEditor(String xml) throws Exception {
        final PathwayEditor editor = new CyPathwayEditor();
        DiagramGKBReader reader = new DiagramGKBReader();
        RenderablePathway pathway = reader.openDiagram(xml);
        editor.setRenderable(pathway);
        editor.setEditable(false);
        return editor;
    }
    
    /**
     * A helper method to create an InternalFrameListener that should be added to JInternalFrame for a pathway.
     * @return
     */
    private InternalFrameListener createFrameListener() {
        InternalFrameListener listener = new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent arg0) {
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
