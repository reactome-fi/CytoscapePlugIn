/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.util;

import java.awt.Component;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.events.SetCurrentNetworkViewEvent;
import org.cytoscape.application.events.SetCurrentNetworkViewListener;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
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
    // This is used to keep the mapping between a Service object and its Registration
    // so that they can be turned on/off
    private Map<Object, ServiceRegistration> serviceToRegistration;
    // Keep this registration in order to turn off
    private CyNetworkViewContextMenuFactory fiAnnotMenu;
    private CyNetworkViewContextMenuFactory convertToNetworkMenu;
    
    /**
     * Default constructor. This is a private constructor so that the single instance should be used.
     */
    private PlugInObjectManager() {
        serviceReferences = new ArrayList<ServiceReference>();
        serviceToRegistration = new HashMap<Object, ServiceRegistration>();
    }
    
    public static PlugInObjectManager getManager() {
        if (manager == null)
            manager = new PlugInObjectManager();
        return manager;
    }
    
    public void setConvertToNetworkMenu(CyNetworkViewContextMenuFactory menu) {
        this.convertToNetworkMenu = menu;
    }

    public void setFiAnnotMenu(CyNetworkViewContextMenuFactory fiAnnotMenu) {
        this.fiAnnotMenu = fiAnnotMenu;
    }
    
    /**
     * Check if Reactome pathways are loaded.
     * @return
     */
    public boolean isPathwaysLoaded() {
        CySwingApplication app = getCySwingApplication();
        CytoPanel westPane = app.getCytoPanel(CytoPanelName.WEST);
        for (int i = 0; i < westPane.getCytoPanelComponentCount(); i++) {
            Component comp = westPane.getComponentAt(i);
            if (comp instanceof CytoPanelComponent) {
                String title = ((CytoPanelComponent)comp).getTitle();
                if (title.equals("Reactome"))
                    return true;
            }
        }
        return false;
    }
    
    /**
     * Install a "Fetch FI Annotations" menu
     */
    private void installContextMenu(CyNetworkViewContextMenuFactory menu,
                                    String title) {
        if (menu == null)
            return;
        ServiceRegistration registration = serviceToRegistration.get(menu);
        if (registration != null)
            return; // It has been registered already
        Properties fiFetcherProps = new Properties();
        fiFetcherProps.setProperty("title", title);
        fiFetcherProps.setProperty("preferredMenu", "Apps.Reactome FI");
        // Want to keep the registration of this menu in order to turn it off
        registration = context.registerService(CyNetworkViewContextMenuFactory.class.getName(), 
                                               menu, 
                                               fiFetcherProps);
        serviceToRegistration.put(menu, registration);
    }
    
    private void uninstallContextMenu(CyNetworkViewContextMenuFactory menu) {
        if (menu == null)
            return;
        ServiceRegistration registration = serviceToRegistration.get(menu);
        if (registration == null)
            return; // It has unregistered already
        registration.unregister();
        serviceToRegistration.remove(menu);
    }

    public String getFiNetworkVersion()
    {
        if (this.fiNetworkVersion != null)
            return this.fiNetworkVersion;
        else
            return getDefaultFINeworkVersion();
    }

    /**
     * Get the default version of the FI network if it is set. Otherwise,
     * the first one listed will be returned.
     * @return
     */
    public String getDefaultFINeworkVersion() {
        Properties prop = PlugInObjectManager.getManager().getProperties();
        String fiVersions = prop.getProperty("FINetworkVersions");
        String[] tokens = fiVersions.split(",");
        for (String token : tokens)
        {
            token = token.trim();
            if (token.toLowerCase().contains("default")) return token;
        }
        // There is no default set. Choose the first one.
        return tokens[0];
    }
    
    /**
     * Get the latest version of the FI network listed in the configuration.
     * @return
     */
    public String getLatestFINetworkVersion() {
        Properties prop = PlugInObjectManager.getManager().getProperties();
        String fiVersions = prop.getProperty("FINetworkVersions");
        String[] tokens = fiVersions.split(",");
        Map<Integer, String> yearToVersion = new HashMap<Integer, String>();
        for (String token : tokens) {
            token = token.trim();
            int index = token.indexOf("(");
            if (index > 0)
                yearToVersion.put(new Integer(token.substring(0, index).trim()),
                                  token);
            else
                yearToVersion.put(new Integer(token), token);
        }
        List<Integer> years = new ArrayList<Integer>(yearToVersion.keySet());
        Collections.sort(years);
        return yearToVersion.get(years.get(years.size() - 1));
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
                    uninstallContextMenu(PlugInObjectManager.this.fiAnnotMenu);
                    installContextMenu(PlugInObjectManager.this.convertToNetworkMenu,
                                       "Convert to Diagram");
                }
                else {
                    installContextMenu(PlugInObjectManager.this.fiAnnotMenu,
                                       "Fetch FI Annotations");
                    uninstallContextMenu(PlugInObjectManager.this.convertToNetworkMenu);
                }
            }
        };
        context.registerService(SetCurrentNetworkViewListener.class.getName(),
                                currentNetworkViewListener,
                                null);
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
