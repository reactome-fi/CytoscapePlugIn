package org.reactome.cytoscape3;


import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;

public class NodeActionCollection
{

    class GeneCardMenu implements CyNodeViewContextMenuFactory
    {
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView, final View<CyNode> nodeView)
        {
            JMenuItem geneCardMenuItem = new JMenuItem("Gene Card");
            geneCardMenuItem.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    Long nodeID = nodeView.getModel().getSUID();
                    String id = netView.getModel().getDefaultNodeTable().getRow(nodeID).get("name", String.class);
                    String url = "http://www.genecards.org/cgi-bin/carddisp.pl?gene=" + id;
                    PlugInUtilities.openURL(url);
                }
                
            });
            return new CyMenuItem(geneCardMenuItem, 1.0f);
        } 
    }


}
