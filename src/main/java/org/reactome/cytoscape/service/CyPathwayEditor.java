/*
 * Created on Dec 21, 2016
 *
 */
package org.reactome.cytoscape.service;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JViewport;
import javax.swing.SwingUtilities;

import org.gk.graphEditor.GraphEditorActionEvent;
import org.gk.graphEditor.PathwayEditor;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableInteraction;
import org.gk.render.RenderablePathway;

/**
 * A customized PathwayEditor in order to do something special for PathwayDiagram displayed in
 * Cytoscape.
 * @author gwu
 */
public class CyPathwayEditor extends PathwayEditor {
    // Record newly added RenderableInteraction for new drawing
    private List<FIRenderableInteraction> overlaidFIs;
    // A flag to block repaint during overlaying
    private boolean duringOverlay;
    
    public CyPathwayEditor() {
        setEditable(false); // Only used as a pathway diagram view and not for editing
    }

    public boolean isDuringOverlay() {
        return duringOverlay;
    }

    public void setDuringOverlay(boolean duringOverlay) {
        this.duringOverlay = duringOverlay;
    }
    
    public List<FIRenderableInteraction> getOverlaidFIs() {
        return this.overlaidFIs;
    }

    @Override
    public Rectangle getVisibleRect() {
        if (getParent() instanceof JViewport) {
            JViewport viewport = (JViewport) getParent();
            Rectangle parentBounds = viewport.getBounds();
            Rectangle visibleRect = new Rectangle();
            Point position = SwingUtilities.convertPoint(viewport.getParent(),
                                                         parentBounds.x,
                                                         parentBounds.y,
                                                         this);
            visibleRect.x = position.x;
            visibleRect.y = position.y;
            visibleRect.width = parentBounds.width;
            visibleRect.height = parentBounds.height;
            return visibleRect;
        }
        return super.getVisibleRect();
    }
    
    /**
     * Remove FIs attached to a Node specified by r.
     * @param r
     */
    public void removeFIs(Node r) {
        if (overlaidFIs == null || overlaidFIs.size() == 0)
            return;
        List<RenderableInteraction> toBeRemoved = new ArrayList<RenderableInteraction>();
        for (RenderableInteraction fi : overlaidFIs) {
            if (fi.getInputNodes().contains(r) || fi.getOutputNodes().contains(r)) {
                toBeRemoved.add(fi);
                // Deleted added gene if it is not connected to others
                Node input = fi.getInputNode(0);
                Node output = fi.getOutputNode(0);
                delete(fi);
                Node geneNode = null;
                if (input == r)
                    geneNode = output;
                else
                    geneNode = input;
                if (geneNode.getConnectedReactions().size() == 0)
                    delete(geneNode);
            }
        }
        overlaidFIs.removeAll(toBeRemoved);
        repaint(getVisibleRect());
    }
    
    public void removeFIs(List<FIRenderableInteraction> fis) {
        if (overlaidFIs == null || overlaidFIs.size() == 0)
            return;
        List<RenderableInteraction> toBeRemoved = new ArrayList<RenderableInteraction>();
        for (RenderableInteraction fi : fis) {
            Node input = fi.getInputNode(0);
            Node output = fi.getOutputNode(0);
            delete(fi);
            // Check if a connected node should be deleted too
            if (input.getReactomeId() == null && input.getConnectedReactions().size() == 0)
                delete(input);
            if (output.getReactomeId() == null && output.getConnectedReactions().size() == 0)
                delete(output);
            toBeRemoved.add(fi);
        }
        overlaidFIs.removeAll(toBeRemoved);
        repaint(getVisibleRect());
    }
    
    /**
     * Remove all overlaid FIs.
     */
    public void removeFIs() {
        if (overlaidFIs == null || overlaidFIs.size() == 0)
            return;
        removeFIs(overlaidFIs);
    }
    
