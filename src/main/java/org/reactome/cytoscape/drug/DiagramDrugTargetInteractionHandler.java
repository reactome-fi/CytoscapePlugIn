/*
 * Created on Dec 18, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import org.apache.commons.math.stat.descriptive.moment.SemiVariance.Direction;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.RenderableChemical;
import org.reactome.cytoscape.service.CyPathwayEditor;
import org.reactome.cytoscape.service.PathwayDiagramOverlayHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;

import edu.ohsu.bcb.druggability.Drug;
import edu.ohsu.bcb.druggability.Interaction;

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
        for (Interaction interaction : interactions) {
            Drug drug = interaction.getIntDrug();
            Node drugNode = overlayHelper.getRenderable(drug.getDrugName(),
                                                        RenderableChemical.class,
                                                        newNodes);
            DrugTargetRenderableInteraction rInt = getInteraction(node,
                                                                  drugNode,
                                                                  getDirection(interaction),
                                                                  drugNode.getDisplayName());
            rInt.addInteraction(interaction);
        }
        pathwayEditor.setDuringOverlay(false);
        overlayHelper.layout(node, newNodes);
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
