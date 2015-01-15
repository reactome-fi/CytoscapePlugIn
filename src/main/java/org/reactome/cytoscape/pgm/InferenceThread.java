/*
 * Created on Jan 14, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.util.List;

import javax.swing.JOptionPane;

import org.gk.util.ProgressPane;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.InferenceCannotConvergeException;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.Observation;


/**
 * This customized Thread is used to handle PGM inferenceing
 * @author gwu
 *
 */
public class InferenceThread extends Thread {
    
    private Inferencer inferencer;
    private InferenceStatus status;
    // To display running information
    private ProgressPane progressPane;
    // A flag to cancel inference
    private boolean abort;
    
    public InferenceThread() {
    }

    public void setInferencer(Inferencer inferencer) {
        this.inferencer = inferencer;
    }
    
    /**
     * Abort the thread.
     */
    public void abort() {
        this.abort = true;
    }
    
    public Inferencer getInferencer() {
        return this.inferencer;
    }
    
    public InferenceStatus getStatus() {
        return status;
    }

    public ProgressPane getProgressPane() {
        return progressPane;
    }

    public void setProgressPane(ProgressPane progressPane) {
        this.progressPane = progressPane;
    }

    public void run() {
        if (inferencer == null || inferencer.getFactorGraph() == null)
            return;
        FactorGraph fg = inferencer.getFactorGraph();
        try {
            status = InferenceStatus.WORKING;
            if (progressPane != null)
                progressPane.setText("Perform prior inference...");
            inferencer.setObservation(null);
            inferencer.runInference();
            FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(fg);
            fgResults.storeInferenceResults(null);
            List<Observation> observations = FactorGraphRegistry.getRegistry().getObservations(fg);
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
            observations = FactorGraphRegistry.getRegistry().getRandomObservations(fg);
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
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Inference cannot converge. Please try the Gibbs sampling algorithm.",
                                          "Inference Cannot Converge",
                                          JOptionPane.ERROR_MESSAGE);
            status = InferenceStatus.ERROR;
        }
    }
    
}
