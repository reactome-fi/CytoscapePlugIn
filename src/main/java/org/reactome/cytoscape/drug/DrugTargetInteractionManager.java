/*
 * Created on Dec 21, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

import edu.ohsu.bcb.druggability.dataModel.ExpEvidence;
import edu.ohsu.bcb.druggability.dataModel.Interaction;

/**
 * A singleton to manage a list of drug/target interactions so that interactions will not be duplicated after loading
 * from the server.
 * @author gwu
 */
@SuppressWarnings("unchecked")
public class DrugTargetInteractionManager {
    // Just to save some space
    private final List<Interaction> EMPTY_LIST = new ArrayList<>();
    private static DrugTargetInteractionManager manager;
    // Cached filter
    private InteractionFilter interactionFilter;
    // Cached values
    private Map<DrugDataSource, Map<Long, List<Interaction>>> srcToPEIdToInteractions;
    // The current data source
    private DrugDataSource currentDataSource;
    
    /**
     * Default constructor.
     */
    protected DrugTargetInteractionManager() {
        this.interactionFilter = new InteractionFilter();
        srcToPEIdToInteractions = new HashMap<>();
    }
    
    public static List<String> getAssayTypes(Collection<Interaction> interactions) {
        List<String> types = interactions.stream()
                .filter(i -> i.getExpEvidenceSet() != null && i.getExpEvidenceSet().size() > 0)
                .flatMap(i -> i.getExpEvidenceSet().stream())
                .map(e -> e.getAssayType())
                .filter(type -> type != null)
                .collect(Collectors.toSet())
                .stream()
                .sorted((t1, t2) -> {
                   if (t1.equalsIgnoreCase("KD"))
                       return -1;
                   if (t2.equalsIgnoreCase("KD"))
                       return 1;
                   return t1.compareTo(t2);
                })
                .collect(Collectors.toList());
        return types;
    }
    
    public static DrugTargetInteractionManager getManager() {
        if (manager == null)
            manager = new DrugTargetInteractionManager();
        return manager;
    }
    
    public DrugDataSource getCurrentDataSource() {
        return currentDataSource;
    }

    public void setCurrentDataSource(DrugDataSource currentDataSource) {
        this.currentDataSource = currentDataSource;
    }

    public InteractionFilter getInteractionFilter() {
        return this.interactionFilter;
    }
    
    /**
     * This method should be applied to a sinle PE only
     * @param peId
     * @param pathway
     * @param dataSource
     * @return
     */
    private List<Interaction> getInteractions(Long peId, // peId is not null here
                                              RenderablePathway pathway,
                                              DrugDataSource dataSource) {
        if (peId == null)
            throw new IllegalArgumentException("The passed peId is null!");
        Map<Long, List<Interaction>> idToInteractions = srcToPEIdToInteractions.get(dataSource);
        if (idToInteractions != null) {
            List<Interaction> interactions = idToInteractions.get(peId);
            if (interactions != null)
                return interactions;
        }
        Map<Long, List<Interaction>> newDbIdToInteractions = _fetchDrugs(peId, 
                                                                        pathway,
                                                                        dataSource);
        List<Interaction> interactions = newDbIdToInteractions.get(peId);
        if (interactions == null) {
            interactions = new ArrayList<>();
            newDbIdToInteractions.put(peId, interactions);
        }
        if (idToInteractions == null)
            srcToPEIdToInteractions.put(dataSource, newDbIdToInteractions);
        else
            idToInteractions.putAll(newDbIdToInteractions);
        return interactions;
    }
    
