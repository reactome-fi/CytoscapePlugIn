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

import org.gk.graphEditor.PathwayEditor;
import org.gk.render.HyperEdge;
import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderableInteraction;

/**
 * A customized PathwayEditor in order to do something special for PathwayDiagram displayed in
 * Cytoscape.
 * @author gwu
 */
public class CyPathwayEditor extends PathwayEditor {
    // Record newly added RenderableInteraction for new drawing
    private List<FIRenderableInteraction> overlaidFIs;
    
    public CyPathwayEditor() {
        setEditable(false); // Only used as a pathway diagram view and not for editing
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
    
    /**
     * Remove all overlaid FIs.
     */
    public void removeFIs() {
        if (overlaidFIs == null || overlaidFIs.size() == 0)
            return;
       for (RenderableInteraction fi : overlaidFIs) {
           Node input = fi.getInputNode(0);
           Node output = fi.getOutputNode(0);
           delete(fi);
           // Check if a connected node should be deleted too
           if (input.getReactomeId() == null && input.getConnectedReactions().size() == 0)
               delete(input);
           if (output.getReactomeId() == null && output.getConnectedReactions().size() == 0)
               delete(output);
       }
       overlaidFIs.clear();
       repaint(getVisibleRect());
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
    
    @Override
    public void insertEdge(HyperEdge edge, boolean useDefaultInsertPos) {
        super.insertEdge(edge, useDefaultInsertPos);
        if (edge instanceof FIRenderableInteraction) {
            if (overlaidFIs == null)
                overlaidFIs = new ArrayList<FIRenderableInteraction>();
            overlaidFIs.add((FIRenderableInteraction)edge);
        }
    }
    
    @Override
    public void paint(Graphics g) {
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
        Dimension size = getPreferredSize();
        // Preferred size has been scaled. Need to scale it back
        g2.fillRect(0, 
                    0, 
                    (int)(size.width / scaleX + 1.0d), 
                    (int)(size.height / scaleY + 1.0d));
        // Draw overlaid FIs and their associated Objects
        Set<Node> nodes = new HashSet<Node>();
        for (RenderableInteraction fi : overlaidFIs) {
            nodes.addAll(fi.getInputNodes());
            nodes.addAll(fi.getOutputNodes());
        }
        Rectangle clip = g.getClipBounds();
        for (Node node : nodes) {
            node.validateBounds(g);
            if (clip.intersects(node.getBounds()))
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