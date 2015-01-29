/*
 * Created on Jan 26, 2015
 *
 */
package org.reactome.cytoscape.pathway;

import java.io.File;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.gk.render.RenderablePathway;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.pgm.InferenceRunner;
import org.reactome.cytoscape.pgm.ObservationDataHelper;
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
    // Information entered by the user
    private File geneExpFile;
    private double[] geneExpThresholdValues;
    private File cnvFile;
    private double[] cnvThresholdValues;
    // If this file is not null, two-cases analysis should be performed
    private File sampleInfoFile;
    private List<Inferencer> algorithms;
    
    /**
     * Default constructor.
     */
    public FactorGraphAnalyzer() {
    }
    
    public File getSampleInfoFile() {
        return sampleInfoFile;
    }

    public void setTwoCasesSampleInfoFile(File sampleInfoFile) {
        this.sampleInfoFile = sampleInfoFile;
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
        this.algorithms = algorithms;
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
        frame.getGlassPane().setVisible(true);
        // Convert to a FactorGraph using this object
        DiagramAndFactorGraphSwitcher switcher = new DiagramAndFactorGraphSwitcher();
        try {
            progressPane.setText("Converting pathway into graphical model...");
            FactorGraph factorGraph = switcher.convertPathwayToFactorGraph(pathwayId, pathwayDiagram);
            if (factorGraph == null) {
                progressPane.setVisible(false);
                return; // Something may be wrong
            }
            
            progressPane.setText("Loading observation data...");
            ObservationDataHelper dataHelper = new ObservationDataHelper(factorGraph);
            boolean correct = dataHelper.performLoadData(cnvFile,
                                                         cnvThresholdValues, 
                                                         geneExpFile, 
                                                         geneExpThresholdValues,
                                                         sampleInfoFile,
                                                         progressPane);
            if (!correct) {
                progressPane.setText("Wrong in data loading.");
                progressPane.setVisible(false);
                return;
            }
            progressPane.setText("Data loading is done.");
            
            progressPane.setTitle("Perform inference...");
            InferenceRunner inferenceRunner = new InferenceRunner();
            inferenceRunner.setFactorGraph(factorGraph);
            inferenceRunner.setUsedForTwoCases(sampleInfoFile != null);
            inferenceRunner.setProgressPane(progressPane);
            inferenceRunner.setAlgorithms(algorithms);
            inferenceRunner.performInference(true);
            
            progressPane.setText("Analysis is done!");
            progressPane.setVisible(false);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(frame,
                                          "Error in graphical model analysis: " + e,
                                          "Error in Graphical Model Analysis",
                                          JOptionPane.ERROR_MESSAGE);
            progressPane.setVisible(false);
            System.err.println(e);
        }
    }
    
}