package org.reactome.cytoscape.util;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

public interface NodeUtilities
{
    abstract public void selectNode(CyNode node, CyNetwork network);
}