    public void filterDrugs(CyPathwayEditor pathwayEditor) {
        if (srcToPEIdToInteractions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "There is no drug fetched. Fetch drugs first before filtering.",
                                          "No Drug for Filtering",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        interactionFilter.setPathwayEditor(pathwayEditor);
        interactionFilter.showDialog(PlugInObjectManager.getManager().getCytoscapeDesktop());
    }
    
    public void applyFilter(CyPathwayEditor pathwayEditor) {
        List<Renderable> selected = pathwayEditor.getSelection();
        if (selected != null && selected.size() == 1) { // For one single selected entity
            applyFilterToSelected(pathwayEditor, selected);
        }
        else { // Apply for all 
            _applyFilter(pathwayEditor);
        }
    }

    private void applyFilterToSelected(CyPathwayEditor pathwayEditor, List<Renderable> selected) {
        Long peId = null;
        for (Renderable r : selected) {
            if ((r instanceof Node) && (r.getReactomeId() != null)) {
                peId = r.getReactomeId(); // There should be only one selected object
                break;
            }
        }
        if (peId != null) {
            List<Interaction> filtered = _applyFilter(pathwayEditor, peId);
            if (filtered != null && filtered.size() > 0) {
                DiagramDrugTargetInteractionHandler handler = new DiagramDrugTargetInteractionHandler(pathwayEditor);
                handler.displayInteractions(filtered);
            }
        }
    }

    private void _applyFilter(CyPathwayEditor pathwayEditor) {
        // Get a list of ids that can be filtered
        List<Renderable> objs = pathwayEditor.getDisplayedObjects();
        Set<Long> idsToBeFiltered = objs.stream()
                                        .filter(r -> r instanceof Node)
                                        .filter(r -> r.getReactomeId() != null)
                                        .map(r -> r.getReactomeId())
                                        .collect(Collectors.toSet());
        // Have to do the second step to avoid concurrent exception
        Map<Long, List<Interaction>> dbIdToFiltered = new HashMap<>();
        for (Long dbId : idsToBeFiltered) {
            List<Interaction> filtered = _applyFilter(pathwayEditor, dbId);
            if (filtered != null && filtered.size() > 0)
                dbIdToFiltered.put(dbId, filtered);
        }
        if (dbIdToFiltered.size() > 0) {
            DiagramDrugTargetInteractionHandler handler = new DiagramDrugTargetInteractionHandler(pathwayEditor);
            handler.displayInteractions(dbIdToFiltered);
        }
    }
    
    private List<Interaction> _applyFilter(CyPathwayEditor pathwayEditor, Long peId) {
        if (currentDataSource == null)
            return null; // Nothing to be done
        Map<Long, List<Interaction>> peIdToInteractions = srcToPEIdToInteractions.get(currentDataSource);
        if (peIdToInteractions == null)
            return null;
        List<Interaction> interactions = peIdToInteractions.get(peId);
        if (interactions == null || interactions.size() == 0)
            return null; // Nothing to be done
        List<Interaction> filtered = _applyFilter(interactions);
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
    
    /**
     * Return a set of interactions without displaying
     * @param pathwayEditor
     * @return
     */
    public Set<Interaction> fetchDrugsInteractions(PathwayEditor pathwayEditor, DrugDataSource dataSource) {
        Map<Long, List<Interaction>> pdIdToFiltered = _fetchDrugsForPathway(pathwayEditor,
                                                                            false,
                                                                            dataSource);
        if (pdIdToFiltered.size() == 0)
            return new HashSet<>(); 
        Set<Interaction> rtn = new HashSet<>();
        for (List<Interaction> interactions : pdIdToFiltered.values())
            rtn.addAll(interactions);
        return rtn;
    }
    
    public void fetchDrugsForDisplay(Long peId,
                                     PathwayEditor pathwayEditor,
                                     DrugDataSource dataSource) {
        if (peId == null) { // Fetch interactions for all PEs
            Map<Long, List<Interaction>> pdIdToFiltered = _fetchDrugsForPathway(pathwayEditor, true, dataSource);
            if (pdIdToFiltered.size() == 0)
                return; // Do nothing
            DiagramDrugTargetInteractionHandler handler = new DiagramDrugTargetInteractionHandler(pathwayEditor);
            handler.displayInteractions(pdIdToFiltered);
            return;
        }
        List<Interaction> interactions = getInteractions(peId,
                                                         (RenderablePathway) pathwayEditor.getRenderable(),
                                                         dataSource);
        List<Interaction> filteredInteractions = _applyFilter(interactions);
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
    
    private Map<Long, List<Interaction>> _fetchDrugsForPathway(PathwayEditor pathwayEditor,
                                                               boolean needFilter,
                                                               DrugDataSource dataSource) {
        // For this method, the server will be called regardless
        Map<Long, List<Interaction>> peIdToInteractions = _fetchDrugs(null,
                                                                      (RenderablePathway)pathwayEditor.getRenderable(),
                                                                      dataSource);
        // But we still need to cache these interactions for filtering
        cachePathwayInteractions(peIdToInteractions, pathwayEditor, dataSource);
        if (peIdToInteractions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot find any cancer drug for the pathway.",
                                          "No Cancer Drug",
                                          JOptionPane.INFORMATION_MESSAGE);
            return peIdToInteractions;
        }
        if (!needFilter)
            return peIdToInteractions;
        Map<Long, List<Interaction>> peIdToFiltered = filterInteractions(peIdToInteractions);
        if (peIdToFiltered.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "No drugs can be displayed. Adjust the filter to show interactions.",
                                          "No Drugs to Display",
                                          JOptionPane.INFORMATION_MESSAGE);
            return peIdToFiltered;
        }
        return peIdToFiltered;
    }
    
    private void cachePathwayInteractions(Map<Long, List<Interaction>> peIdToInteractions, 
            PathwayEditor pathwayEditor, 
            DrugDataSource dataSource) {
        Map<Long, List<Interaction>> currentIdToInteractions = srcToPEIdToInteractions.get(dataSource);
        if (currentIdToInteractions == null) {
            srcToPEIdToInteractions.put(dataSource, peIdToInteractions);
            return;
        }
        currentIdToInteractions.putAll(peIdToInteractions);
    }

    private Map<Long, List<Interaction>> filterInteractions(Map<Long, List<Interaction>> pdIdToInteractions) {
        // Need to apply the filter
        Map<Long, List<Interaction>> pdIdToFiltered = new HashMap<>();
        for (Long dbId : pdIdToInteractions.keySet()) {
            List<Interaction> list = pdIdToInteractions.get(dbId);
            if (list != null && list.size() > 0) {
                List<Interaction> filtered = _applyFilter(list);
                if (filtered.size() > 0) {
                    pdIdToFiltered.put(dbId, filtered);
                }
            }
        }
        return pdIdToFiltered;
    }

    private List<Interaction> _applyFilter(List<Interaction> interactions) {
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
                                    Long peId, // This may be null
                                    DrugDataSource dataSource) throws Exception {
        if (peId != null)
            return service.queryDrugTargetInteractionsInDiagram(pathway.getReactomeDiagramId(),
                    peId,
                    dataSource.toString());
        else  
            return service.queryDrugTargetInteractionsInDiagram(pathway.getReactomeDiagramId(),
                    null,
                    dataSource.toString()); // Query for the whole pathway diagram
    }
    
    private Map<Long, List<Interaction>> _fetchDrugs(Long peId, // pedId is null if query is for the whole pathway
                                                    RenderablePathway pathway,
                                                    DrugDataSource dataSource) {
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        try {
            ProgressPane progressPane = new ProgressPane();
            frame.setGlassPane(progressPane);
            progressPane.setMinimum(0);
            progressPane.setMaximum(100);
            progressPane.setIndeterminate(true);
            progressPane.setTitle("Fetch Drugs");
            progressPane.setVisible(true);
            progressPane.setText("Querying the server...");
            RESTFulFIService restfulService = new RESTFulFIService();
            Element rootElm = queryRESTfulAPI(restfulService, pathway, peId, dataSource);
            if (rootElm == null) { // This should not occur
                frame.getGlassPane().setVisible(false);
                return new HashMap<>();
            }
            DrugTargetInteractionParser parser = new DrugTargetInteractionParser();
            parser.parse(rootElm);
            
            Map<Long, List<Interaction>> dbIdToInteractions = parser.getDbIdToInteractions();
            // Need to do a filter for visible nodes only
            List<Renderable> list = pathway.getComponents();
            Set<Long> displayIds = list.stream()
                                       .filter(r -> r instanceof Node)
                                       .filter(r -> r.isVisible())
                                       .filter(r -> r.getReactomeId() != null)
                                       .map(r -> r.getReactomeId())
                                       .collect(Collectors.toSet());
            if (dbIdToInteractions == null) {
                if (peId == null) {
                    Map<Long, List<Interaction>> dbIdToInteractions1 = new HashMap<>();
                    displayIds.forEach(dbId -> dbIdToInteractions1.put(dbId, EMPTY_LIST));
                    dbIdToInteractions = dbIdToInteractions1;
                }
                else {
                    dbIdToInteractions = new HashMap<>();
                    // The interactions may not be marked for this PEID.
                    dbIdToInteractions.put(peId, parser.getInteractions());
                }
            }
            else { // Need to do a filter for visible nodes only
                if (peId == null)
                    dbIdToInteractions.keySet().retainAll(displayIds);
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
     * Check if an ExpEvidence should not be used to get assay value.
     * @param evidence
     * @return
     */
    public boolean shouldFilterOut(ExpEvidence evidence) {
        if (evidence.getAssayType() == null)
            return true; // This happens
        if (evidence.getAssayRelation() != null && evidence.getAssayRelation().equals(">"))
            return true; // Don't want to use ">" values.
        return false;
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
