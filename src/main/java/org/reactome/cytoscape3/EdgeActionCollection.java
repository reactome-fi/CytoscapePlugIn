package org.reactome.cytoscape3;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

public class EdgeActionCollection
{

    class queryFISourceMenu implements CyEdgeViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView arg0, View<CyEdge> arg1)
        {
            JMenuItem queryFIMenuItem = new JMenuItem("Query FI Source");
            
            return new CyMenuItem(queryFIMenuItem, 1.0f);
        }
        
    }
}
