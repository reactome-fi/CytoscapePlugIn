/*
 * Created on Mar 4, 2014
 *
 */
package org.reactome.cytoscape.service;

/**
 * This interface is used to handle popup menu for the Reactome network.
 * Different networks from Reactome may have different popup menus. So each Reactome
 * network type should have its own implementation of PopupMenuHandler.
 * @author gwu
 *
 */
public interface PopupMenuHandler {
    
    /**
     * Install a popup menu to the build-in Cytoscape network popup menu. After this method is
     * called, true should be returned from method isInstalled().
     */
    public void install();
    
    /**
     * Remove a previously installed popup menu from the build-in Cytoscape network popup menu.
     * After this method is called, false should be returned from method isInstalled().
     */
    public void uninstall();
    
    /**
     * Check if the popup menu has been installed.
     * @return
     */
    public boolean isInstalled();
    
}
