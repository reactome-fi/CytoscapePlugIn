package org.reactome.cytoscape3;

import org.cytoscape.model.CyNetwork;

/**
 * This class provides layout setup for a given
 * network view. Without it, all a user sees is what
 * looks to be a single node (this is a common bug in
 * older Cytoscape plugin (e.g. 2.7) releases.
 * @author Eric T. Dawson
 *@date July 2013
 */
public class VisualStyleHelper
{
    private final String FI_VISUAL_STYLE = "FI Network";
    
    public VisualStyleHelper()
    {
        
    }
    
    private void setVisualStyle(CyNetwork network)
    {
        //Set the node color
        
        //Give the node a label
        
        //Set the node size
        
        //Set the desired layout (yFiles Organic)
        
    }
}
