/*
 * Created on Mar 4, 2014
 *
 */
package org.reactome.cytoscape.pathway;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.service.AbstractPopupMenuHandler;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.service.ReactomeNetworkType;
import org.reactome.cytoscape.service.ReactomeSourceView;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This PopupMenuHandler is used for a factor graph network.
 * @author gwu
 *
 */
public class FactorGraphPopupMenuHandler extends AbstractPopupMenuHandler {
    // Keep these two menus for install/uninstall based on node type
    private CyNodeViewContextMenuFactory viewVariableMarginalMenu;
    private CyNodeViewContextMenuFactory viewFactorValuesMenu;
    protected Map<CyNodeViewContextMenuFactory, ServiceRegistration> menuToRegistration;
    
    /**
     * Default constructor.
     */
    public FactorGraphPopupMenuHandler() {
        menuToRegistration = new HashMap<CyNodeViewContextMenuFactory, ServiceRegistration>();
        // Add a selection listener in order to switch the menus
        RowsSetListener selectionListener = new RowsSetListener() {
            
            @Override
            public void handleEvent(RowsSetEvent event) {
                // We want to check node selection only
                if (!event.containsColumn(CyNetwork.SELECTED)) {
                    return;
                }
                CyNetworkView currentView = PopupMenuManager.getManager().getCurrentNetworkView();
                if (currentView == null)
                    return;
                List<CyNode> nodes = CyTableUtil.getNodesInState(currentView.getModel(),
                                                                 CyNetwork.SELECTED,
                                                                 true);
                if (nodes.size() != 1)
                    return;
                CyNode node = nodes.get(0);
                doNodePopupMenu(currentView.getModel(),
                                node);
            }

        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(RowsSetListener.class.getName(),
                                selectionListener, 
                                null);
    }
    
    /**
     * Create a node specific popup menu.
     * @param network
     * @param node
     */
    private void doNodePopupMenu(CyNetwork network,
                                 CyNode node) {
        TableHelper tableHelper = new TableHelper();
        ReactomeNetworkType networkType = tableHelper.getReactomeNetworkType(network);
        if (networkType != ReactomeNetworkType.FactorGraph)
            return;
        String nodeType = tableHelper.getStoredNodeAttribute(network,
                                                             node,
                                                             "nodeType",
                                                             String.class);
        if ("factor".equals(nodeType)) {
            uninstallDynamicMenu(viewVariableMarginalMenu);
            installViewFactorValuesMenu();
        }
        else if ("variable".equals(nodeType)) {
            installViewVariableMarginalMenu();
            uninstallDynamicMenu(viewFactorValuesMenu);
        }
        else {
            uninstallDynamicMenu(viewFactorValuesMenu);
            uninstallDynamicMenu(viewVariableMarginalMenu);
        }
    }
    
    private void installViewFactorValuesMenu() {
        if (viewFactorValuesMenu == null)
            viewFactorValuesMenu = new ViewFactorValueMenu();
        installDynamicMenu(viewFactorValuesMenu, "View Factor Values");
    }
    
    private void installViewVariableMarginalMenu() {
        if (viewVariableMarginalMenu == null)
            viewVariableMarginalMenu = new ViewVariableMarginalMenu();
        installDynamicMenu(viewVariableMarginalMenu, "View Marginal Probabilities");
    }
    
    private void installDynamicMenu(CyNodeViewContextMenuFactory menu,
                                    String title) {
        ServiceRegistration registration = menuToRegistration.get(menu);
        if (registration != null)
            return; // This menu has been installed.
        Properties props = new Properties();
        props.setProperty("title", title);
        props.setProperty("preferredMenu", "Apps.Reactome FI");
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        registration = context.registerService(CyNodeViewContextMenuFactory.class.getName(),
                                               menu,
                                               props);
        menuToRegistration.put(menu, registration);
    }
    
    private void uninstallDynamicMenu(CyNodeViewContextMenuFactory menu) {
        if (menu == null)
            return;
        ServiceRegistration registration = menuToRegistration.get(menu);
        if (registration == null)
            return; // This menu has been uninstalled already
        registration.unregister();
        menuToRegistration.remove(menu);
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.PopupMenuHandler#install()
     */
    @Override
    protected void installMenus() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        String preferredMenu = "Apps.Reactome FI";
        
        CyNodeViewContextMenuFactory viewReactomeSourceMenu = new ViewReactomeSourceMenu();
        Properties props = new Properties();
        props.setProperty("title", "View Reactome Source");
        props.setProperty("preferredMenu", preferredMenu);
        ServiceRegistration registration = context.registerService(CyNodeViewContextMenuFactory.class.getName(),
                                                                   viewReactomeSourceMenu,
                                                                   props);
        menuRegistrations.add(registration);
        
        CyNetworkViewContextMenuFactory convertToPathwayMenu = new ConvertToDiagramMenu();
        props = new Properties();
        props.setProperty("title", "Convert to Pathway");
        props.setProperty("preferredMenu", preferredMenu);
        registration = context.registerService(CyNetworkViewContextMenuFactory.class.getName(),
                                               convertToPathwayMenu,
                                               props);
        menuRegistrations.add(registration);
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.PopupMenuHandler#uninstall()
     */
    @Override
    public void uninstallMenus() {
        super.uninstallMenus();
    }
    
    private class ViewFactorValueMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView,
                                         final View<CyNode> nodeView) {
            JMenuItem menuItem = new JMenuItem("View Factor Values");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    viewFactorValues(netView, nodeView);
                }
            });
            return new CyMenuItem(menuItem, 10.0f);
        }
        
        private void viewFactorValues(CyNetworkView netView,
                                      View<CyNode> nodeView) {
            System.out.println("View factor values!");
        }
    }
    
    private class ViewVariableMarginalMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView,
                                         final View<CyNode> nodeView) {
            JMenuItem menuItem = new JMenuItem("View Marginal Probabilities");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    viewVariableMarginal(netView, nodeView);
                }
            });
            return new CyMenuItem(menuItem, 10.0f);
        }
        
        private void viewVariableMarginal(CyNetworkView netView,
                                          View<CyNode> nodeView) {
            System.out.println("View Marginal Probabilities!");
        }
    }
    
    private class ConvertToDiagramMenu implements CyNetworkViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView) {
            JMenuItem menuItem = new JMenuItem("Convert to Pathway");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    convertToPathway(netView);
                }
            });
            return new CyMenuItem(menuItem, 0.0f);
        }
        
        private void convertToPathway(CyNetworkView netView) {
            DiagramAndNetworkSwitcher helper = new DiagramAndNetworkSwitcher();
            helper.convertToDiagram(netView);
        }
        
    }
    
    private class ViewReactomeSourceMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView,
                                         final View<CyNode> nodeView) {
            JMenuItem menuItem = new JMenuItem("View Reactome Source");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent event) {
                    viewReactomeSource(netView, nodeView);
                }
            });
            CyMenuItem rtn = new CyMenuItem(menuItem, 0.0f);
            return rtn;
        }
        
        private void viewReactomeSource(CyNetworkView netView,
                                        View<CyNode> nodeView) {
            // Get the label for the node
            CyNode node = nodeView.getModel();
            TableHelper tableHelper = new TableHelper();
            String nodeLabel = tableHelper.getStoredNodeAttribute(netView.getModel(),
                                                                  node,
                                                                  "name", 
                                                                  String.class);
            if (nodeLabel.matches("(\\d+)")) {
                ReactomeSourceView sourceView = new ReactomeSourceView();
                sourceView.viewReactomeSource(new Long(nodeLabel),
                                              PlugInObjectManager.getManager().getCytoscapeDesktop());
            }
            else {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "The selected node is a derived node from a Reactome pathway object. Please try\n" + 
                                              "to select another node related to this node for information in Reactome.",
                                              "No Information",
                                              JOptionPane.INFORMATION_MESSAGE);
            }
        }
        
    }
    
}
