/*
 * Created on May 8, 2009
 *
 */
package org.reactome.cytoscape3;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Properties;

import javax.swing.WindowConstants;

import org.cytoscape.application.swing.CySwingApplication;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cancerindex.model.CancerIndexSentenceDisplayFrame;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * A singleton to manage other singleton objects, and some utility methods.
 * 
 * @author wgm ported July 2013 by Eric T Dawson
 */
public class PlugInScopeObjectManager
{
    private static PlugInScopeObjectManager manager;
    // Don't cache it in case FI network version has been changed
    private FINetworkService networkService;
    // Try to track CancerIndexSentenceDisplayFrame
    private CancerIndexSentenceDisplayFrame cgiFrame;
    // Currently selected FI network version
    private String fiNetworkVersion;
    private String userGuideURL = "http://wiki.reactome.org/index.php/Reactome_FI_Cytoscape_Plugin";

    private PlugInScopeObjectManager()
    {
    }

    public static PlugInScopeObjectManager getManager()
    {
        if (manager == null)
        {
            manager = new PlugInScopeObjectManager();
        }
        return manager;
    }
    
    public BundleContext getBundleContext() {
        return PlugInObjectManager.getManager().getBundleContext();
    }

    public String getFiNetworkVersion()
    {
        return this.fiNetworkVersion;
    }

    public String getDefaultFINeworkVersion()
    {
        Properties prop = getProperties();
        String fiVersions = prop.getProperty("FINetworkVersions");
        String[] tokens = fiVersions.split(",");
        for (String token : tokens)
        {
            token = token.trim();
            if (token.toLowerCase().contains("default")) return token;
        }
        return null;
    }

    public void setFiNetworkVersion(String fiNetworkVersion)
    {
        this.fiNetworkVersion = fiNetworkVersion;
    }

    public CancerIndexSentenceDisplayFrame getCancerIndexFrame(
            CySwingApplication desktopApp)
    {
        if (cgiFrame == null)
        {
            cgiFrame = new CancerIndexSentenceDisplayFrame();
            cgiFrame.setTitle("Cancer Index Annotations");
            cgiFrame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            cgiFrame.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                    cgiFrame = null; // Enable to GC.
                }
            });
            cgiFrame.setSize(800, 600);
            cgiFrame.setLocationRelativeTo(desktopApp.getJFrame());
            cgiFrame.setVisible(true);
        }
        else
        {
            cgiFrame.setState(Frame.NORMAL);
            cgiFrame.toFront();
        }
        return cgiFrame;
    }

    public Properties getProperties()
    {
        return PlugInObjectManager.getManager().getProperties();
    }

    public FINetworkService getNetworkService() throws Exception
    {
        Properties prop = getProperties();
        String clsName = prop.getProperty("networkService",
                "org.reactome.cytoscape3.LocalService");
        FINetworkService networkService = (FINetworkService) Class.forName(
                clsName).newInstance();
        // FINetworkService networkService = (FINetworkService) new
        // LocalService();
        return networkService;
    }

    /**
     * Get the RESTful URL
     * 
     * @param fiVersion
     * @return
     */
    public String getRestfulURL(String fiVersion)
    {
        fiVersion = fiVersion.replaceAll(" ", "_");
        String key = fiVersion + "_restfulURL";
        Properties prop = getProperties();
        return prop.getProperty(key);
    }

    public String getRestfulURL()
    {
        return getRestfulURL(getFiNetworkVersion());
    }

    public String getDataSourceURL(String fiVersion)
    {
        String dataSourceURL = getProperties().getProperty("dataSourceURL");
        fiVersion = fiVersion.replaceAll(" ", "_");
        String dbName = getProperties().getProperty(fiVersion + "_sourceDb");
        String rtn = dataSourceURL.replace("${DB_NAME}", dbName);
        return rtn;
    }

    public String getDataSourceURL()
    {
        return getDataSourceURL(getFiNetworkVersion());
    }

    //A lot of getter methods for retrieving the references to various cytoscape services.
    public CySwingApplication getCySwingApp()
    {
        CySwingApplication desktopApp = null;
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference servRef = context.getServiceReference(CySwingApplication.class.getName());
        if (servRef != null)
        {
            desktopApp = (CySwingApplication) context.getService(servRef);
        }
        return desktopApp;
    }
    
    //A method to unget a service reference and release it for garbage collecting.
    public void releaseService(ServiceReference serviceRef)
    {
        if (serviceRef != null)
        {
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            context.ungetService(serviceRef);
            serviceRef = null;
        }
    }
    
}
