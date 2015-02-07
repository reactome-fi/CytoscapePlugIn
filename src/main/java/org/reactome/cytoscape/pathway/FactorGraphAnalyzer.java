/*
 * Created on Jan 26, 2015
 *
 */
package org.reactome.cytoscape.pathway;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.commons.math.MathException;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableInteraction;
import org.gk.render.RenderablePathway;
import org.gk.render.RenderableReaction;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.pgm.FactorGraphInferenceResults;
import org.reactome.cytoscape.pgm.FactorGraphRegistry;
import org.reactome.cytoscape.pgm.InferenceRunner;
import org.reactome.cytoscape.pgm.ObservationDataHelper;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Inferencer;
import org.reactome.factorgraph.Variable;

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
                                                         getSampleInfoFile(),
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
            // Get the set of output variables for results analysis
            Set<Variable> pathwayVars = getPathwayVars(factorGraph, pathwayDiagram);
            inferenceRunner.setPathwayVars(pathwayVars);
            Set<Variable> outputVars = getOutputVariables(factorGraph, pathwayDiagram);
            inferenceRunner.setOutputVars(outputVars);
            inferenceRunner.setUsedForTwoCases(getSampleInfoFile() != null);
            inferenceRunner.setProgressPane(progressPane);
            inferenceRunner.setAlgorithms(FactorGraphRegistry.getRegistry().getLoadedAlgorithms());
            inferenceRunner.setHiliteControlPane(this.hiliteControlPane);
            // Now call for inference
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
            e.printStackTrace();
        }
    }
    
    /**
     * Call this method to display a saved results from a file.
     * @param fgResults
     */
    public void showInferenceResults(FactorGraphInferenceResults fgResults) throws MathException {
        if (pathwayDiagram == null || hiliteControlPane == null)
            return; // Cannot do anything here
        FactorGraph factorGraph = fgResults.getFactorGraph();
        InferenceRunner inferenceRunner = new InferenceRunner();
        inferenceRunner.setFactorGraph(factorGraph);
        // Get the set of output variables for results analysis
        Set<Variable> pathwayVars = getPathwayVars(factorGraph, pathwayDiagram);
        inferenceRunner.setPathwayVars(pathwayVars);
        Set<Variable> outputVars = getOutputVariables(factorGraph, pathwayDiagram);
        inferenceRunner.setOutputVars(outputVars);
        inferenceRunner.setUsedForTwoCases(fgResults.getSampleToType() != null);
        inferenceRunner.setHiliteControlPane(hiliteControlPane);
        inferenceRunner.showInferenceResults(fgResults);
    }
    
    private Set<Variable> getPathwayVars(FactorGraph fg, RenderablePathway diagram) {
        // Get output ids from diagram diagram
        Set<String> reactomeIds = new HashSet<String>();
        for (Object o : diagram.getComponents()) {
            Renderable r = (Renderable) o;
            if (r.getReactomeId() == null || !(r instanceof Node))
                continue; // Nothing to be done
            reactomeIds.add(r.getReactomeId() + "");
        }
        Set<Variable> pathwayVars = new HashSet<Variable>();
        // If a variable's reactome id is in this list, it should be a output
        for (Variable var : fg.getVariables()) {
            if (var.getCustomizedInfo() == null)
                continue;
            if (reactomeIds.contains(var.getCustomizedInfo()))
                pathwayVars.add(var);
        }
        return pathwayVars;
    }
    
    private Set<Variable> getOutputVariables(FactorGraph fg, RenderablePathway diagram) {
        // Get output ids from diagram diagram
        Set<String> outputIds = new HashSet<String>();
        for (Object o : diagram.getComponents()) {
            Renderable r = (Renderable) o;
            if (r.getReactomeId() == null)
                continue; // Nothing to be done
            if (r instanceof RenderableReaction ||
                r instanceof RenderableInteraction) {
                HyperEdge edge = (HyperEdge) r;
                List<Node> outputs = edge.getOutputNodes();
                if (outputs != null) {
                    for (Node output : outputs) {
                        if (output.getReactomeId() != null)
                            outputIds.add(output.getReactomeId() + "");
                    }
                }
            }
        }
        Set<Variable> outputVar = new HashSet<Variable>();
        // If a variable's reactome id is in this list, it should be a output
        for (Variable var : fg.getVariables()) {
            if (var.getCustomizedInfo() == null)
                continue;
            if (outputIds.contains(var.getCustomizedInfo()))
                outputVar.add(var);
        }
        return outputVar;
    }
    
}