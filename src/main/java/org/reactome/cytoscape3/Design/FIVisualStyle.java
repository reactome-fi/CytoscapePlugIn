package org.reactome.cytoscape3.Design;

import javax.swing.JMenuItem;

import org.cytoscape.view.model.CyNetworkView;

public interface FIVisualStyle
{
    void setVisualStyle(CyNetworkView view);
    void createVisualStyle(CyNetworkView view);
    int [] getSampleNumberRange(CyNetworkView view);
    JMenuItem getyFilesOrganic();
    void setLayout();
}
