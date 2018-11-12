/*
 * Created on Dec 18, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import org.gk.graphEditor.GraphEditorActionEvent.ActionType;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.RenderableChemical;
import org.reactome.cytoscape.service.CyPathwayEditor;
import org.reactome.cytoscape.service.PathwayDiagramOverlayHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

import edu.ohsu.bcb.druggability.dataModel.Drug;
import edu.ohsu.bcb.druggability.dataModel.Interaction;

/**
 * This class is used to handle display of drug/target interactions.
 * @author gwu
 *
 */
public class DiagramDrugTargetInteractionHandler {
    private CyPathwayEditor pathwayEditor;
    private PathwayDiagramOverlayHelper overlayHelper;
    
    /**
     * Default constructor.
     */
    public DiagramDrugTargetInteractionHandler(PathwayEditor editor) {
        if (!(editor instanceof CyPathwayEditor))
            throw new IllegalArgumentException("The parameter should be a CyPathwayEditor!");
        pathwayEditor = (CyPathwayEditor) editor;
        overlayHelper = new PathwayDiagramOverlayHelper(editor);
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }

    /**
     * A helper method to display cancer drugs for a selected displayed PE.
     * @param interactions
     * @param peId
     */
    public void displayInteractions(List<Interaction> interactions) {
        if (interactions.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Cannot find cancer drugs for the selected object.",
                                          "No Cancer Drugs",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Node node = overlayHelper.getSelectedNode();
        if (node == null)
            return; // Nothing to be done
        List<Node> newNodes = new ArrayList<>();
        pathwayEditor.setDuringOverlay(true);
        displayInteractions(interactions, node, newNodes);
        overlayHelper.layout(node, newNodes);
        pathwayEditor.setDuringOverlay(false);
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        pathwayEditor.fireGraphEditorActionEvent(ActionType.INSERT);
    }

    private void displayInteractions(List<Interaction> interactions, 
                                     Node node, 
                                     List<Node> newNodes) {
        for (Interaction interaction : interactions) {
            Drug drug = interaction.getIntDrug();
            Node drugNode = overlayHelper.getRenderable(drug.getDrugName(),
                                                        RenderableChemical.class,
                                                        newNodes,
                                                        PlugInUtilities.DRUG_COLOR);
            DrugTargetRenderableInteraction rInt = getInteraction(node,
                                                                  drugNode,
                                                                  getDirection(interaction),
                                                                  drugNode.getDisplayName());
            rInt.addInteraction(interaction);
        }
    }
    
    public void displayInteractions(final Map<Long, List<Interaction>> dbIdToInteractions) {
        // For easy search
        Map<Long, Node> dbIdToNode = new HashMap<>();
        for (Object obj : pathwayEditor.getDisplayedObjects()) {
            if (obj instanceof Node) {
                Node node = (Node) obj;
                if (node.getReactomeId() != null)
                    dbIdToNode.put(node.getReactomeId(), node);
            }
        }
        // Display interactions     
        pathwayEditor.setDuringOverlay(true);
        Map<Node, List<Node>> nodeToNewNodes = new HashMap<>();
        // Sort this so that we can get a better layout
        List<Long> dbIdList = new ArrayList<>(dbIdToInteractions.keySet());
        Collections.sort(dbIdList, new Comparator<Long>() {
            public int compare(Long id1, Long id2) { 
                List<Interaction> list1 = dbIdToInteractions.get(id1);
                List<Interaction> list2 = dbIdToInteractions.get(id2);
                return list2.size() - list1.size();
            }
        });
        for (Long dbId : dbIdList) {
            List<Interaction> interactions = dbIdToInteractions.get(dbId);
            if (interactions.size() == 0)
                continue;
            Node node = dbIdToNode.get(dbId);
            if (node == null)
                continue;
            List<Node> newNodes = new ArrayList<>();
            nodeToNewNodes.put(node, newNodes);
            displayInteractions(interactions, node, newNodes);
        }
        // Try to perform a nice layout by anchoring to PE nodes having most new nodes
        for (Node node : nodeToNewNodes.keySet()) {
            List<Node> newNodes = nodeToNewNodes.get(node);
            overlayHelper.layout(node, newNodes);
        }
        pathwayEditor.setDuringOverlay(false);
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
        pathwayEditor.fireGraphEditorActionEvent(ActionType.INSERT);
    }
    
    private String getDirection(Interaction interaction) {
        String interactionType = interaction.getInteractionType();
        // The following types are saved in the database:
        // Antagonist
        // Inhibition
        // Agonist
        // Binding
        // None
        // Full agonist
        // Negative
        // NULL
        if (interactionType == null || 
            interaction.equals("None") ||  
            interaction.equals("Binding") ||
            interaction.equals("Null"))
            return "-";
        if (interactionType.toLowerCase().contains("agonist"))
            return "->";
        return "-|";
    }
    
    private DrugTargetRenderableInteraction getInteraction(Node targetNode,
                                                           Node drugNode,
                                                           String direction,
                                                           String name) {
        // Search pre-existing one
        List<HyperEdge> edges = targetNode.getConnectedReactions();
        for (HyperEdge edge : edges) {
            if (edge instanceof DrugTargetRenderableInteraction) {
                Node drugNode1 = edge.getInputNode(0);
                if (drugNode == drugNode1) {
                    DrugTargetRenderableInteraction rtn = (DrugTargetRenderableInteraction)edge;
                    rtn.addDirections(direction);
                    return rtn;
                }
            }
        }
        // Need to create one
        return createInteraction(targetNode, 
                                 drugNode, 
                                 direction,
                                 name);
    }
    
    private DrugTargetRenderableInteraction createInteraction(Node targetNode, 
                                                              Node drugNode, 
                                                              String direction,
                                                              String name) {
        // Create an interaction
        DrugTargetRenderableInteraction interaction = new DrugTargetRenderableInteraction();
        interaction.addInput(drugNode);
        interaction.addOutput(targetNode);
        interaction.setDirections(direction);
        // Add a display name
        interaction.setDisplayName(name);
        interaction.layout();
        pathwayEditor.insertEdge(interaction, false);
        return interaction;
    }
    
}
