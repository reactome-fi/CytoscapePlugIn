/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.util;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.cytoscape.app.CyAppAdapter;
import org.cytoscape.application.CyVersion;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskManager;
import org.gk.graphEditor.SelectionMediator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.SynchronousBundleListener;
import org.reactome.fi.pgm.FIPGMConfiguration;
import org.reactome.pathway.factorgraph.PathwayPGMConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class in the utility packgage is used to manage some plug-in scope objects that are used across
 * the whole Reactome FI plug-in for all features. Some of features in this class are refactored from the 
 * original PlugInScopeObjectManager. This is a singleton as with the original PlugInScopeObjectManager.
 * @author gwu
 *
 */
@SuppressWarnings("rawtypes")
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
    // Cache to fetch the version of Cytoscape
    private CyVersion cyVersion;
    // Cache TaskManager since it will be used multiple times
    private TaskManager taskManager;
    // Sometimes we may need to use synchronized version of TaskManager
    private SynchronousTaskManager syncTaskManager;
    // Currently selected FI network version
    private String fiNetworkVersion;
    // Used for setting colors for pathway diagram highlighting
    private double[] minMaxColorValues = new double[]{-1.0, 1.0};
    // These two configurations are used for PGM-based analyses
    private PathwayPGMConfiguration pathwayPGMConfig;
    private FIPGMConfiguration fiPGMConfig;
    // To control radio button
    private Map<String, ButtonGroup> btnNameToGroup;
    // To synchronize selection of Reactome objects based on DB_IDs.
    private SelectionMediator dbIdSelectionMediator;
    // To syncrhonize observation variable selection
    private SelectionMediator observationSelectionMediator;
    // Cached JDesktop for doing something for pathway view
    private JDesktopPane pathwayDesktop;
    // Used as a label for card layout for pathwaydesktop in Cytoscape 3.4.0 or above
    private final String PATHWAY_DIAGRAM_CARD_LABEL = "PathwayDiagramDesktop";
    // For connecting some remote links
    private PropertyChangeSupport propertyChangeSupport;
    
    /**
     * Default constructor. This is a private constructor so that the single instance should be used.
     */
    private PlugInObjectManager() {
        serviceReferences = new ArrayList<ServiceReference>();
        propertyChangeSupport = new PropertyChangeSupport(this);
    }
    
    public static PlugInObjectManager getManager() {
        if (manager == null)
            manager = new PlugInObjectManager();
        return manager;
    }
    
    public void addPropetyChangeListener(PropertyChangeListener l) {
        propertyChangeSupport.addPropertyChangeListener(l);
    }
    
    public void removePropertyChangeLisener(PropertyChangeListener l) {
        propertyChangeSupport.removePropertyChangeListener(l);
    }
    
    public void firePropertyChangeEvent(PropertyChangeEvent event) {
        propertyChangeSupport.firePropertyChange(event);
    }
    
    public PathwayPGMConfiguration getPathwayPGMConfig() {
        if (pathwayPGMConfig == null) {
            pathwayPGMConfig = PathwayPGMConfiguration.getConfig();
            // Need to use resources to load it
            try {
                pathwayPGMConfig.config(getClass().getResourceAsStream("PGM_Pathway_Config.xml"));
            }
            catch(Exception e) {
                throw new IllegalStateException("Cannot configure PathwayPGMConfiguration: " + e);
            }
        }
        return pathwayPGMConfig;
    }
    
    public SelectionMediator getObservationVarSelectionMediator() {
        if (observationSelectionMediator == null)
            observationSelectionMediator = new SelectionMediator();
        return observationSelectionMediator;
    }
    
    public SelectionMediator getDBIdSelectionMediator() {
        if (dbIdSelectionMediator == null)
            dbIdSelectionMediator = new SelectionMediator();
        return dbIdSelectionMediator;
    }

    public void registerRadioButton(String groupName,
                                    JRadioButton button) {
        if (btnNameToGroup == null)
            btnNameToGroup = new HashMap<>();
        ButtonGroup group = btnNameToGroup.get(groupName);
        if (group == null) {
            group = new ButtonGroup();
            btnNameToGroup.put(groupName, group);
        }
        group.add(button);
    }
    
    public void unregisterRadioButton(String groupName,
                                      JRadioButton button) {
        if (btnNameToGroup == null)
            return;
        ButtonGroup group = btnNameToGroup.get(groupName);
        if (group == null)
            return;
        group.remove(button);
    }
    
    public FIPGMConfiguration getFIPGMConfig() {
        if (fiPGMConfig == null) {
            fiPGMConfig = FIPGMConfiguration.getConfig();
         // Need to use resources to load it
            try {
                fiPGMConfig.config(getClass().getResourceAsStream("PGM_FI_Config.xml"));
            }
            catch(Exception e) {
                throw new IllegalStateException("Cannot configure FIPGMConfiguration: " + e);
            }
        }
        return fiPGMConfig;
    }
    
    public double[] getMinMaxColorValues() {
        return minMaxColorValues;
    }

    public void setMinMaxColorValues(double[] minMaxColorValues) {
        this.minMaxColorValues = minMaxColorValues;
    }
    
    /**
     * For Cytoscape 3.4.0 or above, a customized JDesktopPane is used for showing
     * pathway diagrams. This JDesktopPane is held in a Cardlayout's panel (aka container
     * in NetworkViewMainPanel). Using this method to force this desktop to be displayed.
     */
    public void showPathwayDesktop() {
        if (pathwayDesktop == null)
            return; // Don't do anything
        CyVersion version = getCyVersion();
        if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 4) {
            Container container = pathwayDesktop.getParent();
            if (container.getLayout() instanceof CardLayout) {
                PlugInUtilities.unselectNetwork();
                CardLayout layout = (CardLayout) container.getLayout();
                layout.show(container, PATHWAY_DIAGRAM_CARD_LABEL);
            }
        }
    }
    
    /**
     * Get a JDesktopPane for holding pathway diagrams.
     * @return
     */
    public JDesktopPane getPathwayDesktop() {
        if (pathwayDesktop != null)
            return pathwayDesktop;
        CyVersion version = getCyVersion();
        if (version.getMajorVersion() >= 3 && version.getMinorVersion() >= 4) {
            pathwayDesktop = createPathwayDesktop();
        }
        else {
            pathwayDesktop = searchDesktopPane();
        }
        return pathwayDesktop;
    }
    
    public void removePathwayDesktop() {
        if (pathwayDesktop == null)
            return;
        Container container = pathwayDesktop.getParent();
        if (container == null)
            return;
        container.remove(pathwayDesktop);
        LayoutManager layout = container.getLayout();
        layout.removeLayoutComponent(pathwayDesktop);
        pathwayDesktop = null; // Have to null it to show network views as of Cytoscape 3.6.0.
    }
    
    /**
     * Use a JFrame to hold a JDesktopPane.
     * @return
     */
    private JDesktopPane createPathwayDesktop() {
        // Since Cytoscape version 3.4.0, JDesktopPane is not used anymore.
        // We want to find a JPanel with CardLayout, which is used to display
        // networks and grids of networks. JDesktopPane will be added as a card
        JPanel container = searchPanelWithCardLayout();
        if (container == null)
            return null;
        JDesktopPane desktop = new JDesktopPane();
        container.add(desktop, PATHWAY_DIAGRAM_CARD_LABEL);
        CardLayout layout = (CardLayout) container.getLayout();
        layout.show(container, PATHWAY_DIAGRAM_CARD_LABEL);
        return desktop;
    }
    
    private JPanel searchPanelWithCardLayout() {
        JFrame frame = getCytoscapeDesktop();
        // Use this loop to find JDesktopPane
        Set<Component> children = new HashSet<Component>();
        for (Component comp : frame.getComponents())
            children.add(comp);
        Set<Component> next = new HashSet<Component>();
        while (children.size() > 0) {
            for (Component comp : children) {
                if (comp instanceof JPanel) {
                    JPanel pane = (JPanel) comp;
                    if (pane.getLayout() instanceof CardLayout) {
                        return pane;
                    }
                }
                if (comp instanceof Container) {
                    Container container = (Container) comp;
                    if (container.getComponentCount() > 0) {
                        for (Component comp1 : container.getComponents())
                            next.add(comp1);
                    }
                }
            }
            children.clear();
            children.addAll(next);
            next.clear();
        }
        return null;
    }
    
    private JDesktopPane searchDesktopPane() {
        JFrame frame = getCytoscapeDesktop();
        // Use this loop to find JDesktopPane
        Set<Component> children = new HashSet<Component>();
        for (Component comp : frame.getComponents())
            children.add(comp);
        Set<Component> next = new HashSet<Component>();
        while (children.size() > 0) {
            for (Component comp : children) {
                if (comp instanceof JDesktopPane) {
                    return (JDesktopPane) comp;
                }
                if (comp instanceof Container) {
                    Container container = (Container) comp;
                    if (container.getComponentCount() > 0) {
                        for (Component comp1 : container.getComponents())
                            next.add(comp1);
                    }
                }
            }
            children.clear();
            children.addAll(next);
            next.clear();
        }
        return null;
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
     * Check if cancer targetome features are enabeld.
     * @return
     */
    public boolean isCancerTargetEnabled() {
        String enableCancerTargetome = getManager().getProperties().getProperty("enableCancerTargetome", "false");
        return enableCancerTargetome.equals("true");
    }
    
    public boolean isMechismoEnabled() {
        String enabled = getManager().getProperties().getProperty("enableMechismo", "false");
        return enabled.equals("true");
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
    
    public CyVersion getCyVersion() {
        if (cyVersion == null) {
            ServiceReference reference = context.getServiceReference(CyVersion.class.getName());
            if (reference == null)
                return null;
            cyVersion = (CyVersion) context.getService(reference);
            if (cyVersion != null)
                serviceReferences.add(reference);
        }
        return cyVersion;
    }
    
    /**
     * Get the system-wide TaskManager that is registered as a service
     */
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
    
    public SynchronousTaskManager getSyncTaskManager() {
        if (syncTaskManager != null)
            return syncTaskManager;
        ServiceReference ref = context.getServiceReference(SynchronousTaskManager.class.getName());
        if (ref == null)
            return null;
        syncTaskManager = (SynchronousTaskManager) context.getService(ref);
        if (syncTaskManager != null)
            serviceReferences.add(ref);
        return syncTaskManager;
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
    
    public void selectCytoPane(Component panel, CytoPanelName direction) {
        CySwingApplication desktopApp = getCySwingApplication();
        CytoPanel tableBrowserPane = desktopApp.getCytoPanel(direction);
        int index = tableBrowserPane.indexOfComponent(panel);
        if (index >= 0)
            tableBrowserPane.setSelectedIndex(index);
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
    
    public void setProperties(Properties prop) {
        this.properties = prop;
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
    
    public String getReactomeRESTfulURL() {
        Properties prop = getProperties();
        String url = prop.getProperty("ReactomeRESTfulAPI");
        return url;
    }
    
    /**
     * Get the RESTful URL
     * 
     * @param fiVersion
     * @return
     */
    public String getRestfulURL(String fiVersion) {
        fiVersion = fiVersion.replaceAll(" ", "_");
        String key = fiVersion + "_restfulURL";
        Properties prop = getProperties();
        String url = prop.getProperty(key);
        return url;
    }

    public String getRestfulURL() {
        return getRestfulURL(getFiNetworkVersion());
    }
    
    public String getHostURL() {
        String serviceUrl = getRestfulURL();
     // Get the host URL name
        int index = serviceUrl.lastIndexOf("/", serviceUrl.length() - 2);
        return serviceUrl.substring(0, index + 1);
    }
    
}
