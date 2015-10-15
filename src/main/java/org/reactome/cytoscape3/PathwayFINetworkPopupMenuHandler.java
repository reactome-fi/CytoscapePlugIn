package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.reactome.cytoscape.pathway.DiagramAndNetworkSwitcher;

/**
 * Some extra work needs for a FI network converted from a pathway diagram.
 * 
 */
public class PathwayFINetworkPopupMenuHandler extends FINetworkPopupMenuHandler {
    
    public PathwayFINetworkPopupMenuHandler() {
    }
    
    @Override
    protected void installMenus() {
        super.installMenus();
        NetworkToDiagramMenu networkToDigramMenu = new NetworkToDiagramMenu();
        installOtherNetworkMenu(networkToDigramMenu,
                        "Convert to Diagram");
    }

    // A way to convert from FI network view back to Reactome diagram view
    private class NetworkToDiagramMenu implements CyNetworkViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView networkView) {
            JMenuItem menuItem = new JMenuItem("Convert to Diagram");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    DiagramAndNetworkSwitcher helper = new DiagramAndNetworkSwitcher();
                    helper.convertToDiagram(networkView);
                }
            });
            CyMenuItem rtn = new CyMenuItem(menuItem, 1.5f);
            return rtn;
        }
    };
}
