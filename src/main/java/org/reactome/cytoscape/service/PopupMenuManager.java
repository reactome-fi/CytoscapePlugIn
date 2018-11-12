/*
 * Created on Mar 4, 2014
 *
 */
package org.reactome.cytoscape.service;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.osgi.framework.BundleContext;
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
    // Since there is no API in Cytoscape that can be used to get the current
    // selected CyNetworkView, this variable is used to cache this information
    private CyNetworkView currentNetworkView;
    
    public static PopupMenuManager getManager() {
        if (manager == null)
            manager = new PopupMenuManager();
        return manager;
    }
    
    /**
     * The sole private constructor.
     */
    private PopupMenuManager() {
        typeToHandler = new HashMap<ReactomeNetworkType, PopupMenuHandler>();
        
        // Add a listener for NewtorkView selection
        SetCurrentNetworkViewListener currentNetworkViewListener = new SetCurrentNetworkViewListener() {
            
            @Override
            public void handleEvent(SetCurrentNetworkViewEvent event) {
                currentNetworkView = event.getNetworkView();
                if (event.getNetworkView() == null)
                    return; // This is more like a Pathway view
                CyNetwork network = event.getNetworkView().getModel();
                // Check the ReactomeNetworkType
                ReactomeNetworkType type = new TableHelper().getReactomeNetworkType(network);
                if (type == null)
                    type = ReactomeNetworkType.FINetwork; // Default
                installPopupMenu(type);
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(SetCurrentNetworkViewListener.class.getName(),
                                currentNetworkViewListener,
                                null);
    }
    
    /**
     * Get the current selected CyNetworkView in the whole Cytoscape application.
     * @return
     */
    public CyNetworkView getCurrentNetworkView() {
        return this.currentNetworkView;
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
    
}
