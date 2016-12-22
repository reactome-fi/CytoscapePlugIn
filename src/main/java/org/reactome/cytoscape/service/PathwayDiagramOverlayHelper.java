/*
 * Created on Feb 10, 2014
 *
 */
package org.reactome.cytoscape.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gk.graphEditor.PathwayEditor;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;

/**
 * A helper class for overlaying FIs and other interactions onto a pathway diagram.
 * @author gwu
 *
 */
public class PathwayDiagramOverlayHelper {
    private PathwayEditor pathwayEditor;
    
    public PathwayDiagramOverlayHelper(PathwayEditor editor) {
        this.pathwayEditor = editor;
    }
    
    public void getPreAddedFIs(Node node,
                               Map<String, FIRenderableInteraction> nodesToFI) {
        List<HyperEdge> edges = node.getConnectedReactions();
        for (HyperEdge edge : edges) {
            if (edge instanceof FIRenderableInteraction) {
                FIRenderableInteraction fi = (FIRenderableInteraction) edge;
                String key = generateKeyForFI(fi);
                nodesToFI.put(key, fi);
            }
        }
    }
    
    /**
     * Do a giggle layout around the center nodes for newly added FI partners.
     * @param node
     * @param newNodes
     */
    public void layout(Node node,
                       List<Node> newNodes) {
        if (newNodes.size() == 0)
            return;
        List<String> newNames = new ArrayList<String>();
        for (Renderable r : newNodes)
            newNames.add(r.getDisplayName());
        Map<String, double[]> nameToCoords = new JiggleLayout().jiggleLayout(node.getDisplayName(),
                                                                             newNames);
        double dx = node.getPosition().getX() - nameToCoords.get(node.getDisplayName())[0];
        double dy = node.getPosition().getY() - nameToCoords.get(node.getDisplayName())[1];
        for (Node r : newNodes) {
            double[] coords = nameToCoords.get(r.getDisplayName());
            int x = (int) (coords[0] + dx);
            int y = (int) (coords[1] + dy);
            // Should not be allowed outside the bounds
            if (x < 50) // These numbers 50 and 25 are rather arbitrary
                x = 50;
            if (y < 25)
                y = 25;
            r.setPosition(x, y);
            // Need to call the following invalidation methods to make layout work.
            r.setBounds(null); // Force to recreate a new bounds. Otherwise, the layout cannot work
                               // This is more like a bug in the original code!!!
            r.invalidateConnectWidgets();
            r.invalidateTextBounds(); // Make sure text display is correct
            List<HyperEdge> interactions = ((Node)r).getConnectedReactions();
            for (HyperEdge edge : interactions)
                edge.layout();
        }
        pathwayEditor.repaint(pathwayEditor.getVisibleRect());
    }

    public FIRenderableInteraction createInteraction(Node node, 
                                                     Node partner, 
                                                     String direction,
                                                     String name,
                                                     PathwayEditor editor) {
        // Create an interaction
        FIRenderableInteraction interaction = new FIRenderableInteraction();
        interaction.addInput(node);
        interaction.addOutput(partner);
        interaction.setDirections(direction);
        // Add a display name
        interaction.setDisplayName(name);
        interaction.layout();
        editor.insertEdge(interaction, false);
        return interaction;
    }

    private String generateKeyForFI(FIRenderableInteraction fi) {
        Node node = fi.getInputNode(0);
        Node partner = fi.getOutputNode(0);
        return generateKeyForNodes(node, partner);
    }
    
    public String generateKeyForNodes(Node node,
                                      Node partner) {
        // Generate a key
        String name1 = node.getDisplayName();
        String name2 = partner.getDisplayName();
        String key = null;
        if (name1.compareTo(name2) < 0)
            key = name1 + " - " + name2;
        else
            key = name2 + " - " + name1;
        return key;
    }
    
    /**
     * Get a Renderable for an entity specified by its name. If this gene has been
     * added in the diagram, the previously added RenderableProtein should be returned.
     * Otherwise, a new RenderableProtein should be created. For the time being, only
     * RenderableProtein will be created assuming that FIs involve proteins only.
     * @param gene
     * @return
     */
    public <T extends Node> Node getRenderable(String name,
                                               Class<T> type,
                                               List<Node> newNodes) {
        for (Object obj : pathwayEditor.getDisplayedObjects()) {
            Renderable r = (Renderable) obj;
            if (r instanceof HyperEdge || r.getReactomeId() != null)
                continue;
            if (r.getClass() == type && r.getDisplayName().equals(name))
                return (Node) r; // For sure this is a Node object.
        }
        try {
            Node r = type.newInstance();
            r.setDisplayName(name);
            pathwayEditor.insertNode(r);
            newNodes.add(r);
            return r;
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public Node getSelectedNode() {
        // Get the selected PE as the anchor for adding FIs
        List<Renderable> selection = pathwayEditor.getSelection();
        if (selection == null || selection.size() != 1)
            return null;
        Renderable r = selection.get(0);
        if (!(r instanceof Node))
            return null;
        Node node = (Node) r;
        return node;
    }
    
    public Node getNodeForName(String name) {
        for (Object obj : pathwayEditor.getDisplayedObjects()) {
            Renderable r = (Renderable) obj;
            if (!(r instanceof Node) || r.getReactomeId() == null)
                continue;
            Node node = (Node) r;
            if (node.getDisplayName().equals(name))
                return node;
        }
        return null;
    }
    
}
