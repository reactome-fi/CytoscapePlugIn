/*
 * Created on Mar 4, 2014
 *
 */
package org.reactome.cytoscape.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This singleton class is used to manager network popup menu.
 * @author gwu
 *
 */
public class PopupMenuManager {
    
    private static PopupMenuManager manager;
    // Used to register PopupMenuHandler for different network
    private Map<ReactomeNetworkType, PopupMenuHandler> typeToHandler;
    // Keep these two menus in order to turn on/off
    private CyNetworkViewContextMenuFactory fiAnnotMenu;
    private CyNetworkViewContextMenuFactory convertToDiagramMenu;
    private Map<CyNetworkViewContextMenuFactory, ServiceRegistration> menuToRegistration;
    
    public static PopupMenuManager getManager() {
        if (manager == null)
            manager = new PopupMenuManager();
        return manager;
    }
    
    public void setConvertToDiagramMenu(CyNetworkViewContextMenuFactory menu) {
        this.convertToDiagramMenu = menu;
    }

    public void setFiAnnotMenu(CyNetworkViewContextMenuFactory fiAnnotMenu) {
        this.fiAnnotMenu = fiAnnotMenu;
    }
    
    /**
     * The sole private constructor.
     */
    private PopupMenuManager() {
        typeToHandler = new HashMap<ReactomeNetworkType, PopupMenuHandler>();
        menuToRegistration = new HashMap<CyNetworkViewContextMenuFactory, ServiceRegistration>();
        // Add a listener for NewtorkView selection
        SetCurrentNetworkViewListener currentNetworkViewListener = new SetCurrentNetworkViewListener() {
            
            @Override
            public void handleEvent(SetCurrentNetworkViewEvent event) {
                if (event.getNetworkView() == null)
                    return; // This is more like a Pathway view
                CyNetwork network = event.getNetworkView().getModel();
                // Check if this network is a converted
                CyRow row = network.getDefaultNetworkTable().getRow(network.getSUID());
                String dataSetType = row.get("dataSetType",
                                             String.class);
                if ("PathwayDiagram".equals(dataSetType)) {
                    // Don't need this annotation
                    uninstallContextMenu(PopupMenuManager.this.fiAnnotMenu);
                    installContextMenu(PopupMenuManager.this.convertToDiagramMenu,
                                       "Convert to Diagram");
                }
                else {
                    installContextMenu(PopupMenuManager.this.fiAnnotMenu,
                                       "Fetch FI Annotations");
                    uninstallContextMenu(PopupMenuManager.this.convertToDiagramMenu);
                }
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(SetCurrentNetworkViewListener.class.getName(),
                                currentNetworkViewListener,
                                null);
    }
    
    /**
     * Install the previously registered PopupMenuHandler for a specified ReactomeNetworkType.
     * The returned PopupMenuHandler will install its popup menu, and popup menus in other
     * PopupMenuHandler will be uninstalled.
     * @param type
     * @return a null may be returned if no PopupMenuHandler has been registered for the 
     * specified ReactomeNetworkType.
     */
    public PopupMenuHandler installPopupMenu(ReactomeNetworkType type) {
        PopupMenuHandler handler = typeToHandler.get(type);
        if (handler == null)
            return null; // Just return a null.
        // Uninstall menus in other PopupMenuHandler
        for (PopupMenuHandler tmpHandler : typeToHandler.values()) {
            if (tmpHandler == handler)
                continue;
            if (!tmpHandler.isInstalled())
                continue;
            tmpHandler.uninstall();
        }
        handler.install();
        return handler;
    }

    /**
     * Register a PopupMenuHandler to a ReactomeNetworkType. Previously registered
     * PopupMenuHandler will be overwritten.
     * @param type
     * @param handler
     */
    public void registerMenuHandler(ReactomeNetworkType type,
                                    PopupMenuHandler handler) {
        typeToHandler.put(type, handler);
    }
    
    /**
     * Install a "Fetch FI Annotations" menu
     */
    private void installContextMenu(CyNetworkViewContextMenuFactory menu,
                                    String title) {
        if (menu == null)
            return;
        ServiceRegistration registration = menuToRegistration.get(menu);
        if (registration != null)
            return; // It has been registered already
        Properties fiFetcherProps = new Properties();
        fiFetcherProps.setProperty("title", title);
        fiFetcherProps.setProperty("preferredMenu", "Apps.Reactome FI");
        // Want to keep the registration of this menu in order to turn it off
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        registration = context.registerService(CyNetworkViewContextMenuFactory.class.getName(), 
                                               menu, 
                                               fiFetcherProps);
        menuToRegistration.put(menu, registration);
    }
    
    private void uninstallContextMenu(CyNetworkViewContextMenuFactory menu) {
        if (menu == null)
            return;
        ServiceRegistration registration = menuToRegistration.get(menu);
        if (registration == null)
            return; // It has unregistered already
        registration.unregister();
        menuToRegistration.remove(menu);
    }
    
}
