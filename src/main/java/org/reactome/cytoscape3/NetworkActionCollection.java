package org.reactome.cytoscape3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.model.CyEdge;
import org.cytoscape.task.AbstractNetworkViewTask;
import org.cytoscape.task.AbstractNetworkViewTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
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

    class ClusterFINetworkContextMenu extends AbstractNetworkViewTaskFactory
    {
        @Override
        public TaskIterator createTaskIterator(CyNetworkView view)
        {
            return new TaskIterator(new ClusterFINetworkTask(view));
        }
    }
    class ClusterFINetworkTask extends AbstractNetworkViewTask
    {

        public ClusterFINetworkTask(CyNetworkView view)
        {
            super(view);
        }

        @Override
        public void run(TaskMonitor tm) throws Exception
        {
            tm.setProgress(-1);
            tm.setStatusMessage("Clustering FI Network...");
            List<CyEdge> edges = view.getModel().getEdgeList();
            System.out.println("here");
            try
            {
                RESTFulFIService service = new RESTFulFIService();
                NetworkClusterResult clusterResult = service.cluster(edges);
                System.out.println("here");
                Map<String, Integer> nodeToCluster = new HashMap<String, Integer>();
                List<GeneClusterPair> geneClusterPairs = clusterResult.getGeneClusterPairs();
                if (geneClusterPairs != null)
                {
                    for (GeneClusterPair geneCluster : geneClusterPairs)
                        nodeToCluster.put(geneCluster.getGeneId(),
                                          geneCluster.getCluster());
                }
                tableManager.loadNodeAttributesByName(view, "module", nodeToCluster);
                System.out.println("here");
                tableManager.storeClusteringType(view, CyTableFormatter.getSpectralPartitionCluster());
                Map<String, Object> nodeToSamples = tableManager.getNodeTableValuesByName(view.getModel(), 
                        "samples", String.class);
                System.out.println(nodeToSamples);
            }
            catch(Exception e){
                System.out.println(e);
            }
        }
        
    }

//    class FIAnnotationFetcherMenu extends AbstractNetworkViewTaskFactory
//    {
//        @Override
//        public TaskIterator createTaskIterator(CyNetworkView arg0)
//        {
//            // TODO Auto-generated method stub
//            return null;
//        }
//
//    }
    /*
     * class FIAnnotationFetcherTask extends AbstractNetworkViewTask
     * 
     */

    class NetworkPathwayEnrichmentMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            // TODO Auto-generated method stub
            return null;
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
    class ModulePathwayEnrichmentMenu implements CyNetworkViewContextMenuFactory
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
    class ModuleGOMolecularFunctionMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
    class SurvivalAnalysisMenuFactory implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView arg0)
        {
            // TODO Auto-generated method stub
            return null;
        }
        
    }
}
