/*
 * Created on Sep 28, 2016
 *
 */
package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.ServiceProperties;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.service.ReactomeSourceView;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * Show popup menu for Reaction networks.
 * @author gwu
 *
 */
public class ReactionNetworkPopupMenuHandler extends FINetworkPopupMenuHandler {
    
    public ReactionNetworkPopupMenuHandler() {
    }

    @Override
    protected void installMenus() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        
        NetworkPathwayEnrichmentMenu netPathMenu = new NetworkPathwayEnrichmentMenu();
        Properties netPathProps = new Properties();
        netPathProps.setProperty(ServiceProperties.TITLE, "Network Pathway Enrichment");
        String preferredMenuText = PREFERRED_MENU + ".Analyze Network Functions[10]";
        netPathProps.setProperty(ServiceProperties.PREFERRED_MENU, preferredMenuText);
        addPopupMenu(context, netPathMenu, CyNetworkViewContextMenuFactory.class, netPathProps);
        
        ReactioneFetchMenu reactionMenu = new ReactioneFetchMenu();
        Properties props = new Properties();
        props.setProperty(ServiceProperties.TITLE, "Fetch Reaction");
        props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, reactionMenu, CyNodeViewContextMenuFactory.class, props);
    }
    
    private class ReactioneFetchMenu implements CyNodeViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView,
                                         final View<CyNode> nodeView) {
            JMenuItem fetchReaction = new JMenuItem("View Reactome Source");
            fetchReaction.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    fetchReaction(netView, nodeView);
                }
            });

            return new CyMenuItem(fetchReaction, 
                                  1.0f);
        }
        
        private void fetchReaction(CyNetworkView netView,
                                   View<CyNode> nodeView) {
            // Need to extract the Reaction DB_ID
            CyTable nodeTable = netView.getModel().getDefaultNodeTable();
            Long nodeSUID = nodeView.getModel().getSUID();
            String nodeName = nodeTable.getRow(nodeSUID).get("name", String.class);
            ReactomeSourceView view = new ReactomeSourceView();
            view.viewReactomeSource(new Long(nodeName), // DB_ID is used for node name
                                    PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                    false);
        }
        
    }
    
    
    
}
