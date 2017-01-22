/*
 * Created on Dec 21, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.gk.graphEditor.PathwayEditor;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;
import org.gk.util.ProgressPane;
import org.jdom.Element;
import org.reactome.cytoscape.service.CyPathwayEditor;
import org.reactome.cytoscape.service.FIRenderableInteraction;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;

import edu.ohsu.bcb.druggability.ExpEvidence;
import edu.ohsu.bcb.druggability.Interaction;

/**
 * A singleton to manage a list of drug/target interactions so that interactions will not be duplicated after loading
 * from the server.
 * @author gwu
 *
 */
public class DrugTargetInteractionManager {
    private static DrugTargetInteractionManager manager;
    // Cached filter
    private InteractionFilter interactionFilter;
    // Cached values
    private Map<Long, List<Interaction>> peID2Interactions;
    
    /**
     * Default constructor.
     */
    protected DrugTargetInteractionManager() {
        this.interactionFilter = new InteractionFilter();
        peID2Interactions = new HashMap<>();
    }
    
    public static DrugTargetInteractionManager getManager() {
        if (manager == null)
            manager = new DrugTargetInteractionManager();
        return manager;
    }
    
    public InteractionFilter getInteractionFilter() {
        return this.interactionFilter;
    }
    
    private List<Interaction> getInteractions(Long peId,
                                              RenderablePathway pathway) {
        List<Interaction> interactions = peID2Interactions.get(peId);
        if (interactions != null)
            return interactions;
        Map<Long, List<Interaction>> dbIdToInteractions = fetchCancerDrugs(peId, 
                                                                           pathway);
        interactions = dbIdToInteractions.get(peId);
        if (interactions == null)
            interactions = new ArrayList<>();
        peID2Interactions.put(peId, interactions);
        return interactions;
    }
    
