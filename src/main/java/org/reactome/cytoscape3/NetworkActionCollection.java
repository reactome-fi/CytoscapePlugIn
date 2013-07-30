package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    /**
     * A method to grab the TableFormatterImpl from the address space so that
     * new tables can be created and filled with the proper columns.
     * @author Eric T Dawson
     */
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
    /**
     * A method to release the TableFormatterImpl
     * @author Eric T Dawson
     */
    private void releaseTableFormatter()
    {
        BundleContext context = PlugInScopeObjectManager.getManager().getBundleContext();
        context.ungetService(tableFormatterServRef);
    }
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
                            
                            if (reply != JOptionPane.OK_OPTION)
                                return;
                        }
                        clusterFINetwork(view);
//                        ClusterFINetworkTaskFactory clusterFactory = new ClusterFINetworkTaskFactory(
//                                view);
//                        BundleContext context = PlugInScopeObjectManager
//                                .getManager().getBundleContext();
//                        ServiceReference taskMgrRef = context
//                                .getServiceReference(TaskManager.class.getName());
//                        TaskManager taskMgr = (TaskManager) context
//                                .getService(taskMgrRef);
//                        taskMgr.execute(clusterFactory.createTaskIterator());
//                        context.ungetService(taskMgrRef);
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
            clusterFINetwork(view);
            
        }

    }
    /**
     * A class for the network view context menu item to fetch FI annotations.
     * @author Eric T. Dawson
     *
     */
    class FIAnnotationFetcherMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem fetchFIAnnotationsMenu = new JMenuItem(
                    "Fetch FI Annotations");
            fetchFIAnnotationsMenu.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
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
            netPathMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            return new CyMenuItem(netPathMenuItem, 2.0f);
        }

    }

    class NetworkGOCellComponentMenu implements CyNetworkViewContextMenuFactory
    {
        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem netGOCellComponentMenuItem = new JMenuItem("GO Cell Component");
            netGOCellComponentMenuItem.addActionListener(new ActionListener()
            {

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            return new CyMenuItem(netGOCellComponentMenuItem, 3.0f);

        }
    }

    class NetworkGOBioProcessMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem netGOBioMenuItem = new JMenuItem("GO Biological Process");
            netGOBioMenuItem.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            // TODO Auto-generated method stub
            return new CyMenuItem(netGOBioMenuItem, 4.0f);
        }

    }

    class NetworkGOMolecularFunctionMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem netGOMolFuncMenuItem = new JMenuItem("GO Molecular Function");
            netGOMolFuncMenuItem.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            // TODO Auto-generated method stub
            return new CyMenuItem(netGOMolFuncMenuItem, 5.0f);
        }

    }

    class ModulePathwayEnrichmentMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem modPathMenuItem = new JMenuItem("Pathway Enrichment");
            modPathMenuItem.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            // TODO Auto-generated method stub
            return new CyMenuItem(modPathMenuItem, 6.0f);
        }

    }

    class ModuleGOCellComponentMenu implements CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem modGOCellMenuItem = new JMenuItem("GO Cell Component") ;
            modGOCellMenuItem.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            // TODO Auto-generated method stub
            return new CyMenuItem(modGOCellMenuItem, 7.0f);
        }

    }
    /**
     * A class for showing the gene ontology biological processes
     * of a given module using the network view context menu item.
     * @author Eric T. Dawson
     *
     */
    class ModuleGOBioProcessMenu implements CyNetworkViewContextMenuFactory
    {
        
        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem modGOBioProcessMenuItem = new JMenuItem("GO Biological Process");
            modGOBioProcessMenuItem.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            // TODO Auto-generated method stub
            return new CyMenuItem(modGOBioProcessMenuItem, 8.0f);
        }

    }

    /**
     * A class for showing the gene ontology molecular functions
     * of a given module using the given network view context menu item.
     * @author Eric T. Dawson
     *
     */
    class ModuleGOMolecularFunctionMenu implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem modGOMolFuncMenuItem = new JMenuItem("GO Molecular Function");
            modGOMolFuncMenuItem.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            // TODO Auto-generated method stub
            return new CyMenuItem(modGOMolFuncMenuItem, 9.0f);
        }

    }

    /**
     * A class for performing survival analysis using the network
     * view context menu item.
     * @author Eric T. Dawson
     *
     */
    class SurvivalAnalysisMenuFactory implements
            CyNetworkViewContextMenuFactory
    {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView view)
        {
            JMenuItem survivalAnalysisMenuItem = new JMenuItem("Survival Analysis");
            survivalAnalysisMenuItem.addActionListener(new ActionListener(){

                @Override
                public void actionPerformed(ActionEvent e)
                {
                    // TODO Auto-generated method stub
                    
                }
                
            });
            // TODO Auto-generated method stub
            return new CyMenuItem(survivalAnalysisMenuItem, 10.f);
        }

    }
    /**
     * A method for performing clustering of the FI network. This function
     * is only applicable for Spectral Partition Clustering. Reclustering networks
     * originally clustered with other algorithms may change results.
     * @param view The current network view.
     * @author Eric T Dawson
     */
    public void clusterFINetwork(CyNetworkView view)
    {
        try
        {
            getTableFormatter();
            tableFormatter.makeModuleAnalysisTables(view.getModel());
            List<CyEdge> edgeList = view.getModel().getEdgeList();
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
                showModuleInTab(nodeToCluster, nodeToSamples, clusterResult.getModularity(), view);
                
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
    private void showModuleInTab(Map<String, Integer> nodeToCluster,
            Map<String, Object> nodeToSamples,
            Double modularity,
            CyNetworkView view) {
        Map<String, Set<String>> nodeToSampleSet = extractNodeToSampleSet(nodeToSamples);
        ResultDisplayHelper.getHelper().showModuleInTab(nodeToCluster, 
                nodeToSampleSet,
                modularity, 
                view);
    }

    private Map<String, Set<String>> extractNodeToSampleSet(Map<String, Object> nodeToSamples) {
        Map<String, Set<String>> nodeToSampleSet = null;
        if (nodeToSamples != null) {
            nodeToSampleSet = new HashMap<String, Set<String>>();
            for (String node : nodeToSamples.keySet()) {
                String sampleText = (String) nodeToSamples.get(node);
                String[] tokens = sampleText.split(";");
                Set<String> set = new HashSet<String>();
                for (String token : tokens)
                    set.add(token);
                nodeToSampleSet.put(node, set);
            }
        }
        return nodeToSampleSet;
    }
}
