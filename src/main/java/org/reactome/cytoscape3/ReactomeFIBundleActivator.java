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

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
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
import org.cytoscape.view.model.events.NetworkViewDestroyedListener;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.TaskManager;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape3.EdgeActionCollection.EdgeQueryFIMenuItem;
import org.reactome.cytoscape3.NetworkActionCollection.ClusterFINetworkMenu;
import org.reactome.cytoscape3.NetworkActionCollection.FIAnnotationFetcherMenu;
import org.reactome.cytoscape3.NetworkActionCollection.LoadCancerGeneIndexForNetwork;
import org.reactome.cytoscape3.NetworkActionCollection.ModuleGOBioProcessMenu;
import org.reactome.cytoscape3.NetworkActionCollection.ModuleGOCellComponentMenu;
import org.reactome.cytoscape3.NetworkActionCollection.ModuleGOMolecularFunctionMenu;
import org.reactome.cytoscape3.NetworkActionCollection.ModulePathwayEnrichmentMenu;
import org.reactome.cytoscape3.NetworkActionCollection.NetworkGOBioProcessMenu;
import org.reactome.cytoscape3.NetworkActionCollection.NetworkGOCellComponentMenu;
import org.reactome.cytoscape3.NetworkActionCollection.NetworkGOMolecularFunctionMenu;
import org.reactome.cytoscape3.NetworkActionCollection.NetworkPathwayEnrichmentMenu;
import org.reactome.cytoscape3.NetworkActionCollection.SurvivalAnalysisMenu;
import org.reactome.cytoscape3.NodeActionCollection.CancerGeneIndexMenu;
import org.reactome.cytoscape3.NodeActionCollection.FetchFIsMenu;
import org.reactome.cytoscape3.NodeActionCollection.GeneCardMenu;

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
        
        //Cache the bundlecontext.
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
        MapTableToNetworkTablesTaskFactory mapNetworkAttrTFServiceRef = getService(context,MapTableToNetworkTablesTaskFactory.class);
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

        //Initialize and register the FI VIsual Style with the framework,
        //allowing it to be used by all Reactome FI classes.
        FIVisualStyleImpl styleHelper = new FIVisualStyleImpl(
                visMapManager, visStyleFactory, vmfFactoryC,
                vmfFactoryD, vmfFactoryP, layoutManager,
                taskManager, desktopApp);
        Properties visStyleHelperProps = new Properties();
        visStyleHelperProps.setProperty("title", "FIVisualStyleImpl");
        registerAllServices(context, styleHelper, visStyleHelperProps);
        
        //Initialize and register the TableFormatter with the network
        //so that it is accessible across the app.
        TableFormatterImpl tableFormatter = new TableFormatterImpl(tableFactory, tableManager, networkTableManager, mapNetworkAttrTFServiceRef);
        Properties tableFormatterProps = new Properties();
        tableFormatterProps.setProperty("title", "TableFormatterImpl");
        registerAllServices(context, tableFormatter, tableFormatterProps);
        
        //Instantiate Reactome FI App services
        GeneSetMutationAnalysisAction gsma = new GeneSetMutationAnalysisAction(desktopApp);

        MicroarrayAnalysisAction maa = new MicroarrayAnalysisAction(desktopApp);
        UserGuideAction uga = new UserGuideAction();
        HotNetAnalysisAction hna = new HotNetAnalysisAction(desktopApp);
        
        // Register said Reactome FI Services with the OSGi framework.
        registerAllServices(context, gsma, new Properties());
        registerAllServices(context, hna, new Properties());
        registerAllServices(context, maa, new Properties());
