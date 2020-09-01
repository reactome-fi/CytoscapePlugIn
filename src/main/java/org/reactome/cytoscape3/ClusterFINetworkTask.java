package org.reactome.cytoscape3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.cytoscape.model.CyEdge;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.TaskMonitor;
import org.gk.util.ProgressPane;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.reactome.cytoscape.service.FINetworkService;
import org.reactome.cytoscape.service.FINetworkServiceFactory;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.TableFormatter;
import org.reactome.cytoscape.service.TableFormatterImpl;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.r3.graph.GeneClusterPair;
import org.reactome.r3.graph.NetworkClusterResult;

public class ClusterFINetworkTask extends AbstractTask {
    private CyNetworkView view;
    private JFrame frame;
    
    public ClusterFINetworkTask(CyNetworkView view, JFrame frame) {
        this.view = view;
        this.frame = frame;
    }

    @Override
    public void run(TaskMonitor taskMonitor) throws Exception {
        taskMonitor.setTitle("Cluster FI Network");
        taskMonitor.setProgress(0.0d);
        taskMonitor.setStatusMessage("Clustering network...");
        clusterFINetwork();
        taskMonitor.setProgress(1.0d);
    }
    
    private void showModuleInTab(Map<String, Integer> nodeToCluster, Map<String, Object> nodeToSamples,
            Double modularity, CyNetworkView view) {
        Map<String, Set<String>> nodeToSampleSet = PlugInUtilities.extractNodeToSampleSet(nodeToSamples);
        ResultDisplayHelper.getHelper().showModuleInTab(nodeToCluster, nodeToSampleSet, modularity, view);
    }

    /**
     * The actual place for doing network clustering.
     */
    public void clusterFINetwork() {
        ProgressPane progPane = new ProgressPane();
        progPane.setIndeterminate(true);
        progPane.setText("Clustering FI network...");
        frame.setGlassPane(progPane);
        frame.getGlassPane().setVisible(true);
        
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        ServiceReference tableFormatterServRef = context.getServiceReference(TableFormatter.class.getName());
        if (tableFormatterServRef == null)
            return;
            
        TableFormatterImpl tableFormatter = (TableFormatterImpl) context.getService(tableFormatterServRef);
        tableFormatter.makeModuleAnalysisTables(view.getModel());
        List<CyEdge> edgeList = view.getModel().getEdgeList();
        
        try {
            // As of July 23, 2016, this Spectral partition based network clustering
            // has been moved to the client side because of the long process in the server-side
            // for large networks submitted by users.
            FINetworkService service = new FINetworkServiceFactory().getFINetworkService();
            NetworkClusterResult clusterResult = service.cluster(edgeList, view);
            Map<String, Integer> nodeToCluster = new HashMap<String, Integer>();
            List<GeneClusterPair> geneClusterPairs = clusterResult.getGeneClusterPairs();
            if (geneClusterPairs != null) {
                for (GeneClusterPair geneCluster : geneClusterPairs) {
                    nodeToCluster.put(geneCluster.getGeneId(), geneCluster.getCluster());
                }
            }
            
            TableHelper tableHelper = new TableHelper();
            tableHelper.storeNodeAttributesByName(view, "module", nodeToCluster);
            
            progPane.setText("Storing clustering results...");
            tableHelper.storeClusteringType(view, TableFormatterImpl.getSpectralPartitionCluster());
            Map<String, Object> nodeToSamples = tableHelper.getNodeTableValuesByName(view.getModel(), "samples",
                                                                                     String.class);
                                                                                     
            showModuleInTab(nodeToCluster, nodeToSamples, clusterResult.getModularity(), view);
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(frame, "Error in clustering FI network: " + e.getMessage(),
                                          "Error in Clustering", JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
        }
  
        view.updateView();
        
        frame.getGlassPane().setVisible(false);
    }

}
