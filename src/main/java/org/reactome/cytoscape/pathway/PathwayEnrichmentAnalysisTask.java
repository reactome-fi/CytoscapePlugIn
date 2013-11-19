/*
 * Created on Nov 18, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.io.StringReader;
import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.reactome.annotate.ModuleGeneSetAnnotation;
import org.reactome.cytoscape.service.RESTFulFIService;

/**
 * @author gwu
 *
 */
public class PathwayEnrichmentAnalysisTask extends AbstractTask {
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
        ModuleGeneSetAnnotation annotation = annotations.get(0); // There should be only one annotation here
        eventPane.showPathwayEnrichments(annotation.getAnnotations());
        taskMonitor.setProgress(1.0d);
    }
    
}