    public void filterCancerDrugs(CyPathwayEditor pathwayEditor) {
        if (peID2Interactions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "There is no drug fetched. Fetch drugs first before filtering.",
                                          "No Drug for Filtering",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        interactionFilter.setPathwayEditor(pathwayEditor);
        interactionFilter.showDialog();
    }
    
    @SuppressWarnings("unchecked")
    public void applyFilter(CyPathwayEditor pathwayEditor) {
        List<Renderable> selected = pathwayEditor.getSelection();
        if (selected != null && selected.size() == 1) { // For one single selected entity
            _applyFilterToSelected(pathwayEditor, selected);
        }
        else { // Apply for all 
            _applyFilter(pathwayEditor);
        }
    }

    private void _applyFilterToSelected(CyPathwayEditor pathwayEditor, List<Renderable> selected) {
        Long peId = null;
        for (Renderable r : selected) {
            if ((r instanceof Node) && (r.getReactomeId() != null)) {
                peId = r.getReactomeId();
                break;
            }
        }
        if (peId != null) {
            List<Interaction> filtered = applyFilter(pathwayEditor, peId);
            if (filtered != null && filtered.size() > 0) {
                DiagramDrugTargetInteractionHandler handler = new DiagramDrugTargetInteractionHandler(pathwayEditor);
                handler.displayInteractions(filtered);
            }
        }
    }

    private void _applyFilter(CyPathwayEditor pathwayEditor) {
        // Get a list of ids that can be filtered
        List<Long> idsToBeFiltered = new ArrayList<>();
        for (Object obj : pathwayEditor.getDisplayedObjects()) {
            if (obj instanceof Node) {
                Node node = (Node) obj;
                if (node.getReactomeId() != null) {
                    List<Interaction> interactions = peID2Interactions.get(node.getReactomeId());
                    if (interactions != null && interactions.size() > 0)
                        idsToBeFiltered.add(node.getReactomeId());
                }
            }
        }
        // Have to do the second step to avoid concurrent exception
        Map<Long, List<Interaction>> dbIdToFiltered = new HashMap<>();
        for (Long dbId : idsToBeFiltered) {
            List<Interaction> filtered = applyFilter(pathwayEditor, dbId);
            if (filtered != null && filtered.size() > 0)
                dbIdToFiltered.put(dbId, filtered);
        }
        if (dbIdToFiltered.size() > 0) {
            DiagramDrugTargetInteractionHandler handler = new DiagramDrugTargetInteractionHandler(pathwayEditor);
            handler.displayInteractions(dbIdToFiltered);
        }
    }
    
    private List<Interaction> applyFilter(CyPathwayEditor pathwayEditor, Long peId) {
        List<Interaction> interactions = peID2Interactions.get(peId);
        if (interactions == null || interactions.size() == 0)
            return null; // Nothing to be done
        List<Interaction> filtered = applyFilter(interactions);
        // Check displayed interactions
        List<FIRenderableInteraction> toBeDeleted = new ArrayList<>();
        for (Object r : pathwayEditor.getDisplayedObjects()) {
            if (!(r instanceof Node) || ((Node)r).getReactomeId() != peId)
                continue;
            Node node = (Node) r;
            List<HyperEdge> connected = node.getConnectedReactions();
            if (connected.size() == 0)
                break;
            for (HyperEdge edge : connected) {
                if (!(edge instanceof DrugTargetRenderableInteraction)) 
                    continue;
                DrugTargetRenderableInteraction displayed = (DrugTargetRenderableInteraction) edge;
                Set<Interaction> backInteractions = displayed.getInteractions();
                backInteractions.retainAll(filtered);
                if (backInteractions.size() == 0) {
                    toBeDeleted.add(displayed);
                }
            }
        }
        pathwayEditor.removeFIs(toBeDeleted);
        return filtered;
    }
    
    public void fetchCancerDrugsForDisplay(Long peId,
                                           PathwayEditor pathwayEditor) {
        if (peId == null) {
            fetchCancerDrugs(pathwayEditor);
            return;
        }
        List<Interaction> interactions = getInteractions(peId,
                                                         (RenderablePathway) pathwayEditor.getRenderable());
        List<Interaction> filteredInteractions = applyFilter(interactions);
        if (interactions.size() > 0 && filteredInteractions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "No drugs can be displayed. Adjust the filter to show interactions.",
                                          "No Drugs to Display",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DiagramDrugTargetInteractionHandler handler = new DiagramDrugTargetInteractionHandler(pathwayEditor);
        handler.displayInteractions(filteredInteractions);
    }
    
    private void fetchCancerDrugs(PathwayEditor pathwayEditor) {
        // For this method, the server will be called regardless
        Map<Long, List<Interaction>> newIdToInteractions = fetchCancerDrugs(null,
                                                                           (RenderablePathway)pathwayEditor.getRenderable());
        // Cache fetched interactions
        Map<Long, List<Interaction>> pdIdToInteractions = new HashMap<>();
        for (Object obj : pathwayEditor.getDisplayedObjects()) {
            if ((obj instanceof Node) && (((Node)obj).getReactomeId() != null)) {
                Node node = (Node) obj;
                Long reactomeId = node.getReactomeId();
                // Check pre-existing one first
                List<Interaction> interactions = peID2Interactions.get(reactomeId);
                if (interactions == null) {
                    interactions = newIdToInteractions.get(reactomeId);
                    if (interactions == null)
                        interactions = new ArrayList<>(); // Want to mark it
                    peID2Interactions.put(reactomeId, interactions);
                }
                if (interactions.size() > 0)
                    pdIdToInteractions.put(reactomeId, interactions);
            }
        }
        if (pdIdToInteractions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot find any cancer drug for the pathway.",
                                          "No Cancer Drug",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        boolean hasInteractions = false;
        // Need to apply the filter
        Map<Long, List<Interaction>> pdIdToFiltered = new HashMap<>();
        for (Long dbId : pdIdToInteractions.keySet()) {
            List<Interaction> list = pdIdToInteractions.get(dbId);
            if (list != null && list.size() > 0) {
                List<Interaction> filtered = applyFilter(list);
                if (filtered.size() > 0) {
                    pdIdToFiltered.put(dbId, filtered);
                    hasInteractions = true;
                }
            }
        }
        if (!hasInteractions) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "No drugs can be displayed. Adjust the filter to show interactions.",
                                          "No Drugs to Display",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        DiagramDrugTargetInteractionHandler handler = new DiagramDrugTargetInteractionHandler(pathwayEditor);
        handler.displayInteractions(pdIdToFiltered);
    }
    
    private List<Interaction> applyFilter(List<Interaction> interactions) {
        List<Interaction> rtn = new ArrayList<>();
        if (interactions != null) {
            for (Interaction interaction : interactions) {
                if (interactionFilter.filter(interaction)) {
                    rtn.add(interaction);
                }
            }
        }
        return rtn;
    }
    
    private Element queryRESTfulAPI(RESTFulFIService service,
                                    RenderablePathway pathway,
                                    Long peId) throws Exception {
        if (peId != null)
            return service.queryDrugTargetInteractionsInDiagram(pathway.getReactomeDiagramId(),
                                                                peId);
        else if (peID2Interactions.size() == 0)
            return service.queryDrugTargetInteractionsInDiagram(pathway.getReactomeDiagramId(),
                                                                null); // Query for the whole pathway diagram
        else {
            // query whatever is needed
            Set<Long> toBeQueried = new HashSet<>();
            if (pathway.getComponents() != null) {
                for (Object obj : pathway.getComponents()) {
                    if (obj instanceof Node) {
                        Node node = (Node) obj;
                        if (node.getReactomeId() == null)
                            continue;
                        Long reactomeId = node.getReactomeId();
                        if (peID2Interactions.containsKey(reactomeId))
                            continue;
                        toBeQueried.add(reactomeId);
                    }
                }
            }
            if (toBeQueried.size() == 0)
                return null;
            return service.queryDrugTargetIneractions(toBeQueried);
        }
    }
    
    private Map<Long, List<Interaction>> fetchCancerDrugs(Long peId,
                                                          RenderablePathway pathway) {
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        try {
            RESTFulFIService service = new RESTFulFIService();
            ProgressPane progressPane = new ProgressPane();
            frame.setGlassPane(progressPane);
            progressPane.setMinimum(0);
            progressPane.setMaximum(100);
            progressPane.setIndeterminate(true);
            progressPane.setTitle("Fetch Cancer Drugs");
            progressPane.setVisible(true);
            progressPane.setText("Querying the server...");
            RESTFulFIService restfulService = new RESTFulFIService();
            Element rootElm = queryRESTfulAPI(restfulService, pathway, peId);
            if (rootElm == null) {
                frame.getGlassPane().setVisible(false);
                return new HashMap<>();
            }
            DrugTargetInteractionParser parser = new DrugTargetInteractionParser();
            parser.parse(rootElm);
            
            Map<Long, List<Interaction>> dbIdToInteractions = parser.getDbIdToInteractions();
            if (dbIdToInteractions == null) {
                dbIdToInteractions = new HashMap<>();
                dbIdToInteractions.put(peId, parser.getInteractions());
            }
            frame.getGlassPane().setVisible(false);
            return dbIdToInteractions;
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                                          "Error in querying cancer drugs for a selected object in diagram:\n" + e.getMessage(),
                                          "Error in Querying Drugs",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
        }
        return new HashMap<>(); 
    }
    
    /**
     * A utility method kept it here in appropriately.
     * @param exp
     * @return
     */
    public Number getExpEvidenceValue(ExpEvidence exp) {
        Number rtn = Double.MAX_VALUE;
        if (exp.getAssayValueMedian() != null) {
            try {
                // Try to use Integer first
                if (!exp.getAssayValueMedian().contains("."))
                    rtn = Integer.parseInt(exp.getAssayValueMedian());
                else
                    rtn = Double.parseDouble(exp.getAssayValueMedian());
            }
            catch(NumberFormatException e) {}
        }
        else if (exp.getAssayValueLow() != null) {
            try {
                rtn = Double.parseDouble(exp.getAssayValueLow());
            }
            catch(NumberFormatException e) {}
        }
        return rtn;
    }
    
}
