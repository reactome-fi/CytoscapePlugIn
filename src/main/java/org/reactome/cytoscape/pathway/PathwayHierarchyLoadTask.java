/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.Component;
import java.io.StringReader;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.reactome.cytoscape.service.PathwaySpecies;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This class is used to load Reactome pathway hierarchy and then displayed them in Cytoscape.
 * @author gwu
 *
 */
public class PathwayHierarchyLoadTask extends AbstractTask {
    
    private static final Logger logger = LoggerFactory.getLogger(PathwayHierarchyLoadTask.class);
    private PathwaySpecies species = PathwaySpecies.Homo_sapiens; // The default is always for human
    
    /**
     * Default constructor.
     */
    public PathwayHierarchyLoadTask() {
    }
    
    public PathwaySpecies getSpecies() {
        return species;
    }

    public void setSpecies(PathwaySpecies species) {
        this.species = species;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        displayReactomePathways(taskMonitor);
    }

    private void displayReactomePathways(TaskMonitor monitor) {
        PathwayControlPanel controlPane = PathwayControlPanel.getInstance();
        CySwingApplication application = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel panel = application.getCytoPanel(CytoPanelName.WEST);
        if (panel.getState() == CytoPanelState.HIDE)
            panel.setState(CytoPanelState.DOCK);
        // Check if controlPane has been displayed
        int index = getIndexOfPathwayControlPane(panel);
        if (index >= 0) {
            // Make sure it is selected
            panel.setSelectedIndex(index);
            if (controlPane.getCurrentSpecies().equals(species))
                return; // This is just a selection since pathways for the current species has been loaded.
        }
        if (panel.getCytoPanelComponentCount() > 0 && panel.getComponentAt(0) != null) {
            Component comp = panel.getComponentAt(0);
            controlPane.setPreferredSize(comp.getPreferredSize()); // Control its size to avoid weird behavior
        }   
        try {
            monitor.setStatusMessage("Loading pathways...");
            monitor.setTitle("Pathway Loading");
            monitor.setProgress(0.2d);
            String text = ReactomeRESTfulService.getService().pathwayHierarchy(species);
            StringReader reader = new StringReader(text);
            SAXBuilder builder = new SAXBuilder();
            Document document = builder.build(reader);
            Element root = document.getRootElement();
            controlPane.setAllPathwaysInElement(root);
            monitor.setProgress(1.0d);
        }
        catch(Exception e) {
            logger.error("Error in loading tree: " + e.getMessage(),
                         e);
            e.printStackTrace();
        }
        controlPane.setServiceRegistration(PlugInUtilities.registerCytoPanelComponent(controlPane));
        // Make sure if a new Reactome control panel can be selected
        // Note: This may not work if the above call is asynchronous!
        index = getIndexOfPathwayControlPane(panel);
        if (index >= 0)
            panel.setSelectedIndex(index);
        controlPane.validateDividerPosition();
        PathwayControlPanel.getInstance().setCurrentSpecies(species);
        // Make sure any displayed pathway is closed
        PathwayDiagramRegistry.getRegistry().closeAllFrames();
    }
    
    /**
     * A helper method to get the PathwayControlPanel panel.
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
