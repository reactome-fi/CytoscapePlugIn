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
import org.reactome.cytoscape.genescore.GeneScoreOverlayHelper;
import org.reactome.cytoscape.mechismo.MechismoDataFetcher;
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
    // For GSEA auto scoring
    private Map<String, Double> geneToScore;
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
    
    /**
     * Reset the cached data so that nothing to be highlighted for new pathway diagram.
     */
    public void reset() {
        geneToScore = null;
        pathwayToAnnotation = null;
    }
    
    public void setPathwayToAnnotation(Map<String, GeneSetAnnotation> annotations) {
        this.pathwayToAnnotation = annotations;
        this.geneToScore = null;
    }
    
    public Map<String, GeneSetAnnotation> getPathwayToAnnotation() {
        return this.pathwayToAnnotation;
    }
    
    public void setGeneToScore(Map<String, Double> geneToScore) {
        this.geneToScore = geneToScore;
        // Null other type of results
        this.pathwayToAnnotation = null;
    }
    
    /**
     * Check if there is any need to highlight a new displayed pathway diagram.
     * @return
     */
    public boolean isEmpty() {
        if ((pathwayToAnnotation == null || pathwayToAnnotation.size() == 0) &&
            (geneToScore == null || geneToScore.size() == 0) &&
            !MechismoDataFetcher.isMechismoDataLoaded())
            return true;
        return false;
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
                if (annotation.getHitIds() != null)
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
        if (isEmpty()) 
            return; // Do nothing
        if (pathwayFrame == null || eventName == null)
            return;
        // This is more like a hack. A comprehensive pathway hilite manager is still needed.
        if (MechismoDataFetcher.isMechismoDataLoaded())
            MechismoDataFetcher.loadMechismoResults(pathwayFrame.getZoomablePathwayEditor());
        // Try pathwayToAnnotation first
        else if (pathwayToAnnotation != null) {
            GeneSetAnnotation annotation = pathwayToAnnotation.get(eventName);
            if (annotation == null)
                return;
            List<String> hitGenes = annotation.getHitIds();
            highlightPathway(pathwayFrame, hitGenes);
        }
        else if (geneToScore != null) {
            GeneScoreOverlayHelper scoreHelper = new GeneScoreOverlayHelper();
            scoreHelper.overlayGeneScores(geneToScore,
                                          pathwayFrame.getPathwayEditor(), 
                                          pathwayFrame.getZoomablePathwayEditor().getHighlightControlPane());
        }
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
                taskMonitor.setProgress(0.0d);
                CyPathwayDiagramHelper helper = CyPathwayDiagramHelper.getHelper();
                String genes = StringUtils.join(",", hitGenes);
                helper.highlightPathwayDiagram(pathwayEditor.getPathwayEditor(), 
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
        // Check if we need to do more for gene score overlaying
        if (geneToScore != null && geneToScore.size() > 0) {
            GeneScoreOverlayHelper scoreHelper = new GeneScoreOverlayHelper();
            scoreHelper.removeGeneScores(pathwayFrame.getZoomablePathwayEditor().getHighlightControlPane());
        }
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
