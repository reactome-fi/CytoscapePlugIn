package org.reactome.cytoscape.mechismo;

import java.awt.Component;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.HyperEdge;
import org.gk.render.Renderable;
import org.reactome.cytoscape.bn.VariableCytoPaneComponent;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.mechismo.model.CancerType;
import org.reactome.mechismo.model.Interaction;
import org.reactome.mechismo.model.Reaction;
import org.reactome.r3.util.InteractionUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class is used to query mechsimo data.
 * @author wug
 *
 */
public class MechismoDataFetcher {
    private Logger logger = LoggerFactory.getLogger(MechismoDataFetcher.class);
    private String restfulHost;
    private PathwayHighlightControlPanel hiliteControlPane;
    
    public MechismoDataFetcher() {
        restfulHost = PlugInObjectManager.getManager().getProperties().getProperty("MechismoWSURL");
        if (restfulHost == null)
            throw new IllegalStateException("MechismoWSURL has not been configured!");
    }
    
    public PathwayHighlightControlPanel getHiliteControlPane() {
        return hiliteControlPane;
    }

    public void setHiliteControlPane(PathwayHighlightControlPanel hiliteControlPane) {
        this.hiliteControlPane = hiliteControlPane;
    }
    
    public void removeMechismoResults(PathwayEditor editor) {
        if (editor == null || hiliteControlPane == null)
            return;
        // Remove highlight colors
        hiliteControlPane.removeHighlight();
        hiliteControlPane.setVisible(false);
        // Remove tabs related to BNs
        // Remove tabs for showing BN results
        CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(CytoPanelName.SOUTH);
        List<VariableCytoPaneComponent> toBeClosed = new ArrayList<>();
        for (int i = 0; i < cytoPanel.getCytoPanelComponentCount(); i++) {
            Component c = cytoPanel.getComponentAt(i);
            if (c instanceof MechismoReactionPane || c instanceof MechismoInteractionPane) {
                toBeClosed.add((VariableCytoPaneComponent)c);
            }
        }
        toBeClosed.stream().forEach(p -> p.close());
    }

    public Interaction loadMechismoInteraction(String name) throws Exception {
        // Required by the WS
        String[] tokens = name.split(" ");
        // Need to encode it
        String tmpName = InteractionUtilities.generateFIFromGene(tokens[0], tokens[2]);
        tmpName = URLEncoder.encode(tmpName, "UTF-8");
        String url = restfulHost + "interaction/" + tmpName;
        String output = PlugInUtilities.callHttpInJson(url,
                PlugInUtilities.HTTP_GET,
                null);
        if (output == null || output.trim().length() == 0) {
            return null; // Avoid to show a dialog here to lock the user interface
        }
        ObjectMapper mapper = new ObjectMapper();
        Interaction interaction = mapper.readValue(output,
                new TypeReference<Interaction>() {
        });
        displayInteraction(interaction);
        return interaction;
    }
    
    private void displayInteraction(Interaction interaction) {
        InteractionMutationView view = new InteractionMutationView();
        view.setInteraction(interaction);
    }
    
    public List<CancerType> loadCancerTypes() {
        String url = restfulHost + "cancerTypes";
        try {
            String output = PlugInUtilities.callHttpInJson(url,
                    PlugInUtilities.HTTP_GET,
                    null);
            ObjectMapper mapper = new ObjectMapper();
            List<CancerType> cancerTypes = mapper.readValue(output,
                    new TypeReference<List<CancerType>>() {
            });
            return cancerTypes;
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                    "Error in fetching cancer types:\n" + e.getMessage(),
                    "Error in Fetching Cancer Types",
                    JOptionPane.ERROR_MESSAGE);
        }
        return new ArrayList<>();
    }
    
    public void loadMechismoInteractions(CyNetworkView networkView) {
        Set<String> fis = grepFINames(networkView);
        String url = restfulHost + "interactions";
        try {
            String output = PlugInUtilities.callHttpInJson(url,
                                PlugInUtilities.HTTP_POST,
                                String.join(",", fis));
            ObjectMapper mapper = new ObjectMapper();
            List<Interaction> interactions = mapper.readValue(output,
                                                        new TypeReference<List<Interaction>>() {
                                                        });
            displayInteractions(interactions, networkView);
        }
        catch(Exception e) {
            logger.error(e.getMessage(), e);
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in fetching Mechismo analysis results:\n" + e.getMessage(),
                                          "Error in Fetching Mechismo Results",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void displayInteractions(List<Interaction> interactions,
                                    CyNetworkView networkView) {
        MechismoInteractionPane pane = PlugInUtilities.getCytoPanelComponent(MechismoInteractionPane.class,
                CytoPanelName.SOUTH, 
                MechismoInteractionPane.TITLE);
        pane.setNetworkView(networkView);
        pane.setInteractions(interactions);
    }
    
    private Set<String> grepFINames(CyNetworkView view) {
        CyTable edgeTable = view.getModel().getDefaultEdgeTable();
        Set<String> names = view.getEdgeViews()
                                .stream()
                                .map(edgeView -> getEdgeName(edgeView.getModel(), edgeTable))
                                .collect(Collectors.toSet());
        return names;
    }
    
    private String getEdgeName(CyEdge edge, CyTable table) {
        String name = table.getRow(edge.getSUID()).get("name", String.class);
        // The format of name should be something like: Gene1 (FI) Gene2
        // We need to convert names that can be used by the RESTful API
        String[] tokens = name.split(" ");
        return InteractionUtilities.generateFIFromGene(tokens[0], tokens[2]);
    }
    
    public void loadMechismoReactions(PathwayEditor pathwayEditor) throws Exception {
        Set<String> dbIds = grepReactionIds(pathwayEditor);
        String text = String.join(",", dbIds);
        String url = restfulHost + "reactions";
        String output = PlugInUtilities.callHttpInJson(url,
                PlugInUtilities.HTTP_POST,
                text);
        ObjectMapper mapper = new ObjectMapper();
        List<Reaction> reactions = mapper.readValue(output,
                new TypeReference<List<Reaction>>() {
        });
        // Remove null
        reactions = reactions.stream().filter(rxt -> rxt != null).collect(Collectors.toList());
        displayReactions(reactions);
    }
    
    private void displayReactions(List<Reaction> reactions) {
        MechismoReactionPane pane = PlugInUtilities.getCytoPanelComponent(MechismoReactionPane.class,
                                                                          CytoPanelName.SOUTH, 
                                                                          MechismoReactionPane.TITLE);
        pane.setHiliteControlPane(hiliteControlPane); 
        pane.setReactions(reactions);
    }
    
    @SuppressWarnings("unchecked")
    private Set<String> grepReactionIds(PathwayEditor editor) {
        // Have to cast the following first. Otherwise, statements
        // after this will not work.
        List<Renderable> renderables = editor.getDisplayedObjects();
        Set<String> dbIds = renderables
                .stream()
                .filter(r -> r instanceof HyperEdge)
                .filter(r -> r.getReactomeId() != null)
                .map(r -> r.getReactomeId() + "")
                .collect(Collectors.toSet());
        return dbIds;
    }
    
    

}
