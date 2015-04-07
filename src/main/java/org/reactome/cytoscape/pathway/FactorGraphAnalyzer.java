/*
 * Created on Jan 26, 2015
 *
 */
package org.reactome.cytoscape.pathway;

import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.math.MathException;
import org.gk.render.RenderablePathway;
import org.gk.util.GKApplicationUtilities;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.pgm.FactorGraphInferenceResults;
import org.reactome.cytoscape.pgm.FactorGraphRegistry;
import org.reactome.cytoscape.pgm.InferenceAlgorithmPane;
import org.reactome.cytoscape.pgm.InferenceResultsControl;
import org.reactome.cytoscape.pgm.InferenceRunner;
import org.reactome.cytoscape.pgm.ObservationDataHelper;
import org.reactome.cytoscape.pgm.ObservationDataLoadPanel;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Inferencer;

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
    private PathwayHighlightControlPanel hiliteControlPane;
    // Information entered by the user
    private File geneExpFile;
    private double[] geneExpThresholdValues;
    private File cnvFile;
    private double[] cnvThresholdValues;
    
    /**
     * Default constructor.
     */
    public FactorGraphAnalyzer() {
    }
    
    public PathwayHighlightControlPanel getHighlightControlPane() {
        return hiliteControlPane;
    }

    public void setHighlightControlPane(PathwayHighlightControlPanel highlightControlPane) {
        this.hiliteControlPane = highlightControlPane;
    }


    public File getSampleInfoFile() {
        return FactorGraphRegistry.getRegistry().getTwoCaseSampleInfoFile();
    }

    public void setTwoCasesSampleInfoFile(File sampleInfoFile) {
        FactorGraphRegistry.getRegistry().setTwoCasesSampleInfoFile(sampleInfoFile);
    }

    public File getGeneExpFile() {
        return geneExpFile;
    }

    public void setGeneExpFile(File geneExpFile) {
        this.geneExpFile = geneExpFile;
    }

    public double[] getGeneExpThresholdValues() {
        return geneExpThresholdValues;
    }

    public void setGeneExpThresholdValues(double[] geneExpThresholdValues) {
        this.geneExpThresholdValues = geneExpThresholdValues;
    }

    public File getCnvFile() {
        return cnvFile;
    }

    public void setCnvFile(File cnvFile) {
        this.cnvFile = cnvFile;
    }

    public double[] getCnvThresholdValues() {
        return cnvThresholdValues;
    }

    public void setCnvThresholdValues(double[] cnvThresholdValues) {
        this.cnvThresholdValues = cnvThresholdValues;
    }

    public void setAlgorithms(List<Inferencer> algorithms) {
        FactorGraphRegistry.getRegistry().setLoadedAlgorithms(algorithms);
    }
    
    public List<Inferencer> getAlgorithms() {
        return FactorGraphRegistry.getRegistry().getLoadedAlgorithms();
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
     * Call this method to start a factor graph analysis. 
     */
    public void startAnalysis() {
        // If the user doesn't want to use this experiemental feature, just do nothing.
        if (!FactorGraphRegistry.getRegistry().showFeatureWarningDialog())
            return;
        if (FactorGraphRegistry.getRegistry().isDataLoaded()) {
            int reply = JOptionPane.showConfirmDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                      "Data and algorithms have been loaded previously. Do you want to reload them?",
                                                      "Reload Data and Algorithms?",
                                                      JOptionPane.YES_NO_CANCEL_OPTION);
            if (reply == JOptionPane.CANCEL_OPTION)
                return; 
            if (reply == JOptionPane.NO_OPTION) { // There is no need to reload data.
                // Don't show escape name dialog
                FactorGraphRegistry.getRegistry().setNeedEscapeNameDialog(false);
                Thread t = new Thread() {
                    public void run() {
                        runFactorGraphAnalysis();
                    }
                };
                t.start();
                return;
            }
            // If Yes, the following code will be used.
        }
        FactorGraphRegistry.getRegistry().setNeedEscapeNameDialog(true);
        FactorGraphAnalysisDialog dialog = new FactorGraphAnalysisDialog();
        dialog.setSize(625, 630);
        GKApplicationUtilities.center(dialog);
        dialog.setModal(true);
        dialog.setVisible(true);
        if (dialog.isOkClicked()) {
            getParametersFromDialog(dialog);
            Thread t = new Thread() {
                public void run() {
                    runFactorGraphAnalysis();
                }
            };
            t.start();
        }
    }

    private void getParametersFromDialog(FactorGraphAnalysisDialog dialog) {
        // Initialize FactorGraphAnalyzer and set up its required member variables
        // for performing analysis.
        ObservationDataLoadPanel dataPane = dialog.getDataLoadPane();
        setGeneExpFile(dataPane.getGeneExpFile());
        setGeneExpThresholdValues(dataPane.getGeneExpThresholdValues());
        setCnvFile(dataPane.getDNAFile());
        setCnvThresholdValues(dataPane.getDNAThresholdValues());
        // If two cases analysis should be performed
        if (dataPane.isTwoCasesAnalysisSelected())
            setTwoCasesSampleInfoFile(dataPane.getTwoCasesSampleInfoFile());
        else {
            // Need to remove the originally set sample information
            setTwoCasesSampleInfoFile(null);
            FactorGraphRegistry.getRegistry().setNumberOfPermtation(dataPane.getNumberOfPermutation());
        }
            
        InferenceAlgorithmPane algPane = dialog.getAlgorithmPane();
        setAlgorithms(algPane.getSelectedAlgorithms());
    }

    /**
     * This is the actual place where the factor graph based analysis is performed.
     */
    protected void runFactorGraphAnalysis() {
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        if (pathwayId == null || pathwayDiagram == null) {
            JOptionPane.showMessageDialog(frame,
                                          "Both pathwayId and pathwayDiagram should be provided.", 
                                          "Not Enough Information",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        ProgressPane progressPane = initializeProgressPane(frame);
        // Convert to a FactorGraph using this object
        DiagramAndFactorGraphSwitcher switcher = new DiagramAndFactorGraphSwitcher();
        try {
            progressPane.setText("Converting pathway into graphical model...");
            FactorGraph factorGraph = switcher.convertPathwayToFactorGraph(pathwayId, pathwayDiagram);
            if (factorGraph == null) {
                progressPane.setVisible(false);
                return; // Something may be wrong
            }
            
            if(!loadEvidences(factorGraph, progressPane))
                return; // Something wrong during loading
            
            performInference(factorGraph,
                             progressPane);
            
            progressPane.setText("Analysis is done!");
            progressPane.setVisible(false);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(frame,
                                          "Error in graphical model analysis: " + e,
                                          "Error in Graphical Model Analysis",
                                          JOptionPane.ERROR_MESSAGE);
            progressPane.setVisible(false);
            e.printStackTrace();
        }
    }

    private void performInference(FactorGraph factorGraph,
                                  ProgressPane progressPane) throws Exception {
        progressPane.setTitle("Perform inference...");
        InferenceRunner inferenceRunner = new InferenceRunner();
        inferenceRunner.setFactorGraph(factorGraph);
        inferenceRunner.setUsedForTwoCases(getSampleInfoFile() != null);
        inferenceRunner.setProgressPane(progressPane);
        inferenceRunner.setAlgorithms(FactorGraphRegistry.getRegistry().getLoadedAlgorithms());
        inferenceRunner.setHiliteControlPane(this.hiliteControlPane);
        // Now call for inference
        inferenceRunner.performInference(true);
    }

    protected boolean loadEvidences(FactorGraph factorGraph,
                                    ProgressPane progressPane) throws Exception {
        progressPane.setText("Loading observation data...");
        ObservationDataHelper dataHelper = new ObservationDataHelper(factorGraph);
        boolean correct = dataHelper.performLoadData(cnvFile,
                                                     cnvThresholdValues, 
                                                     geneExpFile, 
                                                     geneExpThresholdValues,
                                                     getSampleInfoFile(),
                                                     progressPane);
        if (!correct) {
            progressPane.setText("Wrong in data loading.");
            progressPane.setVisible(false);
            return false;
        }
        progressPane.setText("Data loading is done.");
        return true;
    }

    protected ProgressPane initializeProgressPane(JFrame frame) {
        ProgressPane progressPane = new ProgressPane();
        progressPane.setTitle("Run Graphical Model Analysis");
        progressPane.setIndeterminate(true);
        frame.setGlassPane(progressPane);
        frame.getGlassPane().setVisible(true);
        return progressPane;
    }
    
    /**
     * Call this method to display a saved results from a file.
     * @param fgResults
     */
    public void showInferenceResults(FactorGraphInferenceResults fgResults) throws MathException {
        if (pathwayDiagram == null || hiliteControlPane == null)
            return; // Cannot do anything here
        InferenceResultsControl control = new InferenceResultsControl();
        control.setHiliteControlPane(hiliteControlPane);
        control.showInferenceResults(fgResults);
        FactorGraphRegistry.getRegistry().registerDiagramToFactorGraph(pathwayDiagram, fgResults.getFactorGraph());
        FactorGraphRegistry.getRegistry().registerInferenceResults(fgResults);
    }
}