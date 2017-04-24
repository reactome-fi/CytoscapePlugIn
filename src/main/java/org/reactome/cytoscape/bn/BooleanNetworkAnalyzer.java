/*
 * Created on Apr 21, 2017
 *
 */
package org.reactome.cytoscape.bn;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CytoPanelName;
import org.gk.graphEditor.PathwayEditor;
import org.reactome.booleannetwork.BooleanNetwork;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.service.RESTFulFIService;
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

    public void startSimulation() {
        if (pathwayEditor == null || hiliteControlPane == null)
            return; 
        Long pathwayId = pathwayEditor.getRenderable().getReactomeId();
        if (pathwayId == null)
            return;
        RESTFulFIService fiService = new RESTFulFIService();
        BooleanNetwork network = null;
        try {
            network = fiService.convertPathwayToBooleanNetwork(pathwayId);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot convert the displayed pathway to a Boolean network:\n" + e.getMessage(),
                                          "Error in Converting",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return; // Cannot do anything basically
        }
        performSimulation(network);
    }
    
    /**
     * The actual method for Boolean network simulation.
     * @param network
     */
    private void performSimulation(BooleanNetwork network) {
        if (network == null)
            return;
        try {
            BooleanNetworkMainPane mainPane = (BooleanNetworkMainPane) PlugInUtilities.getCytoPanelComponent(BooleanNetworkMainPane.class,
                                                                                                             CytoPanelName.EAST,
                                                                                                             BooleanNetworkMainPane.TITLE);
            // Make sure the following order is correct
            mainPane.setPathwayEditor(pathwayEditor);
            mainPane.setBooleanNetwork(network);
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
