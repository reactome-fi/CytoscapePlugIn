package org.reactome.cytoscape.service;

import java.awt.Color;

import org.cytoscape.view.model.CyNetworkView;

/**
 * An interface that is related to visual style setting and auto-layout related to the FI network.
 * @author gwu
 *
 */
public interface FIVisualStyle {
    // Some pre-defined colors
    public static final Color NODE_HIGHLIGHT_COLOR = new Color(138, 43, 126); // A kind of purple

    void setVisualStyle(CyNetworkView view);
    
    /**
     * 
     * @param view
     * @param createStyle true to create a new VisualStyle from cratch. Otherwise, use an existing one.
     */
    void setVisualStyle(CyNetworkView view, boolean createStyle);
    
    void setLayout();
}
