/*
 * Created on Sep 19, 2013
 *
 */
package org.reactome.cytoscape3;

import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.TableFormatter;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * An abstract class that group common features for all tasks related to FI plug-ins.
 * @author gwu
 *
 */
public abstract class FIAnalysisTask implements Runnable {
    // Some serviceReference objects for easy access later on
    protected ServiceReference tableFormatterServRef;
    protected ServiceReference netManagerRef;
    protected ServiceReference viewFactoryRef;
    protected ServiceReference viewManagerRef;
    
    public FIAnalysisTask() {
    }

    @Override
    public void run() {
        performTask();
    }
    
    private void performTask() {
        initializeCyServices();
        doAnalysis();
        releaseCyServices();
    }
    
    protected abstract void doAnalysis();

    protected void initializeCyServices() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();        
        this.netManagerRef = context.getServiceReference(CyNetworkManager.class.getName());
        this.viewFactoryRef = context.getServiceReference(CyNetworkViewFactory.class.getName());
        this.viewManagerRef = context.getServiceReference(CyNetworkViewManager.class.getName());
        this.tableFormatterServRef = context.getServiceReference(TableFormatter.class.getName());
    }

    protected void releaseCyServices() {
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.ungetService(netManagerRef);
        context.ungetService(viewFactoryRef);
        context.ungetService(viewManagerRef);
        context.ungetService(tableFormatterServRef);
    }
    
}
