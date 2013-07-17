package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.model.CyEdge;
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
    private CyTableManager tableManager;

    // private ModuleBasedSurvivalHelper survivalHelper;
    public NetworkActionCollection()
    {
        tableManager = new CyTableManager();
    }

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
                    ClusterFINetworkTaskFactory clusterFactory = new ClusterFINetworkTaskFactory(
                            view);
                    BundleContext context = PlugInScopeObjectManager
                            .getManager().getBundleContext();
                    ServiceReference taskMgrRef = context
                            .getServiceReference(TaskManager.class.getName());
                    try
                    {
                        TaskManager taskMgr = (TaskManager) context
                                .getService(taskMgrRef);
                        taskMgr.execute(clusterFactory.createTaskIterator());
                    }
                    catch (Throwable t)
                    {
                        JOptionPane.showMessageDialog(null,
                                "The network cannot be clustered at this time\n"
                                        + t, "Error in Clustering Network",
                                JOptionPane.ERROR_MESSAGE);
                    }
                    context.ungetService(taskMgrRef);
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
            Thread.sleep(3000);
            List<CyEdge> edgeList = view.getModel().getEdgeList();
            // List<String> edgeNames = new ArrayList<String>();
            // for (CyEdge edge : edgeList)
            // {
            //
            // }

            try
            {

                RESTFulFIService service = new RESTFulFIService(view);

                // The below method takes CyEdges as an input type, but with the
                // reorganization of the API in 3.x it should really take the
                // name
                // of the nodes (nodes now have an SUID and not a String
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
                tableManager.loadNodeAttributesByName(view, "module",
                        nodeToCluster);
                tableManager.storeClusteringType(view, CyTableFormatter
                        .getSpectralPartitionCluster());
                Map<String, Object> nodeToSamples = tableManager
                        .getNodeTableValuesByName(view.getModel(), "samples",
                                String.class);
    //            VisualStyleHelper visHelper = ;
                
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }

    }

    class FIAnnotationFetcherMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView arg0)
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
        public CyMenuItem createMenuItem(CyNetworkView arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    class ModuleGOCellComponentMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    class ModuleGOBioProcessMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    class ModuleGOMolecularFunctionMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

    class SurvivalAnalysisMenuFactory implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }

    }

}
