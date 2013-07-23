/*
 * Created on Jul 23, 2013
 *
 */
package org.reactome.cytoscape.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.osgi.framework.BundleContext;
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
    // System-wide proeprties
    private Properties properties;
    private static PlugInObjectManager manager;
    
    /**
     * Default constructor. This is a private constructor so that the single instance should be used.
     */
    private PlugInObjectManager() {
    }
    
    public static PlugInObjectManager getManager() {
        if (manager == null)
            manager = new PlugInObjectManager();
        return manager;
    }
    
    public void setBundleContext(BundleContext context) {
        this.context = context;
    }
    
    public BundleContext getBundleContext() {
        return this.context;
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
    
}
