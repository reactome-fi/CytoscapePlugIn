/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class in the utility packgage is used to manage some plug-in scope objects that are used across
 * the whole Reactome FI plug-in for all features. Some of features in this class are refactored from the 
 * original PlugInScopeObjectManager. This is a singleton as with the original PlugInScopeObjectManager.
 * @author gwu
 *
 */
public class PlugInObjectManager {
    private final Logger logger = LoggerFactory.getLogger(PlugInObjectManager.class);
    // BundleContext for the whole Reactome FI plug-in.
    private BundleContext context;
    // System-wide properties
    private Properties properties;
    private static PlugInObjectManager manager;
    // Record cached ServiceReference so that they can be unget when this bundle (aka) is stopped
    private List<ServiceReference> serviceReferences;
    // Cache CyAppAdapter so that no need to search it multiple times
    private CyAppAdapter appAdapter;
    // Cache the CySwingApplication to be used multiple times
    private CySwingApplication cyApplication;
    // Cache TaskManager since it will be used multiple times
    @SuppressWarnings("rawtypes")
    private TaskManager taskManager;
    // Currently selected FI network version
    private String fiNetworkVersion;
    
    /**
     * Default constructor. This is a private constructor so that the single instance should be used.
     */
    private PlugInObjectManager() {
        serviceReferences = new ArrayList<ServiceReference>();
    }
    
    public static PlugInObjectManager getManager() {
        if (manager == null)
            manager = new PlugInObjectManager();
        return manager;
    }
    
    public String getFiNetworkVersion()
    {
        if (this.fiNetworkVersion != null)
            return this.fiNetworkVersion;
        else
            return getDefaultFINeworkVersion();
    }

    public String getDefaultFINeworkVersion()
    {
        Properties prop = PlugInObjectManager.getManager().getProperties();
        String fiVersions = prop.getProperty("FINetworkVersions");
        String[] tokens = fiVersions.split(",");
        for (String token : tokens)
        {
            token = token.trim();
            if (token.toLowerCase().contains("default")) return token;
        }
        return null;
    }

    public void setFiNetworkVersion(String fiNetworkVersion) {
        this.fiNetworkVersion = fiNetworkVersion;
    }
    
    public void setBundleContext(final BundleContext context) {
        this.context = context;
        context.addBundleListener(new SynchronousBundleListener() {
            
            @Override
            public void bundleChanged(BundleEvent event) {
                if (event.getType() == BundleEvent.STOPPING) {
//                    System.out.println("Bundle is stopping!");
                    if (serviceReferences.size() > 0) {
                        for (ServiceReference reference : serviceReferences) {
                            if (reference != null)
                                context.ungetService(reference);
                        }
                    }
                }
            }
            
        });
    }
    
    public BundleContext getBundleContext() {
        return this.context;
    }
    
    /**
     * Get the system-wide CyAppAdapter that is registered as a service.
     * @return
     */
    public CyAppAdapter getAppAdapter() {
        if (appAdapter == null) {
            ServiceReference reference = context.getServiceReference(CyAppAdapter.class.getName());
            if (reference == null)
                return null;
            appAdapter = (CyAppAdapter) context.getService(reference);
            if (appAdapter != null)
                serviceReferences.add(reference);
        }
        return appAdapter;
    }
    
    /**
     * Get the system-wide TaskManager that is registered as a service
     */
    @SuppressWarnings("rawtypes")
    public TaskManager getTaskManager() {
        if (taskManager != null)
            return taskManager;
        ServiceReference ref = context.getServiceReference(TaskManager.class.getName());
        if (ref == null)
            return null;
        taskManager = (TaskManager) context.getService(ref);
        if (taskManager != null)
            serviceReferences.add(ref);
        return taskManager;
    }
    
    /**
     * Get the system-wide CySwingApplication that is registered as a service.
     * @return
     */
    public CySwingApplication getCySwingApplication() {
        if (cyApplication == null) {
            ServiceReference ref = context.getServiceReference(CySwingApplication.class.getName());
            if (ref == null)
                return null;
            cyApplication = (CySwingApplication) context.getService(ref);
            if (cyApplication != null)
                serviceReferences.add(ref);
        }
        return cyApplication;
    }
    
    /**
     * Get the JFrame used to hold the whole application.
     * @return
     */
    public JFrame getCytoscapeDesktop() {
        return getCySwingApplication().getJFrame();
    }
    
    /**
     * Get the preset properties.
     * @return
     */
    public Properties getProperties() {
        if (properties == null)
        {
            try
            {
                properties = new Properties();
                InputStream is = getClass().getResourceAsStream("Config.prop");
                properties.load(is);
            }
            catch (IOException e)
            {
                logger.error("Cannot find Config.prop: "
                        + e.getMessage(), e);
            }
        }
        return this.properties;
    }
    
    /**
     * Create an ImageIcon using images in this package (org.reactome.cytoscape.util).
     * @param imgFileName
     * @return
     */
    public ImageIcon createImageIcon(String imgFileName) {
        String urlName = "org/reactome/cytoscape/util/" + imgFileName;
        URL url = getClass().getClassLoader().getResource(urlName);
        ImageIcon icon = null;
        if (url == null) {
            icon = new ImageIcon(imgFileName);
        }
        else {
            icon = new ImageIcon(url);
        }
        return icon;
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

    public String getRestfulURL() {
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
    
}
