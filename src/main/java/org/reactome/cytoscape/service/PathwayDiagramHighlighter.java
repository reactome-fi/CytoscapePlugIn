/*
 * Created on Jun 19, 2012
 *
 */
package org.reactome.cytoscape.service;

import java.awt.Color;
import java.util.Map;

import org.gk.render.Node;
import org.gk.render.Renderable;
import org.gk.render.RenderablePathway;

/**
 * This class is used to highlight pathway diagrams based on IPAs or p-values. The color generation
 * algorithm is adapted from Java source code, java.awt.GradientPaintContext. An array of colors
 * are pre-generated according to that class.
 * @author gwu
 *
 */
public class PathwayDiagramHighlighter {
    private int[] colors;
    // Default colors
    private Color minColor = Color.GREEN;
    private Color maxColor = Color.RED;
    // A flag to indicate if this is for reaction
    private boolean isForReaction;
    
    public PathwayDiagramHighlighter() {
    }
    
    public boolean isForReaction() {
        return isForReaction;
    }

    public void setForReaction(boolean isForReaction) {
        this.isForReaction = isForReaction;
    }

    public void setMinColor(Color minColor) {
        this.minColor = minColor;
    }
    
    public void setMaxColor(Color maxColor) {
        this.maxColor = maxColor;
    }
    
    public Color getMinColor() {
        return minColor;
    }

    public Color getMaxColor() {
        return maxColor;
    }

    /**
     * Pre-generate an array of colors in int based on the original Java source code.
     */
    private void initColors() {
        if (minColor == null || maxColor == null)
            throw new IllegalStateException("Set minColor and maxColor first!");
        int color1 = minColor.getRGB();
        int r1 = (color1 >> 16) & 0xff;
        int g1 = (color1 >> 8) & 0xff;
        int b1 = (color1) & 0xff;
        int color2 = maxColor.getRGB();
        int dr = ((color2 >> 16) & 0xff) - r1;
        int dg = ((color2 >> 8) & 0xff) - g1;
        int db = ((color2) & 0xff) - b1;
        colors = new int[257]; // Since we don't use the cyclic mode, 257 colors are pre-generated
        for (int i = 0; i <= 256; i++) {
            float rel = i / 256.0f;
            int rgb = (((int) (r1 + rel * dr)) << 16) |
                      (((int) (g1 + rel * dg)) << 8) |
                      (((int) (b1 + rel * db)));
            colors[i] = rgb;
        }
    }
    
    /**
     * Assign colors to displayed PEs with ids in the passed map. If a PE doesn't have an value,
     * white color will be assigned. The color gradient used is from red (highest) to green (lowest).
     * @param diagram
     * @param idToValue
     * @param min
     * @param max
     * @throws Exception
     */
    public void highlightELV(RenderablePathway diagram,
                             Map<String, Double> idToValue,
                             double min,
                             double max) {
        if (colors == null)
            initColors();
        if (isForReaction)
            hiliteEdges(diagram, idToValue, min, max);
        else
            hiliteNodes(diagram, idToValue, min, max);
    }
    
    private void hiliteEdges(RenderablePathway diagram, Map<String, Double> idToValue, double min, double max) {
        for (Object obj : diagram.getComponents()) {
            Renderable r = (Renderable) obj;
            if (r instanceof Node) {
                r.setBackgroundColor(Color.WHITE);
                continue; // Reset all nodes to white for easy results view
            }
            if (!(r instanceof org.gk.render.HyperEdge))
                continue; 
            Long dbId = r.getReactomeId();
            if (dbId == null || !idToValue.containsKey(dbId.toString())) {
                r.setLineColor(Color.LIGHT_GRAY); // Use this as a background
                continue;
            }
            Double value = idToValue.get(dbId.toString());
            Color color = getColor(value, min, max);
            r.setLineColor(color);
        }
    }
    
    public Color getColor(Double value, double min, double max) {
        if (colors == null)
            initColors();
        // In case this value is out-of-range
        if (value > max)
            value = max;
        if (value < min)
            value = min;
        double rel = (value - min) / (max - min);
        // Add 0.5d to round half up
        int index = (int) (rel * (colors.length - 1) + 0.5d); // The last index should be --.
        Color color = new Color(colors[index]);
        return color;
    }

    private void hiliteNodes(RenderablePathway diagram, Map<String, Double> idToValue, double min, double max) {
        for (Object obj : diagram.getComponents()) {
            Renderable r = (Renderable) obj;
            if (!(r instanceof org.gk.render.Node))
                continue; // Want to work on nodes only
//            r.setForegroundColor(Color.LIGHT_GRAY);
            // Better to use black, which is the default.
//            r.setForegroundColor(Color.BLACK);
            Long dbId = r.getReactomeId();
            if (dbId == null || !idToValue.containsKey(dbId.toString())) {
                r.setBackgroundColor(Color.WHITE);
                continue;
            }
            Double value = idToValue.get(dbId.toString());
            Color color = getColor(value, min, max);
            r.setBackgroundColor(color);
//            System.out.println(r.getDisplayName() + "\t" + value + 
//                               "\t" + index + 
//                               "\t" + color);
        }
    }
    
    /**
     * Color pathway diagram using the values from the map of idToValue.
     * @param diagram
     * @param idToValue
     * @throws Exception
     */
    public void highlightELV(RenderablePathway diagram,
                             Map<String, Double> idToValue) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (Double value : idToValue.values()) {
            if (value > max)
                max = value;
            if (value < min)
                min = value;
        }
        highlightELV(diagram, idToValue, min, max);
    }
    
    public int[] getColorSpetrum() {
        if (colors == null)
            initColors();
        return colors;
    }
    
}
