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
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This should be the entry point to perform Boolean network simulation.
 * @author gwu
 *
 */
public class BooleanNetworkAnalyzer {
    // The target panel showig a pathway diagram
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
            BooleanNetworkMainPane mainPane = (BooleanNetworkMainPane) PlugInUtilities.getCytoPanelComponent(BooleanNetworkMainPane.class,
                                                                                                             CytoPanelName.EAST,
                                                                                                             BooleanNetworkMainPane.TITLE);
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
