/*
 * Created on Dec 21, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.gk.graphEditor.PathwayEditor;
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
    private Map<String, Interaction> idToInteraction;
    // Cached filter
    private InteractionFilter interactionFilter;
    // Cached values
    private Map<Long, List<Interaction>> peID2Interactions;
    
    /**
     * Default constructor.
     */
    private DrugTargetInteractionManager() {
        this.idToInteraction = new HashMap<>();
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
    
    public void addInteractions(Collection<Interaction> interactions) {
        if (interactions == null || interactions.size() == 0)
            return;
        for (Interaction interaction : interactions) {
            if (idToInteraction.containsKey(interaction.getId()))
                continue;
            idToInteraction.put(interaction.getId(),
                                interaction);
        }
    }
    
    private List<Interaction> getInteractions(Long peId,
                                              RenderablePathway pathway) {
        List<Interaction> interactions = peID2Interactions.get(peId);
        if (interactions != null)
            return interactions;
        interactions = fetchCancerDrugs(peId, 
                                        pathway);
        peID2Interactions.put(peId, interactions);
        return interactions;
    }
    
    public void filterCancerDrugs(CyPathwayEditor pathwayEditor) {
        interactionFilter.showDialog(pathwayEditor);
    }
    
    public void applyFilter(CyPathwayEditor pathwayEditor) {
        Long peId = null;
        List<Renderable> selected = pathwayEditor.getSelection();
        for (Renderable r : selected) {
            if ((r instanceof Node) && (r.getReactomeId() != null)) {
                peId = r.getReactomeId();
                break;
            }
        }
        List<Interaction> interactions = peID2Interactions.get(peId);
        if (interactions == null || interactions.size() == 0)
            return; // Nothing to be done
        List<Interaction> filtered = applyFilter(interactions);
        // Check displayed interactions
        List<FIRenderableInteraction> toBeDeleted = new ArrayList<>();
        for (Object r : pathwayEditor.getDisplayedObjects()) {
            if (r instanceof DrugTargetRenderableInteraction) {
                DrugTargetRenderableInteraction displayed = (DrugTargetRenderableInteraction) r;
                Set<Interaction> backInteractions = displayed.getInteractions();
                if (!isShared(filtered, backInteractions)) {
                    toBeDeleted.add(displayed);
                }
            }
        }
        pathwayEditor.removeFIs(toBeDeleted);
        DiagramDrugTargetInteractionHandler handler = new DiagramDrugTargetInteractionHandler(pathwayEditor);
        handler.displayInteractions(filtered);
    }
    
    private boolean isShared(List<Interaction> toBeDisplayed,
                             Set<Interaction> existed) {
        for (Interaction i : toBeDisplayed) {
            if (existed.contains(i))
                return true;
        }
        return false;
    }
    
    public void fetchCancerDrugsForDisplay(Long peId,
                                           PathwayEditor pathwayEditor) {
        List<Interaction> interactions = getInteractions(peId,
                                                         (RenderablePathway) pathwayEditor.getRenderable());
        List<Interaction> filteredInteractions = applyFilter(interactions);
        DiagramDrugTargetInteractionHandler handler = new DiagramDrugTargetInteractionHandler(pathwayEditor);
        handler.displayInteractions(filteredInteractions);
    }
    
    private List<Interaction> applyFilter(List<Interaction> interactions) {
        System.out.println("Total interactions before filtering: " + interactions.size());
        List<Interaction> rtn = new ArrayList<>();
        if (interactions != null) {
            for (Interaction interaction : interactions) {
                System.out.println(interaction.getInteractionID() + ": " + 
                        interaction.getIntDrug().getDrugName() + " - " + 
                        interaction.getIntTarget().getTargetName());
                if (interaction.getExpEvidenceSet() != null) {
                    for (ExpEvidence evidence : interaction.getExpEvidenceSet()) {
                        System.out.println(evidence.getAssayType() + " " + 
                                DrugTargetInteractionManager.getManager().getExpEvidenceValue(evidence));
                    }
                }
                if (interactionFilter.filter(interaction)) {
                    System.out.println("Passed!");
                    rtn.add(interaction);
                }
                else
                    System.out.println("Not passed!");
                System.out.println();
            }
        }
        System.out.println("Total interactions after filtering: " + rtn.size());
        return rtn;
    }
    
    private List<Interaction> fetchCancerDrugs(Long peId,
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
            Element rootElm = restfulService.queryDrugTargetInteractionsInDiagram(pathway.getReactomeDiagramId(), 
                                                                                  peId);
            DrugTargetInteractionParser parser = new DrugTargetInteractionParser();
            parser.parse(rootElm);
            
            List<Interaction> drugInteractions = parser.getInteractions();
            frame.getGlassPane().setVisible(false);
            return drugInteractions;
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                                          "Error in querying cancer drugs for a selected object in diagram:\n" + e.getMessage(),
                                          "Error in Querying Drugs",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
        }
        return new ArrayList<>(); // Return an empty list
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
