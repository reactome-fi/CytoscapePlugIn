/*
 * Created on Jan 27, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.apache.commons.math.MathException;
import org.gk.graphEditor.PathwayEditor;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.MessageDialog;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.GibbsSampling;
import org.reactome.factorgraph.InferenceCannotConvergeException;
import org.reactome.factorgraph.InferenceType;
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
    // Flag indicating the final results should be performed based on two cases
    private boolean usedForTwoCases;
    // For highlighting
    private PathwayHighlightControlPanel hiliteControlPane;
    // For synnchronizeation
    private PathwayEditor pathwayEditor;
    
    /**
     * Default constructor.
     */
    public InferenceRunner() {
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

    public boolean isUsedForTwoCases() {
        return usedForTwoCases;
    }

    public void setUsedForTwoCases(boolean usedForTwoCases) {
        this.usedForTwoCases = usedForTwoCases;
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
                _performInference();
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
            showInferenceResults(fgResults);
            if (needFinishDialog) {
                String message = "Inference has finished successfully. ";
                // Check if any posterior inference is done
                if (!fgResults.hasPosteriorResults())
                    message += "You may use \"View Marginal Probabilities\" by\n" + 
                        "selecting a variable node.";
                else
                    message += "You may view IPA values at the bottom \"IPA Node Values\" tab. \n" + 
                            "You may also view pathway level results at the \"IPA Pathway Analysis\" and \"IPA Sample Analysis\" tab.\n" +
                            "Note: IPA stands for \"Integrated Pathway Activity\".";
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              message,
                                              "Inference Finished",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void showInferenceResults(FactorGraphInferenceResults fgResults) throws MathException, IllegalAccessException, InstantiationException {
        InferenceResultsControl control = new InferenceResultsControl();
        control.setHiliteControlPane(hiliteControlPane);
        control.setPathwayEditor(pathwayEditor);
        control.showInferenceResults(fgResults);
    }
    
    /**
     * This is the actual place to run inference
     */
    private void performInference(Observation<Number> observation,
                                  String sample) throws InferenceCannotConvergeException {
        // If LBP is set, we will use it.
        if (lbp != null) {
            try {
                lbp.setObservation(observation);
                lbp.runInference();
            }
            catch(InferenceCannotConvergeException e) {
                if (gibbs == null || ((LoopyBeliefPropagation)lbp).getInferenceType() == InferenceType.MAX_PRODUCT)
                    throw e; // Gibbs cannot support MAX_PRODUCT
                if (gibbs != null) { // Try switch to Gibbs automatically if it is set.
                    progressPane.setText("Use Gibbs for " + 
                                         (sample == null ? " prior" : sample));
                    gibbs.setObservation(observation);
                    gibbs.runInference();
                }
            }
        }
        else { // Only Gibbs is set
            gibbs.setObservation(observation);
            gibbs.runInference();
        }
    }
    
    public synchronized void performInference() {
        if (lbp == null && gibbs == null) // We cannot do anything if there is no inferencer is set.
            return;
        if (factorGraph == null)
            return; // Nothing to be inferred
        // If we have lbp, try lbp first. The following check must work
        // since we have already made sure at least one inferencer should be set.
        if (lbp != null)
            lbp.setFactorGraph(factorGraph);
        if (gibbs != null)
            gibbs.setFactorGraph(factorGraph);
        status = InferenceStatus.WORKING;
        if (progressPane != null)
            progressPane.setText("Perform prior inference...");
        try {
            performInference(null, null);
        }
        catch(InferenceCannotConvergeException exception) { // There is nothing can be done.
            throw new IllegalStateException("Prior inference cannot converge.");
        }
        FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(factorGraph);
        fgResults.setUsedForTwoCases(usedForTwoCases);
        fgResults.storeInferenceResults(null); // Store prior result
        List<Observation<Number>> observations = FactorGraphRegistry.getRegistry().getObservations(factorGraph);
        Map<String, String> sampleToType = new HashMap<String, String>();
        fgResults.setSampleToType(sampleToType);
        List<Observation<Number>> convergedObservations = new ArrayList<Observation<Number>>();
        if (observations != null) {
            progressPane.setIndeterminate(false);
            progressPane.setMaximum(observations.size());
            progressPane.setMinimum(0);
            int count = 0;
            for (Observation<Number> observation : observations) {
                // If this is used for two cases and there is no type information for the observation
                // the inference will not be performed for it.
                if (usedForTwoCases && observation.getAnnoation() == null)
                    continue; 
                if (progressPane != null)
                    progressPane.setText("Sample: " + observation.getName());
                try {
                    performInference(observation, 
                                     observation.getName());
                }
                catch (InferenceCannotConvergeException e) {
                    continue; // Just switch to next sample
                }
                fgResults.storeInferenceResults(observation.getName());
                // If there is no sample type, don't include to avoid a new type (null!)
                // appears in the further analysis.
                if (observation.getAnnoation() != null)
                    sampleToType.put(observation.getName(), observation.getAnnoation());
                count ++;
                convergedObservations.add(observation);
                progressPane.setValue(count);
                if (abort)
                    break;
            }
            fgResults.setObservations(convergedObservations);
            if (sampleToType.size() > 0 && fgResults.isUsedForTwoCases())
                fgResults.setSampleToType(sampleToType);
        }
        if (!abort) { // Maybe abort in the above loop.
            observations = FactorGraphRegistry.getRegistry().getRandomObservations(factorGraph);
            List<Observation<Number>> convergedRandomObs = new ArrayList<Observation<Number>>();
            if (observations != null) {
                int count = 0;
                progressPane.setMaximum(observations.size());
                for (Observation<Number> observation : observations) {
                    count ++;
                    if (progressPane != null)
                        progressPane.setText("Random sample: " + count);
                    try {
                        performInference(observation,
                                         "Random sample " + count);
                    }
                    catch(InferenceCannotConvergeException e) {
                        // For random sample, just escape it
                        continue;
                    }
                    fgResults.storeInferenceResults(observation.getName());
                    progressPane.setValue(count);
                    convergedRandomObs.add(observation);
                    if (abort)
                        break;
                }
            }
            fgResults.setRandomObservations(convergedRandomObs);
        }
        if (abort) {
            status = InferenceStatus.ABORT;
            // Empty inference results
            fgResults.clear();
        }
        else
            status = InferenceStatus.DONE;
    }

    private void _performInference() {
        try {
            performInference();
        }
        catch(IllegalStateException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in inference: " + e,
                                          "Inference Error",
                                          JOptionPane.ERROR_MESSAGE);
            status = InferenceStatus.ERROR;
        }
        if (status != InferenceStatus.DONE)
            return;
        StringBuilder errorMessage = new StringBuilder();
        // Check how many Random samples have not converged
        FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(factorGraph);
        List<Observation<Number>> randomObs = FactorGraphRegistry.getRegistry().getRandomObservations(factorGraph);
        List<Observation<Number>> convergedRandomObs = fgResults.getRandomObservations();
        List<Observation<Number>> randomCopy = new ArrayList<Observation<Number>>(randomObs);
        randomCopy.removeAll(convergedRandomObs);
        if (randomCopy.size() > 0) {
            errorMessage.append(randomCopy.size() + " random samples out of " + randomObs.size() + " couldn't converge during inference. ");
        }
        List<Observation<Number>> obs = FactorGraphRegistry.getRegistry().getObservations(factorGraph);
        List<Observation<Number>> convergedObs = fgResults.getObservations();
        List<Observation<Number>> obsCopy = new ArrayList<Observation<Number>>(obs);
        obsCopy.removeAll(convergedObs);
        if (obsCopy.size() > 0) {
            errorMessage.append("The inference with the following samples couldn't converge (" + obsCopy.size() + "/" + obs.size() + "):");
            for (Observation<Number> ob : obsCopy) {
                errorMessage.append("\n    " + ob.getName());
            }
        }
        if (errorMessage.length() > 0) {
            MessageDialog dialog = new MessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
            dialog.setTitle("Results Warning!");
            dialog.setText(errorMessage.toString());
            dialog.setModal(true);
            dialog.setLocationRelativeTo(dialog.getOwner());
            dialog.setSize(500, 400);
            dialog.setVisible(true);
        }
    }
    
}
