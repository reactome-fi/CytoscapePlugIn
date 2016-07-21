/*
 * Created on Nov 25, 2013
 *
 */
package org.reactome.cytoscape.pathway;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.gk.graphEditor.GraphEditorActionEvent.ActionType;
import org.gk.graphEditor.PathwayEditor;
import org.gk.util.StringUtils;
import org.reactome.annotate.GeneSetAnnotation;
import org.reactome.cytoscape.service.CyPathwayDiagramHelper;
import org.reactome.cytoscape.service.TableHelper;
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
    // The singleton
    private static PathwayEnrichmentHighlighter highlighter;
    
    /**
     * Default constructor. Usually there should be only one shared instance in the whole
     * app. 
     */
    private PathwayEnrichmentHighlighter() {
    }
    
    public static PathwayEnrichmentHighlighter getHighlighter() {
        if (highlighter == null)
            highlighter = new PathwayEnrichmentHighlighter();
        return highlighter;
    }
    
    public void setPathwayToAnnotation(Map<String, GeneSetAnnotation> annotations) {
        this.pathwayToAnnotation = annotations;
    }
    
    public Map<String, GeneSetAnnotation> getPathwayToAnnotation() {
        return this.pathwayToAnnotation;
    }
    
    /**
     * Get all hit genes in a single set.
     * @return
     */
    public Set<String> getHitGenes() {
        Set<String> set = new HashSet<String>();
        if (pathwayToAnnotation != null) {
            for (String pathway : pathwayToAnnotation.keySet()) {
                GeneSetAnnotation annotation = pathwayToAnnotation.get(pathway);
                set.addAll(annotation.getHitIds());
            }
        }
        return set;
    }
    
    /**
     * Since a RESTful API is needed to be called for this method, eventName is used in order to get
     * a short list of genes for performance consideration.
     * @param pathwayFrame
     * @param eventName
     */
    public void highlightPathway(PathwayInternalFrame pathwayFrame,
                                 String eventName) {
        if (pathwayFrame == null || eventName == null)
            return;
        final GeneSetAnnotation annotation = pathwayToAnnotation.get(eventName);
        if (annotation == null)
            return;
        List<String> hitGenes = annotation.getHitIds();
        highlightPathway(pathwayFrame, hitGenes);
    }

    public void highlightPathway(PathwayInternalFrame pathwayFrame,
                                 List<String> hitGenes) {
        highlightPathway(pathwayFrame.getZoomablePathwayEditor(), 
                         hitGenes);
    }
    
    public void highlightPathway(CyZoomablePathwayEditor pathwayEditor) {
        List<String> geneList = new ArrayList<String>(getHitGenes());
        highlightPathway(pathwayEditor, geneList);
    }
    
    private void highlightPathway(final CyZoomablePathwayEditor pathwayEditor,
                                  final List<String> hitGenes) {
        // No need
        if (hitGenes == null || hitGenes.size() == 0)
            return; // Nothing to highlight
        AbstractTask task = new AbstractTask() {
            
            @Override
            public void run(TaskMonitor taskMonitor) throws Exception {
                taskMonitor.setTitle("Pathway Highlighting");
                taskMonitor.setStatusMessage("Highlight pathway...");
                CyPathwayDiagramHelper helper = CyPathwayDiagramHelper.getHelper();
                String genes = StringUtils.join(",", hitGenes);
                helper.highlightPathwayDiagram(pathwayEditor, 
                                               genes);
                PathwayEditor editor = pathwayEditor.getPathwayEditor();
                editor.repaint(editor.getVisibleRect());
                // Since there is no HIGHLIGHT ActionType, using SELECTION instead
                // to make repaint consistent.
                editor.fireGraphEditorActionEvent(ActionType.SELECTION);
                taskMonitor.setProgress(1.0d);
            }
        };
        @SuppressWarnings("rawtypes")
        TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
        taskManager.execute(new TaskIterator(task));
    }
    
    public void removeHighlightPathway(PathwayInternalFrame pathwayFrame) {
        CyZoomablePathwayEditor pathwayEditor = pathwayFrame.getZoomablePathwayEditor();
        pathwayEditor.resetColors();
    }
    
    public void removeHighlightNewtork(CyNetwork network) {
        // Create a map with false values
        Map<Long, Boolean> geneToValue = new HashMap<Long, Boolean>();
        for (CyNode node : network.getNodeList()) {
            // Use null instead of false to remove the original true
            geneToValue.put(node.getSUID(), null);
        }
        new TableHelper().storeNodeAttributesBySUID(network, "isHitGene", geneToValue);
    }
    
    /**
     * A helper method to highlight a set of genes displayed in a FI network view. The caller should
     * call update view by itself to repaint the network view.
     * @param network
     * @param hitGenes
     */
    public void highlightNework(CyNetwork network,
                                Collection<String> hitGenes) {
        TableHelper tableHelper = new TableHelper();
        // Set up hit genes
        if (hitGenes != null && hitGenes.size() > 0) {
            Map<String, Boolean> geneToIsHitMap = new HashMap<String, Boolean>();
            for (String gene : hitGenes)
                geneToIsHitMap.put(gene, Boolean.TRUE);
            tableHelper.storeNodeAttributesByName(network, 
                                                  "isHitGene",
                                                  geneToIsHitMap);
        }
    }

}
