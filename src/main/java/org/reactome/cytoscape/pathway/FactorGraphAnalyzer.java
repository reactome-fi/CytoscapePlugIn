/*
 * Created on Jan 26, 2015
 *
 */
package org.reactome.cytoscape.pathway;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.gk.render.RenderablePathway;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.FactorGraph;

/**
 * This class is used to perform factor graph based pathway analysis.
 * @author gwu
 *
 */
public class FactorGraphAnalyzer {
    // If this analyzer is used for a sinle pathway run, the following two memeber
    // variables should be provided
    private Long pathwayId;
    private RenderablePathway pathwayDiagram;
    
    /**
     * Default constructor.
     */
    public FactorGraphAnalyzer() {
    }
    
    public Long getPathwayId() {
        return pathwayId;
    }

    public void setPathwayId(Long pathwayId) {
        this.pathwayId = pathwayId;
    }

    public RenderablePathway getPathwayDiagram() {
        return pathwayDiagram;
    }

    public void setPathwayDiagram(RenderablePathway pathwayDiagram) {
        this.pathwayDiagram = pathwayDiagram;
    }

    /**
     * This is the actual place where the factor graph based analysis is performed.
     */
    public void runFactorGraphAnalysis() {
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        if (pathwayId == null || pathwayDiagram == null) {
            JOptionPane.showMessageDialog(frame,
                                          "Both pathwayId and pathwayDiagram should be provided.", 
                                          "Not Enough Information",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        ProgressPane progressPane = new ProgressPane();
        progressPane.setTitle("Run Graphical Model Analysis");
        progressPane.setIndeterminate(true);
        frame.setGlassPane(progressPane);
        progressPane.setVisible(true);
        // Convert to a FactorGraph using this object
        DiagramAndFactorGraphSwitcher switcher = new DiagramAndFactorGraphSwitcher();
        try {
            progressPane.setText("Converting pathway into a graphical model...");
            FactorGraph factorGraph = switcher.convertPathwayToFactorGraph(pathwayId, pathwayDiagram);
            if (factorGraph == null) {
                progressPane.setVisible(false);
                return; // Something may be wrong
            }
            progressPane.setText("Analysis is done!");
            progressPane.setVisible(false);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(frame,
                                          "Error in graphical model analysis: " + e,
                                          "Error in Graphical Model Analysis",
                                          JOptionPane.ERROR_MESSAGE);
            progressPane.setVisible(false);
        }
    }
    
}