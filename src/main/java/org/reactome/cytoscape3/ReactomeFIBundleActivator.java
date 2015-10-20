package org.reactome.cytoscape3;

/**
 * This is the main entry point for
 * the Reactome FI app. In OSGi parlance,
 * it is a bundle activator. For more info on
 * OSGi, check out Richard Hall's "OSGi in Action"
 * and the OSGi R4 specs.
 * @author Eric T Dawson & Guanming Wu
 */
import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.edit.MapTableToNetworkTablesTaskFactory;
import org.cytoscape.task.write.SaveSessionAsTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.fipgm.PGMImpactAnalysisAction;
import org.reactome.cytoscape.fipgm.PGMImpactAnalysisResultLoadAction;
import org.reactome.cytoscape.pathway.FactorGraphPopupMenuHandler;
import org.reactome.cytoscape.pathway.ReactomePathwayAction;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.FIVisualStyleImpl;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.service.ReactomeNetworkType;
import org.reactome.cytoscape.service.TableFormatterImpl;
import org.reactome.cytoscape.util.PlugInObjectManager;

public class ReactomeFIBundleActivator extends AbstractCyActivator {

    public ReactomeFIBundleActivator() {
        super();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        PlugInObjectManager.getManager().setBundleContext(context);
        
        /* Grab essential Cytoscape Service References */
        CySwingApplication desktopApp = getService(context,
                CySwingApplication.class);
        @SuppressWarnings("rawtypes")
        TaskManager taskManager = getService(context, TaskManager.class);
        CyNetworkManager networkManager = getService(context,
                CyNetworkManager.class);
        CySessionManager sessionManager = getService(context,
                CySessionManager.class);
        CyNetworkFactory networkFactory = getService(context,
                CyNetworkFactory.class);
        CyNetworkTableManager networkTableManager = getService(context, CyNetworkTableManager.class);
        CyNetworkViewFactory viewFactory = getService(context,
                CyNetworkViewFactory.class);
        CyNetworkViewManager viewManager = getService(context,
                CyNetworkViewManager.class);
        CyTableFactory tableFactory = getService(context, CyTableFactory.class);
        CyTableManager tableManager = getService(context, CyTableManager.class);
        SaveSessionAsTaskFactory saveSessionAsTaskFactory = getService(context,
                SaveSessionAsTaskFactory.class);
        FileUtil fileUtil = getService(context, FileUtil.class);
        CyLayoutAlgorithmManager layoutManager = getService(context,
                CyLayoutAlgorithmManager.class);
        
        // Register FI network visualization mapping as OSGi services
        //Initialize and register the FI VIsual Style with the framework,
        //allowing it to be used by all Reactome FI classes.
        FIVisualStyle styleHelper = new FIVisualStyleImpl();
        Properties visStyleHelperProps = new Properties();
        visStyleHelperProps.setProperty("title", "FIVisualStyleImpl");
        registerAllServices(context, styleHelper, visStyleHelperProps);
        
        //Initialize and register the TableFormatter with the network
        //so that it is accessible across the app.
        MapTableToNetworkTablesTaskFactory mapNetworkAttrTFServiceRef = getService(context,
                                                                                   MapTableToNetworkTablesTaskFactory.class);
        TableFormatterImpl tableFormatter = new TableFormatterImpl(tableFactory, 
                                                                   tableManager,
                                                                   networkTableManager,
                                                                   mapNetworkAttrTFServiceRef);
        Properties tableFormatterProps = new Properties();
        tableFormatterProps.setProperty("title", "TableFormatterImpl");
        registerAllServices(context, tableFormatter, tableFormatterProps);
        
        //Instantiate Reactome FI App services
        GeneSetMutationAnalysisAction gsma = new GeneSetMutationAnalysisAction(desktopApp);

        MicroarrayAnalysisAction maa = new MicroarrayAnalysisAction(desktopApp);
        UserGuideAction uga = new UserGuideAction();
        HotNetAnalysisAction hna = new HotNetAnalysisAction(desktopApp);
        // Load pathway diagram into Cytoscape
        ReactomePathwayAction pathwayLoadAction = new ReactomePathwayAction();
        // Perform impact analysis based on PGM
        PGMImpactAnalysisAction pgmImpactAction = new PGMImpactAnalysisAction();
        PGMImpactAnalysisResultLoadAction pgmLoadAction = new PGMImpactAnalysisResultLoadAction();
        
        // Register said Reactome FI Services with the OSGi framework.
        // An empty property
        Properties prop = new Properties();
        registerAllServices(context, gsma, prop);
        registerAllServices(context, pgmImpactAction, prop);
        registerAllServices(context, pgmLoadAction, prop);
        registerAllServices(context, hna, prop);
        registerAllServices(context, maa, prop);
        registerAllServices(context, pathwayLoadAction, prop);
        registerAllServices(context, uga, prop);

        PopupMenuManager popupManager = PopupMenuManager.getManager();
        popupManager.registerMenuHandler(ReactomeNetworkType.FINetwork,
                                         new GeneSetFINetworkPopupMenuHandler());
        popupManager.registerMenuHandler(ReactomeNetworkType.PathwayFINetwork,
                                         new PathwayFINetworkPopupMenuHandler());
        popupManager.registerMenuHandler(ReactomeNetworkType.FactorGraph,
                                         new FactorGraphPopupMenuHandler());
        popupManager.registerMenuHandler(ReactomeNetworkType.PGMFINetwork,
                                         new PGMFINetworkPopupMenuHandler());
        
        // Used as the default PopupMenuHandler. Most likely, this is not needed.
        // But keep it here for the time being.
        popupManager.installPopupMenu(ReactomeNetworkType.FINetwork);
    }

}
