/*
 * Created on Mar 5, 2014
 *
 */
package org.reactome.cytoscape.service;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.ServiceRegistration;

/**
 * A very simple implementation of PopupMenuHandler.
 * @author gwu
 *
 */
public abstract class AbstractPopupMenuHandler implements PopupMenuHandler {
    // This may not be a good idea to push our popup menus to the top-level,
    // instead of add as sub-menus to apps. However, I really don't want our
    // use to see that very long "Extend Network by public interaction database...".
    // 100 is based on numbers defined in interface org.cytoscape.work.ServiceProperties.
    protected final String PREFERRED_MENU = "Reactome FI[100]";
    private boolean isInstalled;
    protected List<ServiceRegistration> menuRegistrations;
    
    /**
     * Default constructor.
     */
    public AbstractPopupMenuHandler() {
        menuRegistrations = new ArrayList<ServiceRegistration>();
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.PopupMenuHandler#install()
     */
    @Override
    public void install() {
        if (isInstalled)
            return;
        installMenus();
        isInstalled = true;
    }
    
    /**
     * This is the actual method to install menus.
     */
    protected abstract void installMenus();
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.PopupMenuHandler#uninstall()
     */
    @Override
    public void uninstall() {
        if (!isInstalled)
            return;
        uninstallMenus();
        isInstalled = false;
    }
    
    /**
     * This is the actual method to uninstall menus.
     */
    protected void uninstallMenus() {
        // Just unregister all registered menus
        for (ServiceRegistration registration : menuRegistrations)
            registration.unregister();
        menuRegistrations.clear();
    }
    
    /* (non-Javadoc)
     * @see org.reactome.cytoscape.service.PopupMenuHandler#isInstalled()
     */
    @Override
    public boolean isInstalled() {
        return isInstalled;
    }
    
}
