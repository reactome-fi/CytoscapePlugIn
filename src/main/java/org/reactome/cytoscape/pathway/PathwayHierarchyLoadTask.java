/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.Component;
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is used to load Reactome pathway hierarchy and then displayed them in Cytoscape.
 * @author gwu
 *
 */
public class PathwayHierarchyLoadTask extends AbstractTask {
    
    private static final Logger logger = LoggerFactory.getLogger(PathwayHierarchyLoadTask.class);
    
    /**
     * Default constructor.
     */
    public PathwayHierarchyLoadTask() {
    }
    
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        displayPathwayHierarchy(taskMonitor);
    }

    private void displayPathwayHierarchy(TaskMonitor monitor) {
        CySwingApplication application = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel panel = application.getCytoPanel(CytoPanelName.WEST);
        if (panel.getState() == CytoPanelState.HIDE)
            panel.setState(CytoPanelState.DOCK);
        // Check if controlPane has been displayed
        int index = getIndexOfPathwayControlPane(panel);
        if (index >= 0) {
            // Make sure it is selected
            panel.setSelectedIndex(index);
            return;
        }
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        PathwayControlPanel controlPane = new PathwayControlPanel();
        try {
            monitor.setStatusMessage("Loading pathways...");
            monitor.setTitle("Pathway Loading");
            monitor.setProgress(0.2d);
            controlPane.loadEventTree();
            monitor.setProgress(1.0d);
        }
        catch(Exception e) {
            logger.error("Error in loading tree: " + e.getMessage(),
                         e);
            e.printStackTrace();
        }
        context.registerService(CytoPanelComponent.class.getName(),
                                controlPane,
                                new Properties());
        // Make sure if a new Reactome control panel can be selected
        // Note: This may not work if the above call is asynchronous!
        index = getIndexOfPathwayControlPane(panel);
        if (index >= 0)
            panel.setSelectedIndex(index);
    }
    
    /**
     * It seems that a simple CytoPanel.indexOf
     * @param cytoPanel
     * @return
     */
    private int getIndexOfPathwayControlPane(CytoPanel cytoPanel) {
        for (int i = 0; i < cytoPanel.getCytoPanelComponentCount(); i++) {
            Component comp = cytoPanel.getComponentAt(i);
            if (comp instanceof PathwayControlPanel)
                return i;
        }
        return -1;
    }
    
}
