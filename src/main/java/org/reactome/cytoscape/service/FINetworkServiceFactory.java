/*
 * Created on Jul 23, 2016
 *
 */
package org.reactome.cytoscape.service;

import java.util.Properties;

import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * A helper method to fetch for FINetworkService preconfigured for the app.
 * @author gwu
 *
 */
public class FINetworkServiceFactory {
    
    /**
     * Default constructor.
     */
    public FINetworkServiceFactory() {
    }
    
    /**
     * Call this method to get a pre-configured FINetworkService.
     * @return
     */
    public FINetworkService getFINetworkService() throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Properties prop = PlugInObjectManager.getManager().getProperties();
        String clsName = prop.getProperty("networkService",
                "org.reactome.cytoscape.service.LocalService");
        FINetworkService networkService = (FINetworkService) Class.forName(clsName).newInstance();
        return networkService;
    }
    
}
