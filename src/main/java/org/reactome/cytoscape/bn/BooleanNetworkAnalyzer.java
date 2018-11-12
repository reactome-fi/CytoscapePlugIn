/*
 * Created on Apr 21, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.ProcessNode;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This should be the entry point to perform Boolean network simulation.
 * @author gwu
 *
 */
public class BooleanNetworkAnalyzer {
    // The target panel showing a pathway diagram
    private PathwayEditor pathwayEditor;
    // Used to highlight the displayed pathway diagram
    private PathwayHighlightControlPanel hiliteControlPane;
    
    /**
     * Default constructor.
     */
    public BooleanNetworkAnalyzer() {
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }

    public void setPathwayEditor(PathwayEditor pathwayEditor) {
        this.pathwayEditor = pathwayEditor;
    }

    public PathwayHighlightControlPanel getHiliteControlPane() {
        return hiliteControlPane;
    }

    public void setHiliteControlPane(PathwayHighlightControlPanel hiliteControlPane) {
        this.hiliteControlPane = hiliteControlPane;
    }
    
    /**
     * Check if the wrapped pathway can be used for Boolean network simulation. Only pathway
     * diagrams have entities laid out can be used for simulation.
     * @return
     */
    public boolean isValidPathwayForBNSimulation() {
        if (pathwayEditor == null)
            return false;
        RenderablePathway pathway = (RenderablePathway) pathwayEditor.getRenderable();
        boolean hasEntity = false;
        for (Object o : pathway.getComponents()) {
            Renderable r = (Renderable) o;
            // If r is not a process node and has reactome id,
            // the diagram should be ELV
            if (r.getReactomeId() != null && !(r instanceof ProcessNode)) {
                hasEntity = true;
                break;
            }
        }
        if (hasEntity)
            return true;
        JOptionPane.showMessageDialog(pathwayEditor,
                                     "This pathway cannot be used for Boolean network simulation. Choose\n" +
                                     "its sub-pathway having entities drawn for simulation.",
                                     "Error in Simulation",
                                     JOptionPane.ERROR_MESSAGE);
        return false;
    }
    
    /**
     * Remove displayed analysis results.
     */
    public void removeResults() {
        if (pathwayEditor == null || hiliteControlPane == null)
            return;
        // Remove highlight colors
        hiliteControlPane.removeHighlight();
        hiliteControlPane.setVisible(false);
        // Remove tabs related to BNs
        try {
            BooleanNetworkMainPane mainPane = (BooleanNetworkMainPane) PlugInUtilities.getCytoPanelComponent(CytoPanelName.EAST,
                                                                                                             BooleanNetworkMainPane.TITLE);
            if (mainPane != null)
                mainPane.close();
            // Remove tabs for showing BN results
            CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(CytoPanelName.SOUTH);
            List<VariableCytoPaneComponent> toBeClosed = new ArrayList<>();
            for (int i = 0; i < cytoPanel.getCytoPanelComponentCount(); i++) {
                Component c = cytoPanel.getComponentAt(i);
                if (c instanceof VariableCytoPaneComponent) {
                    toBeClosed.add((VariableCytoPaneComponent)c);
                }
            }
            toBeClosed.stream().forEach(p -> p.close());
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot remove Boolean network analysis results:\n" + e.getMessage(),
                                          "Error in Removing",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void startSimulation() {
        if (pathwayEditor == null || hiliteControlPane == null)
            return; 
        try {
            BooleanNetworkMainPane mainPane = (BooleanNetworkMainPane) PlugInUtilities.getCytoPanelComponent(BooleanNetworkMainPane.class,
                                                                                                             CytoPanelName.EAST,
                                                                                                             BooleanNetworkMainPane.TITLE);
            // Make sure the following order is correct
            mainPane.setPathwayEditor(pathwayEditor);
            mainPane.setHiliteControlPane(hiliteControlPane);
            mainPane.createNewSimulation();
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot perform Boolean network simulation:\n" + e.getMessage(),
                                          "Error in Simulation",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return; // Cannot do anything basically
        }
    }
    
}
