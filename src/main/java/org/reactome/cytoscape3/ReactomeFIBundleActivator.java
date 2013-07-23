package org.reactome.cytoscape3;

/**
 * This is the main entry point for
 * the Reactome FI app. In OSGi parlance,
 * it is a bundle activator. For more info on
 * OSGi, check out Richard Hall's "OSGi in Action"
 * and the OSGi R4 specs.
 * @author Eric T Dawson, July 2013
 */
import java.util.Properties;

import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.pathway.PathwayLoadAction;
import org.reactome.cytoscape3.NetworkActionCollection.ClusterFINetworkMenu;
import org.reactome.cytoscape3.NodeActionCollection.GeneCardMenu;

//import org.cytoscape.application.CyApplicationManager;

public class ReactomeFIBundleActivator extends AbstractCyActivator
{

    public BundleContext context;

    public ReactomeFIBundleActivator()
    {
        super();
    }

    @Override
    public void start(BundleContext context) throws Exception
    {
        this.context = context;
        PlugInScopeObjectManager.getManager().setBundleContext(context);
        /* Grab essential Cytoscape Service References */
        CySwingApplication desktopApp = getService(context,
                CySwingApplication.class);
        TaskManager taskManager = getService(context, TaskManager.class);
        CyNetworkManager networkManager = getService(context,
                CyNetworkManager.class);
        CySessionManager sessionManager = getService(context,
                CySessionManager.class);
        CyNetworkFactory networkFactory = getService(context,
                CyNetworkFactory.class);
        CyNetworkViewFactory viewFactory = getService(context,
                CyNetworkViewFactory.class);
        CyNetworkViewManager viewManager = getService(context,
                CyNetworkViewManager.class);
        CyTableFactory tableFactory = getService(context, CyTableFactory.class);
        SaveSessionAsTaskFactory saveSessionAsTaskFactory = getService(context,
                SaveSessionAsTaskFactory.class);
        FileUtil fileUtil = getService(context, FileUtil.class);
        OpenBrowser browser = getService(context, OpenBrowser.class);
        CyLayoutAlgorithmManager layoutManager = getService(context,
                CyLayoutAlgorithmManager.class);
        VisualMappingManager visMapManager = getService(context,
                VisualMappingManager.class);
        VisualStyleFactory visStyleFactory = getService(context,
                VisualStyleFactory.class);
        VisualMappingFunctionFactory vmfFactoryC = getService(context,
                VisualMappingFunctionFactory.class, "(mapping.type=continuous)");
        VisualMappingFunctionFactory vmfFactoryD = getService(context,
                VisualMappingFunctionFactory.class, "(mapping.type=discrete)");
        VisualMappingFunctionFactory vmfFactoryP = getService(context,
                VisualMappingFunctionFactory.class,
                "(mapping.type=passthrough)");

        FIVisualStyleImpl styleHelper = new FIVisualStyleImpl(
                visMapManager, visStyleFactory, vmfFactoryC,
                vmfFactoryD, vmfFactoryP, layoutManager,
                taskManager, desktopApp);
        Properties visStyleHelperProps = new Properties();
        visStyleHelperProps.setProperty("title", "FIVisualStyleImpl");
        registerAllServices(context, styleHelper, visStyleHelperProps);
        //Instantiate Reactome FI App services
        GeneSetMutationAnalysisAction gsma = new GeneSetMutationAnalysisAction(
                taskManager, networkManager, saveSessionAsTaskFactory,
                fileUtil, desktopApp, sessionManager, networkFactory,
                viewFactory, viewManager,tableFactory);

        UserGuideAction uga = new UserGuideAction();
        
        // Test code for loading pathway diagram into Cytoscape
        PathwayLoadAction pathwayLoadAction = new PathwayLoadAction();
        pathwayLoadAction.setBundleContext(context);
        String reactomeRestfulUrl = PlugInScopeObjectManager.getManager().getProperties().getProperty("ReactomeRESTfulAPI");
        pathwayLoadAction.setReactomeRestfulURL(reactomeRestfulUrl);
        
        // Register said Reactome FI Services with the OSGi framework.
        registerAllServices(context, gsma, new Properties());
        registerAllServices(context, pathwayLoadAction, new Properties());
        registerAllServices(context, uga, new Properties());

        // Instantiate and register the context menus for the network view
        NetworkActionCollection networkMenu = new NetworkActionCollection();
        ClusterFINetworkMenu clusterMenu = networkMenu.new ClusterFINetworkMenu();
        Properties clusterProps = new Properties();
        clusterProps.setProperty("title", "Cluster FI Network");
        clusterProps.setProperty("preferredMenu", "Apps.Reactome FI");
        registerService(context, clusterMenu,
                CyNetworkViewContextMenuFactory.class, clusterProps);
        // Instantiate and register the context menus for the node views
        NodeActionCollection nodeActionCollection = new NodeActionCollection();
        GeneCardMenu geneCardMenu = nodeActionCollection.new GeneCardMenu();
        Properties geneCardProps = new Properties();
        geneCardProps.setProperty("title", "Gene Card");
        geneCardProps.setProperty("preferredMenu", "Apps.Reactome FI");
        registerService(context, geneCardMenu,
                CyNodeViewContextMenuFactory.class, geneCardProps);

    }

}
