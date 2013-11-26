/*
 * Created on Nov 25, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.util.Map;

import javax.swing.JComponent;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.gk.gkEditor.ZoomablePathwayEditor;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.GraphEditorActionEvent.ActionType;
import org.gk.util.StringUtils;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.cytoscape.service.CyPathwayDiagramHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This class is used to handle pathway enrichment analysis and its display in both pathway
 * tree and displayed pathway diagram.
 * @author gwu
 *
 */
public class PathwayEnrichmentHighlighter {
    // Pathway Enrichment results cached for display
    private Map<String, GeneSetAnnotation> pathwayToAnnotation;
    
    /**
     * Default constructor. Usually there should be only one shared instance in the whole
     * app. However, singleton pattern is not used here.
     */
    public PathwayEnrichmentHighlighter() {
    }
    
    public void setPathwayToAnnotation(Map<String, GeneSetAnnotation> annotations) {
        this.pathwayToAnnotation = annotations;
    }
    
    public Map<String, GeneSetAnnotation> getPathwayToAnnotation() {
        return this.pathwayToAnnotation;
    }
    
    public void highlightPathways(final PathwayInternalFrame pathwayFrame,
                                  String eventName) {
        if (pathwayFrame == null || eventName == null)
            return;
        final GeneSetAnnotation annotation = pathwayToAnnotation.get(eventName);
        if (annotation == null)
            return;
        AbstractTask task = new AbstractTask() {
            
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                taskMonitor.setTitle("Pathway Highlighting");
                taskMonitor.setStatusMessage("Highlight pathway...");
                CyPathwayDiagramHelper helper = CyPathwayDiagramHelper.getHelper();
                String genes = StringUtils.join(",", annotation.getHitIds());
                ZoomablePathwayEditor pathwayEditor = pathwayFrame.getZoomablePathwayEditor();
                helper.highlightPathwayDiagram(pathwayEditor, 
                                               genes);
                PathwayEditor editor = pathwayEditor.getPathwayEditor();
                editor.repaint(editor.getVisibleRect());
                // Warning: New and old values are arbitray here. The listener should not
                // do anything with these values!!!
                //parentComp.firePropertyChange("pathwayRepaint", false, true);
                // Since there is no HIGHLIGHT ActionType, using SELECTION instead
                // to make repaint consistent.
                editor.fireGraphEditorActionEvent(ActionType.SELECTION);
                taskMonitor.setProgress(1.0d);
                pathwayFrame.setHitGenes(annotation.getHitIds());
            }
        };
        @SuppressWarnings("rawtypes")
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        taskManager.execute(new TaskIterator(task));
    }

}
