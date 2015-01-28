/*
 * Created on Jan 27, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.math.MathException;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.GibbsSampling;
import org.reactome.factorgraph.InferenceCannotConvergeException;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.LoopyBeliefPropagation;
import org.reactome.factorgraph.Observation;

/**
 * Use this class for performing actual PGM inference.
 * @author gwu
 *
 */
public class InferenceRunner {
    private FactorGraph factorGraph;
    // Two inferencer
    private Inferencer lbp;
    private Inferencer gibbs;
    // Current status
    private InferenceStatus status;
    // To display running information
    private ProgressPane progressPane;
    // A flag to cancel inference
    private boolean abort;
    
    /**
     * Default constructor.
     */
    public InferenceRunner() {
    }
    
    public ProgressPane getProgressPane() {
        return progressPane;
    }

    public void setProgressPane(ProgressPane progressPane) {
        this.progressPane = progressPane;
    }

    public FactorGraph getFactorGraph() {
        return factorGraph;
    }

    public void setFactorGraph(FactorGraph factorGraph) {
        this.factorGraph = factorGraph;
    }

    public void setAlgorithms(List<Inferencer> algorithms) {
        if (algorithms == null)
            return;
        for (Inferencer alg : algorithms) {
            if (alg instanceof LoopyBeliefPropagation)
                lbp = alg;
            else if (alg instanceof GibbsSampling)
                gibbs = alg;
        }
    }
    
    public void abort() {
        this.abort = true;
    }

    public InferenceStatus getStatus() {
        return status;
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
        valuePane.setInferenceResults(fgResults);
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
        valuePane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
        valuePane.setFactorGraph(fgResults.getFactorGraph());

        // Show outputs results
        title = "IPA Pathway Analysis";
        index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, title);
        IPAPathwayOutputsPane outputPane = null;
        if (index > -1)
            outputPane = (IPAPathwayOutputsPane) tableBrowserPane.getComponentAt(index);
        else
            outputPane = new IPAPathwayOutputsPane(title);
        outputPane.setNetworkView(PopupMenuManager.getManager().getCurrentNetworkView());
        outputPane.setVariableResults(valuePane.getOutputVariableResults());
        if (index == -1)
            index = tableBrowserPane.indexOfComponent(outputPane);
        if (index >= 0) // Select this as the default table for viewing the results
            tableBrowserPane.setSelectedIndex(index);
    }
    
    public void performInference(boolean needFinishDialog) throws Exception {
        if (progressPane != null) {
            progressPane.enableCancelAction(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    abort();
                    // Need a interface to abort inference.
                    //                if (algorithm instanceof LoopyBeliefPropagation)
                    //                    ((LoopyBeliefPropagation)algorithm).setMaxIteration(0);
                    //                else if (algorithm instanceof GibbsSampling)
                    //                    ((GibbsSampling)algorithm).setMaxIteration(0);
                }
            });
        }
        // Use a new thread for performing inference so that it can be cancelled
        Thread t = new Thread() {
            public void run() {
                performInference();
            }
        };
        t.start();
        while (!progressPane.isCancelled()) {
            InferenceStatus status = getStatus();
            if (status == InferenceStatus.DONE || status == InferenceStatus.ERROR || status == InferenceStatus.ABORT) {
                break;
            }
            // Sleep for 2 seconds
            Thread.sleep(2000);
        }
        InferenceStatus status = getStatus();
        if (progressPane.isCancelled() || status != InferenceStatus.DONE) {
            return;
        }
        if (status == InferenceStatus.DONE) {
            FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(factorGraph);
            showIPANodeValues(fgResults);
            showIPAPathwayValues(fgResults);
            if (needFinishDialog) {
                String message = "Inference has finished successfully. You may use \"View Marginal Probabilities\" by\n" + 
                        "selecting a variable node";
                // Check if any posterior inference is done
                if (!fgResults.hasPosteriorResults())
                    message += ".";
                else
                    message += ", and view IPA values at the bottom \"IPA Node Values\" tab. \n" + 
                            "You may also view pathway level results at the \"IPA Pathway Analysis\" tab.\n" +
                            "Note: IPA stands for \"Integrated Pathway Activity\".";
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              message,
                                              "Inference Finished",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void performInference() {
        if (lbp == null && gibbs == null) // We cannot do anything if there is no inferencer is set.
            return;
        if (factorGraph == null)
            return; // Nothing to be inferred
        // If we have lbp, try lbp first. The following check must work
        // since we have already made sure at least one inferencer should be set.
        Inferencer inferencer = null;
        if (lbp != null)
            inferencer = lbp;
        else
            inferencer = gibbs;
        inferencer.setFactorGraph(factorGraph);
        try {
            status = InferenceStatus.WORKING;
            if (progressPane != null)
                progressPane.setText("Perform prior inference...");
            inferencer.setObservation(null);
            inferencer.runInference();
            FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(factorGraph);
            fgResults.storeInferenceResults(null);
            List<Observation> observations = FactorGraphRegistry.getRegistry().getObservations(factorGraph);
            if (observations != null) {
                progressPane.setIndeterminate(false);
                progressPane.setMaximum(observations.size());
                progressPane.setMinimum(0);
                int count = 0;
                for (Observation observation : observations) {
                    if (progressPane != null)
                        progressPane.setText("Sample: " + observation.getName());
                    inferencer.setObservation(observation.getVariableToAssignment());
                    inferencer.runInference();
                    fgResults.storeInferenceResults(observation.getName());
                    count ++;
                    progressPane.setValue(count);
                    if (abort)
                        break;
                }
            }
            observations = FactorGraphRegistry.getRegistry().getRandomObservations(factorGraph);
            if (observations != null) {
                int count = 0;
                progressPane.setMaximum(observations.size());
                for (Observation observation : observations) {
                    count ++;
                    if (progressPane != null)
                        progressPane.setText("Random sample: " + count);
                    inferencer.setObservation(observation.getVariableToAssignment());
                    inferencer.runInference();
                    fgResults.storeInferenceResults(observation.getName());
                    progressPane.setValue(count);
                    if (abort)
                        break;
                }
            }
            if (abort) {
                status = InferenceStatus.ABORT;
                // Empty inference results
                fgResults.clear();
            }
            else
                status = InferenceStatus.DONE;
        }
        catch(InferenceCannotConvergeException e) {
            String message = "Inference cannot converge. You may try to run inference again, which may converge\n" + 
                             "because of its stochastic feature, or try the Gibbs sampling algorithm.";
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Inference cannot converge. You may try run inferencePlease try the Gibbs sampling algorithm.",
                                          "Inference Cannot Converge",
                                          JOptionPane.ERROR_MESSAGE);
            status = InferenceStatus.ERROR;
        }
    }
    
}
