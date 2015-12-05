/*
 * Created on Feb 16, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.gk.render.RenderablePathway;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Variable;

/**
 * This class is used to control the display of inference results.
 * @author gwu
 *
 */
public class InferenceResultsControl {
    // For highlighting
    private PathwayHighlightControlPanel hiliteControlPane; 
    
    /**
     * Default constructor.
     */
    public InferenceResultsControl() {
    }
    
    public PathwayHighlightControlPanel getHiliteControlPane() {
        return hiliteControlPane;
    }

    public void setHiliteControlPane(PathwayHighlightControlPanel hiliteControlPane) {
        this.hiliteControlPane = hiliteControlPane;
    }

    /**
     * Calculate and show IPA values.
     * @param resultsList
     * @param target
     * @return true if values are shown.
     */
    private void showIPANodeValues(FactorGraphInferenceResults fgResults) {
        if (!fgResults.hasPosteriorResults()) // Just prior probabilities
            return ;
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        String title = "IPA Node Values";
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, title);
        IPAValueTablePane valuePane = null;
        if (index < 0)
            valuePane = new IPAValueTablePane(title);
        else
            valuePane = (IPAValueTablePane) tableBrowserPane.getComponentAt(index);
        valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
        valuePane.setFGInferenceResults(fgResults);
    }
    
    private void showIPAPathwayValues(FactorGraphInferenceResults fgResults) throws MathException {
        if (!fgResults.hasPosteriorResults())
            return; 
        String title = "IPA Sample Analysis";
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane,
                                                                    title);
        IPASampleAnalysisPane valuePane = null;
        if (index > -1)
            valuePane = (IPASampleAnalysisPane) tableBrowserPane.getComponentAt(index);
        else
            valuePane = new IPASampleAnalysisPane(title);
        if (valuePane.getFGInferenceResults() != fgResults) {
            valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
            Set<Variable> pathwayVars = getPathwayVars(fgResults.getFactorGraph());
            valuePane.setInferenceResults(fgResults, pathwayVars);
        }
        // Show outputs results
        title = "IPA Pathway Analysis";
        index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, title);
        IPAPathwaySummaryPane outputPane = null;
        if (index > -1)
            outputPane = (IPAPathwaySummaryPane) tableBrowserPane.getComponentAt(index);
        else
            outputPane = new IPAPathwaySummaryPane(title);
        if (outputPane.getFGInferenceResults() != fgResults) {
            outputPane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
            Set<Variable> outputVars = PlugInUtilities.getOutputVariables(fgResults.getFactorGraph());
            outputPane.setVariableResults(valuePane.getInferenceResults(),
                                          outputVars,
                                          fgResults.isUsedForTwoCases() ? fgResults.getSampleToType() : null);
            outputPane.setFGInferenceResults(fgResults);
        }
//         Only select it if this tab is newly created.
        if (index == -1) {
            index = tableBrowserPane.indexOfComponent(outputPane);
            if (index >= 0) // Select this as the default table for viewing the results
                tableBrowserPane.setSelectedIndex(index);
        }
        // Highlight pathway diagram
        if (hiliteControlPane != null) {
            Map<String, Double> idToValue = outputPane.getReactomeIdToIPADiff();
            hiliteControlPane.setIdToValue(idToValue);
            double[] minMaxValues = getMinMaxValues(idToValue);
            hiliteControlPane.resetMinMaxValues(minMaxValues);
            hiliteControlPane.setVisible(true);
            if (hiliteControlPane.getPathwayEditor() != null)
                outputPane.setPathwayDiagram((RenderablePathway)hiliteControlPane.getPathwayEditor().getRenderable());
        }
        // Show samples for the user's selection
        CytoPanel eastPane = desktopApp.getCytoPanel(CytoPanelName.EAST);
        // We want the east pane in the dock state
        if (eastPane.getState() != CytoPanelState.DOCK)
            eastPane.setState(CytoPanelState.DOCK);
        SampleListComponent samplePane = null;
        index = PlugInUtilities.getCytoPanelComponent(eastPane, SampleListComponent.TITLE);
        if (index > -1)
            samplePane = (SampleListComponent) eastPane.getComponentAt(index);
        else {
            samplePane = new SampleListComponent();
            index = eastPane.indexOfComponent(samplePane);
        }
        if (index > -1)
            eastPane.setSelectedIndex(index);
        samplePane.setInferenceResults(fgResults);
    }
    
    private double[] getMinMaxValues(Map<String, Double> idToValue) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Double value : idToValue.values()) {
            if (value < min)
                min = value;
            if (value > max)
                max = value;
        }
        // Want to keep to two digits
        min = Math.floor(min * 100) / 100.0d;
        max = Math.ceil(max * 100) / 100.0d;
        // If one is negative and one is positive, we want them to have
        // the same absolute values for easy comparison
        if (min < 0 && max > 0) {
            double tmp = Math.max(-min, max);
            min = -tmp;
            max = tmp;
        }
        return (new double[]{min, max});
    }

    public void showInferenceResults(FactorGraphInferenceResults fgResults) throws MathException {
        showIPANodeValues(fgResults);
        showIPAPathwayValues(fgResults);
    }
    
    private Set<Variable> getPathwayVars(FactorGraph fg) {
        Set<Variable> pathwayVars = new HashSet<Variable>();
        // If a variable's reactome id is in this list, it should be a output
        for (Variable var : fg.getVariables()) {
            String roles = var.getProperty("role");
            if (roles != null && roles.length() > 0)
                pathwayVars.add(var); // If there is a role assigned to the variable, it should be used as a pathway variable
        }
        return pathwayVars;
    }
    
}