    /**
     * Check if there is any FI overlaid for the specified Renderable object.
     * @param r usually should be a Node object.
     * @return
     */
    public boolean hasFIsOverlaid(Renderable r) {
        if (overlaidFIs == null || overlaidFIs.size() == 0)
            return false;
        for (RenderableInteraction fi : overlaidFIs) {
            if (fi.getInputNodes().contains(r) || fi.getOutputNodes().contains(r))
                return true;
        }
        return false;
    }
    
    /**
     * Check if there is any FI overlaid
     * @return
     */
    public boolean hasFIsOverlaid() {
        if (overlaidFIs == null || overlaidFIs.size() == 0)
            return false;
        return true;
    }
    
    /**
     * Override this method to avoid any threading issues during a batch modification.
     */
    @Override
    public void insertEdge(HyperEdge edge, boolean useDefaultInsertPos) {
        if (useDefaultInsertPos) {
            Point position = new Point(defaultInsertPos);
            edge.initPosition(position);
            updateDefaultInsertPos();
        }
        ((RenderablePathway)displayedObject).addComponent(edge);
        if (edge.getContainer() != displayedObject)
            edge.setContainer(displayedObject);
        // Need to figure out the exact bounds for draw
        Rectangle bounds = edge.getBounds();
        Rectangle tmp = new Rectangle(bounds);
        // Make it a little big
        tmp.x -= 10;
        tmp.y -= 10;
        tmp.width += 20;
        tmp.height += 20;
        if (edge instanceof FIRenderableInteraction) {
            if (overlaidFIs == null)
                overlaidFIs = new ArrayList<FIRenderableInteraction>();
            overlaidFIs.add((FIRenderableInteraction)edge);
        }
    }
    
    /**
     * Override this method to avoid any threading issues during a batch modification.
     */
    @Override
    public void insertNode(Node node) {
        // Check if the position is set
        if (node.getPosition() == null) {
            Point p = new Point(defaultInsertPos);
            node.setPosition(p);
            updateDefaultInsertPos();
        }
        ((RenderablePathway)displayedObject).addComponent(node);
        if (node.getContainer() != displayedObject)
            node.setContainer(displayedObject);
    }

    @Override
    public void fireGraphEditorActionEvent(GraphEditorActionEvent e) {
        if (duringOverlay)
            return; // Don't do anything during overlaying
        super.fireGraphEditorActionEvent(e);
    }

    @Override
    public void paint(Graphics g) {
        if (duringOverlay)
            return; // Don't do anything during overlaying FIs.
        // We need to create bounds for those nodes
        Set<Node> nodes = new HashSet<Node>();
        if (overlaidFIs != null && overlaidFIs.size() > 0) {
            for (RenderableInteraction fi : overlaidFIs) {
                nodes.addAll(fi.getInputNodes());
                nodes.addAll(fi.getOutputNodes());
            }
            for (Node node : nodes) {
                node.validateBounds(g);
            }
        }
        super.paint(g);
        if (overlaidFIs == null || overlaidFIs.size() == 0)
            return;
        // The following code is based on DiseasePathwayImageEditor
        // in package org.gk.graphEditor.
        // Create a background
        Graphics2D g2 = (Graphics2D) g;
        // Draw a transparent background: light grey
        Color color = new Color(204, 204, 204, 175);
        g2.setPaint(color);
        Dimension size = getSize();
        // Preferred size has been scaled. Need to scale it back
        g2.fillRect(0, 
                    0, 
                    (int)(size.width / scaleX + 1.0d), 
                    (int)(size.height / scaleY + 1.0d));
        Rectangle clip = g.getClipBounds();
        for (Node node : nodes) {
            if (node.getBounds() != null && clip.intersects(node.getBounds()))
                node.render(g);
        }
        for (RenderableInteraction fi : overlaidFIs) {
            fi.validateConnectInfo();
            if (clip.intersects(fi.getBounds()))
                fi.render(g);
        }
    }

    /**
     * Override so that nothing is needed to be done for added RenderableInteractions.
     */
    @Override
    protected void validateCompartmentSetting(Renderable r) {
        return; 
    }
}