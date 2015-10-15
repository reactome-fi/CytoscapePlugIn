/*
 * Created on Oct 8, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.util.List;
import java.util.Map;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.reactome.cytoscape.pgm.FactorGraphInferenceResults;
import org.reactome.cytoscape.pgm.IPAValueTablePane;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;

/**
 * This singleton class is used to store observations and inference results.
 * This method is similar to these three classes in package org.reactome.cytoscape.pgm:
 * FactorGraphRegistry, FactorGraphInferenceResults and VariableInferenceResults. Most 
 * likely, this class should be merged into those three classes.
 * @author gwu
 *
 */
public class FIPGMResults {
    private static FIPGMResults results;
    // Stored information
    private List<Observation<Number>> observations;
    private List<Observation<Number>> randomObservations;
    private Map<String, Map<Variable, Double>> sampleToVarToScore;
    private Map<String, Map<Variable, Double>> randomSampleToVarToScore;
    
    /**
     * Default constructor should not be called.
     */
    private FIPGMResults() {
    }
    
    public static FIPGMResults getResults() {
        if (results == null)
            results = new FIPGMResults();
        return results;
    }
    
    /**
     * Convert this FIPGMResults to a FactorGraphInferenceResults to be used
     * in other GUIs.
     * @return
     */
    public FactorGraphInferenceResults convertToFactorGraphInferenceResults() {
        FactorGraphInferenceResults fgResults = new FactorGraphInferenceResults();
        return fgResults;
    }

    public List<Observation<Number>> getObservations() {
        return observations;
    }

    public void setObservations(List<Observation<Number>> observations) {
        this.observations = observations;
    }

    public List<Observation<Number>> getRandomObservations() {
        return randomObservations;
    }

    public void setRandomObservations(List<Observation<Number>> randomObservations) {
        this.randomObservations = randomObservations;
    }

    public Map<String, Map<Variable, Double>> getSampleToVarToScore() {
        return sampleToVarToScore;
    }

    public void setSampleToVarToScore(Map<String, Map<Variable, Double>> sampleToVarToScore) {
        this.sampleToVarToScore = sampleToVarToScore;
    }

    public Map<String, Map<Variable, Double>> getRandomSampleToVarToScore() {
        return randomSampleToVarToScore;
    }

    public void setRandomSampleToVarToScore(Map<String, Map<Variable, Double>> randomSampleToVarToScore) {
        this.randomSampleToVarToScore = randomSampleToVarToScore;
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
    
}
