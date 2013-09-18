package org.reactome.cytoscape.service;

import javax.swing.JMenuItem;

import org.cytoscape.view.model.CyNetworkView;

/**
 * An interface that is related to visual style setting and auto-layout related to the FI network.
 * @author gwu
 *
 */
public interface FIVisualStyle
{
    void setVisualStyle(CyNetworkView view);
    void createVisualStyle(CyNetworkView view);
    int [] getSampleNumberRange(CyNetworkView view);
    JMenuItem getyFilesOrganic();
    void setLayout();
}
