/*
 * Created on Feb 11, 2015
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.math.MathException;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.pgm.FactorGraphInferenceResults;
import org.reactome.cytoscape.pgm.FactorGraphRegistry;
import org.reactome.cytoscape.pgm.InferenceRunner;
import org.reactome.cytoscape.pgm.InferenceStatus;
import org.reactome.cytoscape.pgm.PathwayResultSummary;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.FactorGraph;
import org.reactome.factorgraph.Variable;
import org.reactome.r3.util.JAXBBindableList;

/**
 * This class is used to perform a batch factor graph analysis.
 * @author gwu
 *
 */
public class FactorGraphBatchAnalyzer extends FactorGraphAnalyzer {
    private EventTreePane eventPane;
    
    /**
     * Default constructor.
     */
    public FactorGraphBatchAnalyzer() {
    }

    public EventTreePane getEventPane() {
        return eventPane;
    }

    public void setEventPane(EventTreePane eventPane) {
        this.eventPane = eventPane;
    }

    @Override
    protected void runFactorGraphAnalysis() {
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        ProgressPane progressPane = initializeProgressPane(frame);
        try {
            progressPane.setText("Fetching factor graphs...");
            List<FactorGraph> factorGraphs = fetchFactorGraphs();
            progressPane.setText("Total models: " + factorGraphs.size());
            progressPane.setMaximum(factorGraphs.size());
            progressPane.setMinimum(1);
            progressPane.setIndeterminate(false);
            int count = 1;
            progressPane.setTitle("Perform inference...");
            InferenceRunner inferenceRunner = getInferenceRunner(progressPane);
            List<PathwayResultSummary> resultList = new ArrayList<PathwayResultSummary>();
            for (FactorGraph fg : factorGraphs) {
                progressPane.setText("Analyzing " + fg.getName());
                progressPane.setValue(count);
                PathwayResultSummary result = runFactorGraphAnalysis(fg, 
                                                                     inferenceRunner,
                                                                     progressPane);
                if (result != null)
                    resultList.add(result);
                if (inferenceRunner.getStatus() == InferenceStatus.ABORT ||
                    inferenceRunner.getStatus() == InferenceStatus.ERROR) {
                    break; // Aborted or an error thrown
                }
                count ++;
//                if (count == 11)
//                    break;
            }
            showResults(resultList);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                                          "Error in performing batch graphical model analysis: " + e,
                                          "Error in Analysis",
                                          JOptionPane.ERROR_MESSAGE);
        }
        finally {
            frame.getGlassPane().setVisible(false);
        }
    }
    
    public void showResults(List<PathwayResultSummary> resultList) {
        String title = "Pathway PGM Analysis";
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(CytoPanelName.SOUTH);
        int index = PlugInUtilities.getCytoPanelComponent(tableBrowserPane, title);
        FactorGraphBatchResultPane resultPane = null;
        if (index > -1)
            resultPane = (FactorGraphBatchResultPane) tableBrowserPane.getComponentAt(index);
        else
            resultPane = new FactorGraphBatchResultPane(eventPane, title);
        resultPane.setResults(resultList);
        PlugInObjectManager.getManager().selectCytoPane(resultPane, CytoPanelName.SOUTH);
    }
    
    private PathwayResultSummary runFactorGraphAnalysis(FactorGraph factorGraph,
                                                        InferenceRunner runner,
                                                        ProgressPane progressPane) throws Exception {
        if(!loadEvidences(factorGraph, progressPane))
            return null; // Something wrong during loading
        performInference(factorGraph,
                         runner,
                         progressPane);
        PathwayResultSummary resultSummary = collectResults(factorGraph);
        FactorGraphRegistry.getRegistry().clearData(factorGraph);
        return resultSummary;
    }
    
    private PathwayResultSummary collectResults(FactorGraph fg) throws MathException {
        FactorGraphInferenceResults fgResults = FactorGraphRegistry.getRegistry().getInferenceResults(fg);
        PathwayResultSummary resultSummary = new PathwayResultSummary();
        resultSummary.setResults(fgResults,
                                 getOutputVariables(fg));
        return resultSummary;
    }
    
    private InferenceRunner getInferenceRunner(ProgressPane progressPane) {
        final InferenceRunner inferenceRunner = new InferenceRunner();
        inferenceRunner.setUsedForTwoCases(getSampleInfoFile() != null);
        inferenceRunner.setAlgorithms(FactorGraphRegistry.getRegistry().getLoadedAlgorithms());
        inferenceRunner.setProgressPane(progressPane);
        progressPane.enableCancelAction(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                inferenceRunner.abort();
            }
        });
        return inferenceRunner;
    }
    
    private void performInference(FactorGraph factorGraph,
                                  InferenceRunner inferenceRunner,
                                  ProgressPane progressPane) throws Exception {
        String name = factorGraph.getName();
        int index = name.indexOf("]");
        name = name.substring(index + 1).trim();
        progressPane.setTitle(name);
        inferenceRunner.setFactorGraph(factorGraph);
        // Get the set of output variables for results analysis
        Set<Variable> pathwayVars = getPathwayVars(factorGraph);
        inferenceRunner.setPathwayVars(pathwayVars);
        Set<Variable> outputVars = getOutputVariables(factorGraph);
        inferenceRunner.setOutputVars(outputVars);
        // Now call for inference
        inferenceRunner.performInference();
    }

    private List<FactorGraph> fetchFactorGraphs() throws Exception {
        String hostURL = PlugInObjectManager.getManager().getHostURL();
        String fileUrl = hostURL + "Cytoscape/PathwayDiagramsFactorGraphs.xml.zip";
        URL url = new URL(fileUrl);
        InputStream is = url.openStream();
        BufferedInputStream bis = new BufferedInputStream(is);
        ZipInputStream zis = new ZipInputStream(bis);
        JAXBContext context = JAXBContext.newInstance(JAXBBindableList.class, FactorGraph.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        ZipEntry entry = zis.getNextEntry(); // Have to call this method
        @SuppressWarnings("unchecked")
        JAXBBindableList<FactorGraph> list = (JAXBBindableList<FactorGraph>) unmarshaller.unmarshal(zis);
        return list.getList();
    }
}
