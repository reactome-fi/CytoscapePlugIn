/*
 * Created on Feb 18, 2014
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

import org.gk.render.DefaultFlowLineRenderer;
import org.gk.render.InteractionType;
import org.gk.render.Node;
import org.gk.render.RenderableGene;
import org.gk.render.RendererFactory;
import org.gk.util.DrawUtilities;
import org.reactome.cytoscape.pathway.FIRenderableInteraction.FIDirectionType;

/**
 * A customized Renderer to render FIRenderableDirection.
 * @author gwu
 *
 */
public class FIRenderableInteractionRenderer extends DefaultFlowLineRenderer {
    private static FIRenderableInteractionRenderer renderer;
    
    /**
     * A static method to initialize a single object only.
     */
    public static void initialize() {
        if (renderer != null)
            return;
        renderer = new FIRenderableInteractionRenderer();
        RendererFactory.getFactory().registerRenderer(FIRenderableInteraction.class,
                                                      renderer);
    }
    
    
    /**
     * Default Constructor. Use a private constructor to make sure
     * only one Renderer is initialized.
     */
    private FIRenderableInteractionRenderer() {
    }


    @Override
    protected void drawInteractionType(Point outputHub, 
                                       InteractionType type,
                                       List<Point> backbonePoints,
                                       Graphics2D g2) {
        FIRenderableInteraction interaction = (FIRenderableInteraction) reaction;
        FIDirectionType inputDir = interaction.getInputDirection();           
        Point controlPoint = backbonePoints.get(1);
        Point position = backbonePoints.get(0);
        if (inputDir == FIDirectionType.ARROW) {
            DrawUtilities.drawHollowArrow(position, controlPoint, g2);
        }
        else if (inputDir == FIDirectionType.T) {
            Node node = interaction.getInputNode(0);
            drawInhibitorSymbol(position, node, g2);
        }
        // For output
        FIDirectionType outputDir = interaction.getOutputDirection();
        position = backbonePoints.get(backbonePoints.size() - 1);
        controlPoint = backbonePoints.get(backbonePoints.size() - 2);
        if (outputDir == FIDirectionType.ARROW) {
            DrawUtilities.drawHollowArrow(position, controlPoint, g2);
        }
        else if (outputDir == FIDirectionType.T) {
            Node node = interaction.getOutputNode(0);
            drawInhibitorSymbol(position, node, g2);
        }
    }
    
    private void drawInhibitorSymbol(Point position, 
                                     Node node,
                                     Graphics2D g2) {
        if (node == null || node.getBounds() == null)
            return;
        Rectangle bounds = node.getBounds();
        int y = position.y;
        if ((y < bounds.getY()) || 
            (y > bounds.getMaxY())) {
            // use horizontal line
            int x1 = position.x - CIRCLE_SIZE;
            int x2 = position.x + CIRCLE_SIZE;
            g2.drawLine(x1, position.y, x2, position.y);
        }
        else {
            // Draw vertical line
            int y1 = position.y - CIRCLE_SIZE;
            int y2 = position.y + CIRCLE_SIZE;
            g2.drawLine(position.x, y1,
                        position.x, y2);
        }
    } 
    
}
