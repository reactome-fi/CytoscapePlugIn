/*
 * Created on Jul 15, 2013
 *
 */
package org.reactome.cytoscape3;

import java.awt.Component;

import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.util.swing.OpenBrowser;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Utility methods that can be used by Reactome FI plug-in have been grouped
 * here.
 * 
 * @author gwu
 * 
 */
public class PlugInUtilities
{

    public PlugInUtilities()
    {
    }

    /**
     * Show an error message
     * 
     * @param message
     */
    public static void showErrorMessage(String title, String message)
    {
        // Need a parent window to display error message
        Component parent = null;
        BundleContext context = PlugInScopeObjectManager.getManager()
                .getBundleContext();
        ServiceReference serviceReference = context
                .getServiceReference(CySwingApplication.class.getName());
        if (serviceReference != null)
        {
            CySwingApplication cytoscape = (CySwingApplication) context
                    .getService(serviceReference);
            if (cytoscape != null)
            {
                parent = cytoscape.getJFrame();
            }
        }
        JOptionPane.showMessageDialog(parent, message, title,
                JOptionPane.ERROR_MESSAGE);
        if (serviceReference != null)
        { // Unget the service and null serviceReference
            context.ungetService(serviceReference);
            serviceReference = null;
        }
    }

    /**
     * Open an OS web browser to display the passed URL.
     * 
     * @param url
     */
    public static void openURL(String url)
    {
        BundleContext context = PlugInScopeObjectManager.getManager()
                .getBundleContext();
        ServiceReference serviceReference = context
                .getServiceReference(OpenBrowser.class.getName());
        boolean isOpened = false;
        if (serviceReference != null)
        {
            OpenBrowser browser = (OpenBrowser) context
                    .getService(serviceReference);
            if (browser != null)
            {
                browser.openURL(url);
                isOpened = true;
            }
            context.ungetService(serviceReference);
        }
        // In case the passed URL cannot be opened!
        if (!isOpened)
        {
            showErrorMessage("Error in Opening URL",
                    "Error in opening URL: cannot find a configured browser in Cytoscape!");
        }
    }

}
