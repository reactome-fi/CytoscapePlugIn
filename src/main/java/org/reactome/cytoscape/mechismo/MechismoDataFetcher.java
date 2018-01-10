package org.reactome.cytoscape.mechismo;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CytoPanel;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.HyperEdge;
import org.gk.render.Renderable;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.mechismo.model.Reaction;
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
    
    public MechismoDataFetcher() {
        restfulHost = PlugInObjectManager.getManager().getProperties().getProperty("MechismoWSURL");
        if (restfulHost == null)
            throw new IllegalStateException("MechismoWSURL has not been configured!");
    }
    
    public void loadMechismoReactions(PathwayEditor pathwayEditor) {
        Set<String> dbIds = grepReactionIds(pathwayEditor);
        String text = String.join(",", dbIds);
        String url = restfulHost + "reaction";
        try {
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
        catch(Exception e) {
            logger.error(e.getMessage(), e);
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in fetching Mechismo analysis results:\n" + e.getMessage(),
                                          "Error in Fetching Mechismo Results",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void displayReactions(List<Reaction> reactions) {
        MechismoReactionPane pane = new MechismoReactionPane("Mechismo Analysis");
        CytoPanel cytoPanel = PlugInObjectManager.getManager().getCySwingApplication().getCytoPanel(pane.getCytoPanelName());
        int index = cytoPanel.indexOfComponent(pane);
        if (index > -1)
            cytoPanel.setSelectedIndex(index);
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
