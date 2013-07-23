package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.r3.graph.GeneClusterPair;
import org.reactome.r3.graph.NetworkClusterResult;


/**
 * This class provides a bunch of methods to modify a network (e.g. clustering,
 * retrieving cancer gene index, etc). Most functions appear as pop-up menus in
 * the network view. All context menu items are contained as subclasses
 * 
 * @author Eric T. Dawson
 * 
 */
class NetworkActionCollection
{
    private TableHelper tableHelper;
    ServiceReference tableFormatterServRef;
    TableFormatterImpl tableFormatter;
    // private ModuleBasedSurvivalHelper survivalHelper;
    public NetworkActionCollection()
    {
        tableHelper = new TableHelper();
    }
    private void getTableFormatter()
    {
        try
        {
            BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
            ServiceReference servRef = context.getServiceReference(TableFormatter.class.getName());
            if (servRef != null)
            {
                this.tableFormatterServRef = servRef;
                this.tableFormatter = (TableFormatterImpl) context.getService(servRef);
            }
            else
                throw new Exception();
        }
        catch (Throwable t)
        {
            JOptionPane.showMessageDialog(null, "The table formatter could not be retrieved.", "Table Formatting Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
    private void releaseTableFormatter()
    {
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        context.ungetService(tableFormatterServRef);
    }
    //Subclasses for performing various network actions via context menus
    //in the network view.
    /**
     * A class for the network view right-click menu item
     * which clusters the network and a corresponding task/factory.
     * @author Eric T. Dawson
     *
     */
    class ClusterFINetworkMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view)
        {
            JMenuItem clusterMenuItem = new JMenuItem("Cluster FI Network");
            clusterMenuItem.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    CyTable netTable = view.getModel().getDefaultNetworkTable();
                    String clustering = netTable.getRow(view.getModel().getSUID()).get("clustering_Type", String.class);
                    try
                    {
                        if (clustering != null && !clustering.equals(TableFormatterImpl.getSpectralPartitionCluster()))
                        {   
                            CySwingApplication desktopApp = PlugInScopeObjectManager.getManager().getCySwingApp();
                            int reply = JOptionPane.showConfirmDialog(desktopApp.getJFrame(), 
                                "The displayed network has been clustered before using a different algorithm.\n" +
                                "You may get different clustering results using this clustering feature. Do\n" +
                                "you want to continue?", 
                                "Clustering Algorithm Warning", 
                                JOptionPane.OK_CANCEL_OPTION);
                            
                            if (reply != JOptionPane.OK_CANCEL_OPTION)
                                return;
                        }
                        ClusterFINetworkTaskFactory clusterFactory = new ClusterFINetworkTaskFactory(
                                view);
                        BundleContext context = PlugInScopeObjectManager
                                .getManager().getBundleContext();
                        ServiceReference taskMgrRef = context
                                .getServiceReference(TaskManager.class.getName());
                        TaskManager taskMgr = (TaskManager) context
                                .getService(taskMgrRef);
                        taskMgr.execute(clusterFactory.createTaskIterator());
                        context.ungetService(taskMgrRef);
                    }
                    catch (Throwable t)
                    {
                        JOptionPane.showMessageDialog(null,
                                "The network cannot be clustered at this time\n"
                                        + t, "Error in Clustering Network",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    
                }

            });

            return new CyMenuItem(clusterMenuItem, 3.0f);
        }

    }

    class ClusterFINetworkTaskFactory extends AbstractTaskFactory
    {
        private CyNetworkView view;

        ClusterFINetworkTaskFactory(CyNetworkView view)
        {
            super();
            this.view = view;
        }

        @Override
        public TaskIterator createTaskIterator()
        {
            return new TaskIterator(new ClusterFINetworkTask(view));
        }
    }

    class ClusterFINetworkTask extends AbstractTask
    {
        private CyNetworkView view;

        public ClusterFINetworkTask(CyNetworkView view)
        {
            super();
            this.view = view;
        }

        @Override
        public void run(TaskMonitor tm) throws Exception
        {
            tm.setProgress(-1);
            tm.setStatusMessage("Clustering FI Network...");
            List<CyEdge> edgeList = view.getModel().getEdgeList();
            try
            {
                getTableFormatter();
                tableFormatter.makeModuleAnalysisTables(view.getModel());
                RESTFulFIService service = new RESTFulFIService(view);
                // The below method takes CyEdges as an input type, but with the
                // reorganization of the API in 3.x it should really take the
                // name
                // of the nodes (nodes now have a Long SUID and not a String
                // Identifier).
                
                NetworkClusterResult clusterResult = service.cluster(edgeList,
                        view);
                Map<String, Integer> nodeToCluster = new HashMap<String, Integer>();
                List<GeneClusterPair> geneClusterPairs = clusterResult
                        .getGeneClusterPairs();
                if (geneClusterPairs != null)
                {
                    for (GeneClusterPair geneCluster : geneClusterPairs)
                    {
                        nodeToCluster.put(geneCluster.getGeneId(), geneCluster
                                .getCluster());
                    }
                }
                
                tableHelper.loadNodeAttributesByName(view, "module",
                        nodeToCluster);
                tableHelper.storeClusteringType(view, TableFormatterImpl
                        .getSpectralPartitionCluster());
                Map<String, Object> nodeToSamples = tableHelper
                        .getNodeTableValuesByName(view.getModel(), "samples",
                                String.class);
                try
                {
                    BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
                    ServiceReference servRef = context.getServiceReference(FIVisualStyle.class.getName());
                    FIVisualStyleImpl visStyler = (FIVisualStyleImpl) context.getService(servRef);
                    visStyler.setVisualStyle(view);    
                }
                catch (Throwable t)
                {
                    JOptionPane.showMessageDialog(null, "The visual style could not be applied.", "Visual Style Error",
                            JOptionPane.ERROR_MESSAGE);
                }
                releaseTableFormatter();
            }
            catch (Exception e)
            {
                JOptionPane.showMessageDialog(null, "There was an error during network clustering.", "Error in clustering", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }

    }

    class FIAnnotationFetcherMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem fetchFIAnnotationsMenu = new JMenuItem(
                    "Fetch FI Annotations");
            return new CyMenuItem(fetchFIAnnotationsMenu, 1.0f);
        }

    }
    
    class NetworkPathwayEnrichmentMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem netPathMenuItem = new JMenuItem(
                    "Network Pathway Enrichment Analysis");
            return new CyMenuItem(netPathMenuItem, 2.0f);
        }

    }

    class NetworkGOCellComponentMenu implements CyNetworkViewContextMenuFactory
    {
        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            return null;

        }
    }

    class NetworkGOBioProcessMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    class NetworkGOMolecularFunctionMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    class ModulePathwayEnrichmentMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    class ModuleGOCellComponentMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    class ModuleGOBioProcessMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    class ModuleGOMolecularFunctionMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    class SurvivalAnalysisMenuFactory implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
