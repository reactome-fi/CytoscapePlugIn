package org.reactome.cytoscape;

import static org.reactome.cytoscape.service.ReactomeNetworkType.SingleCellClusterNetwork;
import static org.reactome.cytoscape.service.ReactomeNetworkType.SingleCellNetwork;

import java.util.Properties;

import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.task.edit.MapTableToNetworkTablesTaskFactory;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.fipgm.PGMImpactAnalysisAction;
import org.reactome.cytoscape.fipgm.PGMImpactAnalysisResultLoadAction;
import org.reactome.cytoscape.pathway.FactorGraphPopupMenuHandler;
import org.reactome.cytoscape.pathway.ReactomePathwayAction;
import org.reactome.cytoscape.rest.ReactomeFIVizResource;
import org.reactome.cytoscape.rest.ReactomeFIVizResourceImp;
import org.reactome.cytoscape.sc.SingleCellAnalysisAction;
import org.reactome.cytoscape.sc.SingleCellLoadAction;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.FIVisualStyleImpl;
import org.reactome.cytoscape.service.PopupMenuManager;
import org.reactome.cytoscape.service.ReactomeFIVizPropsReader;
import org.reactome.cytoscape.service.ReactomeNetworkType;
import org.reactome.cytoscape.service.TableFormatterImpl;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.cytoscape3.FINetworkPopupMenuHandler;
import org.reactome.cytoscape3.FactorGraphImportAction;
import org.reactome.cytoscape3.GeneSetFINetworkPopupMenuHandler;
import org.reactome.cytoscape3.GeneSetMutationAnalysisAction;
import org.reactome.cytoscape3.MechismoFINetworkPopupMenuHandler;
import org.reactome.cytoscape3.MicroarrayAnalysisAction;
import org.reactome.cytoscape3.PGMFINetworkPopupMenuHandler;
import org.reactome.cytoscape3.PathwayFINetworkPopupMenuHandler;
import org.reactome.cytoscape3.ReactionNetworkPopupMenuHandler;
import org.reactome.cytoscape3.ScNetworkPopupMenuHandler;
import org.reactome.cytoscape3.UserGuideAction;

public class ReactomeFIBundleActivator extends AbstractCyActivator {

    public ReactomeFIBundleActivator() {
        super();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        PlugInObjectManager.getManager().setBundleContext(context);
        
        // Register properties: Make sure ReactomeFIViz.props is in the resources folder in the bundled app where resources is .in the top
        // Note: the actual file name used for the property doesn't have resource here.
        ReactomeFIVizPropsReader propReader = new ReactomeFIVizPropsReader(PlugInUtilities.APP_NAME,
                                                                           PlugInUtilities.APP_NAME + ".props");
        Properties props = new Properties();
        props.setProperty("cyPropertyName", 
                          PlugInUtilities.APP_NAME + ".props");
        registerAllServices(context, propReader, props);
        PlugInObjectManager.getManager().setCustomizedProps(propReader.getProperties());
        
        PlugInObjectManager.getManager().setServiceRegistra(getService(context, CyServiceRegistrar.class));

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
        CyTableFactory tableFactory = getService(context, CyTableFactory.class);
        CyTableManager tableManager = getService(context, CyTableManager.class);
        CyNetworkTableManager networkTableManager = getService(context, CyNetworkTableManager.class);
        TableFormatterImpl tableFormatter = new TableFormatterImpl(tableFactory, 
                                                                   tableManager,
                                                                   networkTableManager,
                                                                   mapNetworkAttrTFServiceRef);
        Properties tableFormatterProps = new Properties();
        tableFormatterProps.setProperty("title", "TableFormatterImpl");
        registerAllServices(context, tableFormatter, tableFormatterProps);
        
        //Instantiate Reactome FI App services
        CySwingApplication desktopApp = getService(context,
                CySwingApplication.class);
        GeneSetMutationAnalysisAction gsma = new GeneSetMutationAnalysisAction(desktopApp);
        
        // Use for debug
        String pgmDebug = PlugInObjectManager.getManager().getProperties().getProperty("PGMDebug"); 
        if (pgmDebug != null && pgmDebug.equals("true")) {
            FactorGraphImportAction fgImport = new FactorGraphImportAction();
            registerAllServices(context, fgImport, new Properties());
        }

        MicroarrayAnalysisAction maa = new MicroarrayAnalysisAction(desktopApp);
        UserGuideAction uga = new UserGuideAction();
//        HotNetAnalysisAction hna = new HotNetAnalysisAction(desktopApp);
        // Load pathway diagram into Cytoscape
        ReactomePathwayAction pathwayLoadAction = new ReactomePathwayAction();
        // This is test code
        SingleCellAnalysisAction scAction = new SingleCellAnalysisAction();
        SingleCellLoadAction scLoadAction = new SingleCellLoadAction();
        // Perform impact analysis based on PGM
        PGMImpactAnalysisAction pgmImpactAction = new PGMImpactAnalysisAction();
        PGMImpactAnalysisResultLoadAction pgmLoadAction = new PGMImpactAnalysisResultLoadAction();
        
        // Register said Reactome FI Services with the OSGi framework.
        // An empty property
        Properties prop = new Properties();
        registerAllServices(context, gsma, prop);
        registerAllServices(context, pgmImpactAction, prop);
        registerAllServices(context, pgmLoadAction, prop);
        // As of June 19, 2020, HotNet Mutation Analysis is retired.
//        registerAllServices(context, hna, prop);
        registerAllServices(context, scAction, prop);
        registerAllServices(context, scLoadAction, prop);
        registerAllServices(context, maa, prop);
        registerAllServices(context, pathwayLoadAction, prop);
        registerAllServices(context, uga, prop);
        
        // Register REST functions for Cytoscape automation
        ReactomeFIVizResource resource = new ReactomeFIVizResourceImp();
        registerService(context, resource, ReactomeFIVizResource.class, prop);

        PopupMenuManager popupManager = PopupMenuManager.getManager();
        popupManager.registerMenuHandler(ReactomeNetworkType.FINetwork,
                                         new GeneSetFINetworkPopupMenuHandler());
        popupManager.registerMenuHandler(ReactomeNetworkType.PathwayFINetwork,
                                         new PathwayFINetworkPopupMenuHandler());
        popupManager.registerMenuHandler(ReactomeNetworkType.FactorGraph,
                                         new FactorGraphPopupMenuHandler());
        popupManager.registerMenuHandler(ReactomeNetworkType.PGMFINetwork,
                                         new PGMFINetworkPopupMenuHandler());
        popupManager.registerMenuHandler(ReactomeNetworkType.ReactionNetwork,
                                         new ReactionNetworkPopupMenuHandler());
        popupManager.registerMenuHandler(ReactomeNetworkType.MechismoNetwork,
                                         new MechismoFINetworkPopupMenuHandler());
        ScNetworkPopupMenuHandler scPopupHandler = new ScNetworkPopupMenuHandler();
        popupManager.registerMenuHandler(SingleCellNetwork, scPopupHandler);
        popupManager.registerMenuHandler(SingleCellClusterNetwork, scPopupHandler);
        popupManager.registerMenuHandler(ReactomeNetworkType.DorotheaTFTargetNetwork,
                                         new FINetworkPopupMenuHandler()); // Don't have fetch annotations. 
                                                                           // This should be handled when the network is constructed.
        
        // Used as the default PopupMenuHandler. Most likely, this is not needed.
        // But keep it here for the time being.
        popupManager.installPopupMenu(ReactomeNetworkType.FINetwork);
    }

}