//        registerAllServices(context, pathwayLoadAction, new Properties());
        registerAllServices(context, uga, new Properties());

        // Instantiate and register the context menus for the network view
        NetworkActionCollection networkMenu = new NetworkActionCollection();
        ClusterFINetworkMenu clusterMenu = networkMenu.new ClusterFINetworkMenu();
        Properties clusterProps = new Properties();
        clusterProps.setProperty("title", "Cluster FI Network");
        clusterProps.setProperty("preferredMenu", "Apps.Reactome FI");
        registerService(context, clusterMenu,
                CyNetworkViewContextMenuFactory.class, clusterProps);
        
        FIAnnotationFetcherMenu fiFetcherMenu = networkMenu.new FIAnnotationFetcherMenu();
        Properties fiFetcherProps = new Properties();
        fiFetcherProps.setProperty("title", "Fetch FI Annotations");
        fiFetcherProps.setProperty("preferredMenu", "Apps.Reactome FI");
        registerService(context, fiFetcherMenu, CyNetworkViewContextMenuFactory.class, fiFetcherProps);
        
        NetworkPathwayEnrichmentMenu netPathMenu = networkMenu.new NetworkPathwayEnrichmentMenu();
        Properties netPathProps = new Properties();
        netPathProps.setProperty("title", "Network Pathway Enrichment");
        String preferredMenuText = "Apps.Reactome FI.Analyze Network Functions[10]";
        netPathProps.setProperty("preferredMenu", preferredMenuText);
        registerService(context, netPathMenu, CyNetworkViewContextMenuFactory.class, netPathProps);
        
        NetworkGOCellComponentMenu netGOCellMenu = networkMenu.new NetworkGOCellComponentMenu();
        Properties netGOCellProps = new Properties();
        netGOCellProps.setProperty("title", "Network GO Cell Component");
        netGOCellProps.setProperty("preferredMenu", preferredMenuText);
        registerService(context, netGOCellMenu, CyNetworkViewContextMenuFactory.class, netGOCellProps);
        
        NetworkGOBioProcessMenu netGOBioMenu = networkMenu.new NetworkGOBioProcessMenu();
        Properties netGOBioProps = new Properties();
        netGOBioProps.setProperty("title", "Network GO Biological Process");
        netGOBioProps.setProperty("preferredMenu", preferredMenuText);
        registerService(context, netGOBioMenu, CyNetworkViewContextMenuFactory.class, netGOBioProps);
        
        NetworkGOMolecularFunctionMenu netGOMolMenu = networkMenu.new NetworkGOMolecularFunctionMenu();
        Properties netGOMolProps = new Properties();
        netGOMolProps.setProperty("title", "Network GO Molecular Function");
        netGOMolProps.setProperty("preferredMenu", preferredMenuText);
        registerService(context, netGOMolMenu, CyNetworkViewContextMenuFactory.class, netGOMolProps);
        
        ModulePathwayEnrichmentMenu modPathMenu = networkMenu.new ModulePathwayEnrichmentMenu();
        Properties modPathProps = new Properties();
        preferredMenuText = "Apps.Reactome FI.Analyze Module Functions[30]";
        modPathProps.setProperty("title", "Module Pathway Enrichment");
        modPathProps.setProperty("preferredMenu", preferredMenuText);
        registerService(context, modPathMenu, CyNetworkViewContextMenuFactory.class, modPathProps);
        
        ModuleGOCellComponentMenu modCellMenu = networkMenu.new ModuleGOCellComponentMenu();
        Properties modCellProps = new Properties();
        modCellProps.setProperty("title", "Module GO Cell Component");
        modCellProps.setProperty("preferredMenu", preferredMenuText);
        registerService(context, modCellMenu, CyNetworkViewContextMenuFactory.class, modCellProps);
        
        ModuleGOBioProcessMenu modBioMenu = networkMenu.new ModuleGOBioProcessMenu();
        Properties modBioProps = new Properties();
        modBioProps.setProperty("title", "Module GO Biological Process");
        modBioProps.setProperty("preferredMenu", preferredMenuText);
        registerService(context, modBioMenu, CyNetworkViewContextMenuFactory.class, modBioProps);
        
        ModuleGOMolecularFunctionMenu modMolMenu = networkMenu.new ModuleGOMolecularFunctionMenu();
        Properties modMolProps = new Properties();
        modMolProps.setProperty("title", "Module GO Molecular Function");
        modMolProps.setProperty("preferredMenu", preferredMenuText);
        registerService(context, modMolMenu, CyNetworkViewContextMenuFactory.class, modMolProps);
        
        SurvivalAnalysisMenu survivalMenu = networkMenu.new SurvivalAnalysisMenu();
        Properties survivalMenuProps = new Properties();
        survivalMenuProps.setProperty("title", "Survival Analysis");
        survivalMenuProps.setProperty("preferredMenu", preferredMenuText);
        registerService(context, survivalMenu, CyNetworkViewContextMenuFactory.class, survivalMenuProps);
        
        LoadCancerGeneIndexForNetwork fetchCGINetwork = networkMenu.new LoadCancerGeneIndexForNetwork();
        Properties fetchCGINetprops = new Properties();
        fetchCGINetprops.setProperty("title", "Fetch Cancer Gene Index");
        fetchCGINetprops.setProperty("preferredMenu", "Apps.Reactome FI");
        registerService(context, fetchCGINetwork, CyNetworkViewContextMenuFactory.class, fetchCGINetprops);
        
        // Instantiate and register the context menus for the node views
        NodeActionCollection nodeActionCollection = new NodeActionCollection();
        GeneCardMenu geneCardMenu = nodeActionCollection.new GeneCardMenu();
        Properties geneCardProps = new Properties();
        geneCardProps.setProperty("title", "Gene Card");
        geneCardProps.setProperty("preferredMenu", "Apps.Reactome FI");
        registerService(context, geneCardMenu,
                CyNodeViewContextMenuFactory.class, geneCardProps);
        
        CancerGeneIndexMenu cgiMenu = nodeActionCollection.new CancerGeneIndexMenu();
        Properties cgiMenuProps = new Properties();
        cgiMenuProps.setProperty("title", "Fetch Cancer Gene Index");
        cgiMenuProps.setProperty("preferredMenu", "Apps.Reactome FI");
        registerService(context, cgiMenu, CyNodeViewContextMenuFactory.class, cgiMenuProps);
        
        FetchFIsMenu fetchFIs = nodeActionCollection.new FetchFIsMenu();
        Properties fetchFIsProps = new Properties();
        fetchFIsProps.setProperty("title", "Fetch FIs");
        fetchFIsProps.setProperty("preferredMenu", "Apps.Reactome FI");
        registerService(context, fetchFIs, CyNodeViewContextMenuFactory.class, fetchFIsProps);
        
        //Instantiate and register the context menus for edge views
        EdgeActionCollection edgeAC = new EdgeActionCollection();
        EdgeQueryFIMenuItem edgeQueryMenu = edgeAC.new EdgeQueryFIMenuItem();
        Properties edgeMenuProps = new Properties();
        edgeMenuProps.setProperty("title", "Query FI Source");
        edgeMenuProps.setProperty("preferredMenu", "Apps.Reactome FI");
        registerService(context, edgeQueryMenu, CyEdgeViewContextMenuFactory.class, edgeMenuProps);
        //Register the listener for cleaning things up after network destruction.
        FISessionCleanup sessionCleanup = new FISessionCleanup();
        registerService(context, sessionCleanup, NetworkViewDestroyedListener.class, new Properties());
    }

}
