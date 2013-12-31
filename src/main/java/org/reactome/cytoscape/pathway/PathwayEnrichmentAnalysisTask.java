/*
 * Created on Nov 18, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.gk.util.ProgressPane;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author gwu
 *
 */
public class PathwayEnrichmentAnalysisTask extends AbstractTask {
    private static Logger logger = LoggerFactory.getLogger(PathwayEnrichmentAnalysisTask.class);
    private String geneList;
    private EventTreePane eventPane;
    
    /**
     * Default constructor.
     */
    public PathwayEnrichmentAnalysisTask() {
    }
    
    public void setGeneList(String geneList) {
        this.geneList = geneList;
    }
    
    public void setEventPane(EventTreePane treePane) {
        this.eventPane = treePane;
    }
    
    /**
     * Use a glass panel based way to do progress monitoring to avoid part text problem
     * in the event tree.
     * NOTE: This doesn't help at all!
     * @param progressPane
     * @throws Exception
     */
    public void doEnrichmentAnalysis() {
        ProgressPane progressPane = new ProgressPane();
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        frame.setGlassPane(progressPane);
        progressPane.setTitle("Pathway Enrichment Analysis");
        progressPane.setMinimum(0);
        progressPane.setMaximum(100);
        progressPane.setVisible(true);
        if (geneList == null) {
            progressPane.setText("No gene list is provided!");
            progressPane.setValue(100);
            return;
        }
        progressPane.setValue(0);        
        progressPane.setText("Do enrichment analysis...");
        // This is just for test by query pathway diagram for Cell Cycle Checkpoints 
        RESTFulFIService service = new RESTFulFIService();
        try {
            List<ModuleGeneSetAnnotation> annotations = service.annotateGeneSetWithReactomePathways(geneList);
            
            progressPane.setValue(75);
            progressPane.setText("Show enrichment results...");
            ModuleGeneSetAnnotation annotation = annotations.get(0); // There should be only one annotation here
            eventPane.showPathwayEnrichments(annotation.getAnnotations());
            progressPane.setValue(100);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(progressPane,
                                          "Error in enrichment analysis: " + e.getMessage(),
                                          "Error",
                                          JOptionPane.ERROR_MESSAGE);
            logger.error("doEnrichmentAnalysis: " + e.getMessage(), e);
        }
        frame.getGlassPane().setVisible(false);
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Pathway Enrichment Analysis");
        if (geneList == null) {
            taskMonitor.setStatusMessage("No gene list is provided!");
            taskMonitor.setProgress(1.0d);
            return; // Nothing to be displayed!
        }
        taskMonitor.setProgress(0);
        taskMonitor.setStatusMessage("Do enrichment analysis...");
        // This is just for test by query pathway diagram for Cell Cycle Checkpoints 
        RESTFulFIService service = new RESTFulFIService();
        List<ModuleGeneSetAnnotation> annotations = service.annotateGeneSetWithReactomePathways(geneList);
        taskMonitor.setProgress(0.75d);
        taskMonitor.setStatusMessage("Show enrichment results...");
        final ModuleGeneSetAnnotation annotation = annotations.get(0); // There should be only one annotation here
        // Need to use a new thread to make sure all text in the tree can be displayed without truncation.
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                eventPane.showPathwayEnrichments(annotation.getAnnotations());
            }
        });
        taskMonitor.setProgress(1.0d);
    }
    
}
