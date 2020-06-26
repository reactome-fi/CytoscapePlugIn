/*
 * Created on Sep 28, 2016
 *
 */
package org.reactome.cytoscape3;

import java.util.List;
import java.util.Properties;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.ServiceProperties;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.sc.ScNetworkManager;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * Show popup menu for Reaction networks.
 * @author gwu
 *
 */
public class ScNetworkPopupMenuHandler extends FINetworkPopupMenuHandler {
    
    public ScNetworkPopupMenuHandler() {
    }

    @Override
    protected void installMenus() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        
        Properties props = new Properties();
        props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, 
                     new GeneExpressionMenu(), 
                     CyNetworkViewContextMenuFactory.class, 
                     props);
        
        // Add menus for loading cell features
        List<String> cellFeatures = ScNetworkManager.getManager().getCellFeatureNames();
        if (cellFeatures != null && cellFeatures.size() > 0) {
            props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU + ".Load Cell Feature[5.0]");
            for (String feature : cellFeatures) {
                CyNetworkViewContextMenuFactory menuFactory = view -> {
                    JMenuItem menuItem = new JMenuItem(feature);
                    menuItem.addActionListener(e -> ScNetworkManager.getManager().loadCellFeature(feature));
                    return new CyMenuItem(menuItem, cellFeatures.indexOf(feature));
                };
                addPopupMenu(context, menuFactory, CyNetworkViewContextMenuFactory.class, props);
            }
        }
        
        props.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(context, new DPTAnalysis(), CyNetworkViewContextMenuFactory.class, props);
        
        addPopupMenu(context, 
                     new ToggleEdgesDisplay(),
                     CyNetworkViewContextMenuFactory.class,
                     props);
        
    }
    
    private class GeneExpressionMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem loadGeneMenuItem = new JMenuItem("Load Gene Expression");
            loadGeneMenuItem.addActionListener(e -> loadGeneExpression());
            return new CyMenuItem(loadGeneMenuItem, 1.0f);
        }
        
        private void loadGeneExpression() {
            String gene = JOptionPane.showInputDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                      "Enter a gene:",
                                                      "Enter Gene",
                                                      JOptionPane.PLAIN_MESSAGE);
            if (gene == null)
                return;
            ScNetworkManager.getManager().loadGeneExp(gene);
        }
        
    }
    
    private class ToggleEdgesDisplay implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            Boolean isEdgeDisplayed = ScNetworkManager.getManager().isEdgeDisplayed();
            if (isEdgeDisplayed == null)
                return null; // Don't need to show this menu
            String title = null;
            if (isEdgeDisplayed)
                title = "Hide Edges";
            else
                title = "Show Edges";
            JMenuItem netPathMenuItem = new JMenuItem(title);
            netPathMenuItem.addActionListener(e -> ScNetworkManager.getManager().setEdgesVisible(!isEdgeDisplayed));
            return new CyMenuItem(netPathMenuItem, 20.0f);
        }
        
    }
    
    private class DPTAnalysis implements CyNetworkViewContextMenuFactory {
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem netPathMenuItem = new JMenuItem("Diffusion Pseudotime Analysis");
            netPathMenuItem.addActionListener(e -> performDPT());
            return new CyMenuItem(netPathMenuItem, 7.0f);
        }
        
        private void performDPT() {
            // Choose a cell as the root
            String rootCell = JOptionPane.showInputDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                                      "Enter a cell id as the root:",
                                                      "Enter Cell",
                                                      JOptionPane.PLAIN_MESSAGE);
            if (rootCell == null)
                return;
            ScNetworkManager.getManager().performDPT(rootCell);
        }
    }
    
    
    
}
