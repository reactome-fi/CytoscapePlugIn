/*
 * Created on Dec 18, 2016
 *
 */
package org.reactome.cytoscape.drug;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.gk.graphEditor.PathwayEditor;
import org.gk.render.Node;
import org.gk.render.RenderableChemical;
import org.gk.render.RenderablePathway;
import org.gk.util.ProgressPane;
import org.jdom.Element;
import org.reactome.cytoscape.service.PathwayDiagramOverlayHelper;
import org.reactome.cytoscape.service.RESTFulFIService;
import org.reactome.cytoscape.util.PlugInObjectManager;

import edu.ohsu.bcb.druggability.Drug;
import edu.ohsu.bcb.druggability.Interaction;

/**
 * This class is used to handle display of drug/target interactions.
 * @author gwu
 *
 */
public class DiagramDrugTargetInteractionHandler {
    private PathwayEditor pathwayEditor;
    private PathwayDiagramOverlayHelper overlayHelper;
    
    /**
     * Default constructor.
     */
    public DiagramDrugTargetInteractionHandler(PathwayEditor editor) {
        this.pathwayEditor = editor;
        overlayHelper = new PathwayDiagramOverlayHelper(editor);
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }

    public void fetchCancerDrugs(Long peId) {
        if (pathwayEditor == null)
            throw new IllegalStateException("PathwayEditor has not been set!");
        RenderablePathway pathway = (RenderablePathway) pathwayEditor.getRenderable();
        if (pathway == null || pathway.getReactomeDiagramId() == null)
            throw new IllegalStateException("Cannot find pathway diagram id!");
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
            displayInteractions(drugInteractions);
            frame.getGlassPane().setVisible(false);
        }
        catch(Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame,
                                          "Error in querying cancer drugs for a selected object in diagram:\n" + e.getMessage(),
                                          "Error in Querying Drugs",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
        }
    } 
    
    /**
     * A helper method to display cancer drugs for a selected displayed PE.
     * @param interactions
     * @param peId
     */
    private void displayInteractions(List<Interaction> interactions) {
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
        for (Interaction interaction : interactions) {
            Drug drug = interaction.getIntDrug();
            Node drugNode = overlayHelper.getRenderable(drug.getDrugName(),
                                                        RenderableChemical.class,
                                                        newNodes);
            DrugTargetRenderableInteraction rInt = createInteraction(node,
                                                                     drugNode,
                                                                     getDirection(interaction),
                                                                     drugNode.getDisplayName());
            rInt.addInteraction(interaction);
        }
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
    
    public DrugTargetRenderableInteraction createInteraction(Node targetNode, 
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
