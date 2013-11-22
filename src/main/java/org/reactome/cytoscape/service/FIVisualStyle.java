package org.reactome.cytoscape.service;

import java.awt.Color;

import javax.swing.JMenuItem;

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
    void createVisualStyle(CyNetworkView view);
    int [] getSampleNumberRange(CyNetworkView view);
    JMenuItem getyFilesOrganic();
    void setLayout();
}
