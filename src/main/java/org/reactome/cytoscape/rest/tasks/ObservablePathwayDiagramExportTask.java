package org.reactome.cytoscape.rest.tasks;

import java.awt.Dimension;
import java.io.File;
import java.util.Collections;
import java.util.List;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ObservableTask;
import org.cytoscape.work.TaskMonitor;
import org.gk.graphEditor.PathwayEditor;
import org.gk.util.StringUtils;
import org.gk.util.SwingImageCreator;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.cytoscape.pathway.PathwayEnrichmentHighlighter;
import org.reactome.cytoscape.pathway.ReactomeRESTfulService;
import org.reactome.cytoscape.service.CyPathwayDiagramHelper;
import org.reactome.cytoscape.util.PlugInUtilities;

public class ObservablePathwayDiagramExportTask extends AbstractTask implements ObservableTask {
    private Long pathwayId;
    private String pathwayName;
    private String fileName;
    
    public ObservablePathwayDiagramExportTask(Long pathwayId,
                                              String pathwayName,
                                              String fileName) {
        this.pathwayId = pathwayId;
        this.pathwayName = pathwayName;
        this.fileName = fileName;
    }
    
    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Load Pathway");
        if (pathwayId == null) {
            taskMonitor.setStatusMessage("No pathway id is specifcied!");
            taskMonitor.setProgress(1.0d);
            return; // Nothing to be displayed!
        }
        taskMonitor.setProgress(0);
        taskMonitor.setStatusMessage("Loading pathway diagram...");
        String text = ReactomeRESTfulService.getService().pathwayDiagram(pathwayId);
        taskMonitor.setProgress(0.50d);
        taskMonitor.setStatusMessage("Render pathway diagram...");
        PathwayEditor pathwayEditor = new PathwayEditor();
        pathwayEditor.setHidePrivateNote(true);
        PlugInUtilities.setPathwayDiagramInXML(pathwayEditor, text);
        // Check if highlight is needed
        taskMonitor.setProgress(0.8d);
        highlightPathway(pathwayEditor, taskMonitor);
        // Before export, force repaint to make sure all objects are drawn correctly
        Dimension size = pathwayEditor.getPreferredSize();
        pathwayEditor.repaint(0, 0, size.width, size.height);
        SwingImageCreator.exportImageInPDF(pathwayEditor, new File(fileName));
        taskMonitor.setProgress(1.0d);
    }
    
    //TODO: The following method is collected from PathwayEnrichmentHighligher. A refactoring is needed in
    // the future to avoid code duplication.
    private void highlightPathway(PathwayEditor pathwayEditor, TaskMonitor taskMonitor) {
        // Try to highlight pathway based on enrichment results first
        PathwayEnrichmentHighlighter hiliter = PathwayEnrichmentHighlighter.getHighlighter();
        if (hiliter.isEmpty()) {
            return;
        }
        GeneSetAnnotation annotation = hiliter.getPathwayToAnnotation().get(pathwayName);
        if (annotation == null)
            return;
        List<String> hitGenes = annotation.getHitIds();
        taskMonitor.setStatusMessage("Highlight pathway...");
        CyPathwayDiagramHelper helper = CyPathwayDiagramHelper.getHelper();
        String genes = StringUtils.join(",", hitGenes);
        helper.highlightPathwayDiagram(pathwayEditor, 
                                       genes);
    }
    
    @Override
    public <R> R getResults(Class<? extends R> type) {
        if (type.equals(String.class))
            return (R) fileName;
        return null;
    }

    @Override
    public List<Class<?>> getResultClasses() {
        return Collections.singletonList(String.class);
    }
    
}
