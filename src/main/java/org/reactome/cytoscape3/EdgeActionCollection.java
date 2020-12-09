package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskManager;
import org.cytoscape.work.TaskMonitor;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.drug.InteractionView;
import org.reactome.cytoscape.drug.NetworkDrugManager;
import org.reactome.cytoscape.mechismo.MechismoDataFetcher;
import org.reactome.cytoscape.service.FINetworkGenerator;
import org.reactome.cytoscape.service.FISourceQueryHelper;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;

import edu.ohsu.bcb.druggability.dataModel.Interaction;
/**
 * A class which contains functions performed on edges
 * in the FI network.
 * @author Eric T. Dawson
 *
 */
public class EdgeActionCollection {
    
    public static boolean annotateFIs(CyNetworkView view) {
        ProgressPane progPane = new ProgressPane();
        progPane.setIndeterminate(true);
        progPane.setText("Fetching FI Annotations...");
        CySwingApplication desktopApp = PlugInObjectManager.getManager().getCySwingApplication();
        JFrame parentFrame = desktopApp.getJFrame();
        parentFrame.setGlassPane(progPane);
        parentFrame.getGlassPane().setVisible(true);
        FINetworkGenerator helper = new FINetworkGenerator();
        boolean rtn = helper.annotateFIs(view);
        parentFrame.getGlassPane().setVisible(false);
        return rtn;
    }
    
    public static void setEdgeNames(CyNetworkView view)
    {
        TableHelper tableHelper = new TableHelper();
        if (view.getModel().getEdgeCount() != 0)
        {
            for (CyEdge edge : view.getModel().getEdgeList())
            {
                tableHelper.storeEdgeName(edge, view);
            }
        }
        tableHelper = null;
    }
    
    static class DrugTargetDetailsMenuItem implements CyEdgeViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView networkView,
                                         final View<CyEdge> edgeView) {
            String edgeType = getEdgeType(networkView, edgeView);
            if (edgeType == null || !edgeType.equals("Drug/Target"))
                return null;
            JMenuItem menuItem = new JMenuItem("Show Drug/Target Interaction Details");
            menuItem.addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    showDetails(networkView,
                                edgeView);
                }
            });
            return new CyMenuItem(menuItem, 10.0f);
        }
        
        private void showDetails(CyNetworkView networkView,
                                 View<CyEdge> edgeView) {
            Interaction interaction = NetworkDrugManager.getManager().getInteraction(networkView.getModel(),
                                                                                     edgeView.getModel());
            if (interaction == null) {
                JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                              "Cannot find an interaction for displayed drug/target edge!",
                                              "Error in Drug/Target Interaction",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            InteractionView view = new InteractionView();
            view.setInteraction(interaction);
            view.setModal(false);
            view.setVisible(true);
        }
    }
    
    private static String getEdgeType(CyNetworkView networkView,
                                      View<CyEdge> edgeView) {
        // Check the edge type first
        CyTable edgeTable = networkView.getModel().getDefaultEdgeTable();
        String edgeType = (String) edgeTable.getRow(edgeView.getModel().getSUID()).get("EDGE_TYPE", String.class);
        return edgeType;
    }
    
    
    static class EdgeLoadMechsimoMenuItem implements CyEdgeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(CyNetworkView networkView, View<CyEdge> edgeView) {
            if (!PlugInObjectManager.getManager().isMechismoEnabled() ||
                !MechismoDataFetcher.isMechismoDataLoaded()) // Make sure it works after the results are loaded.
                return null;
            JMenuItem item = new JMenuItem("Fetch Mechismo Results");
            CyTable table = networkView.getModel().getDefaultEdgeTable();
            String name = table.getRow(edgeView.getModel().getSUID()).get("name", String.class);
            item.addActionListener(event -> queryMechismoResults(name));
            return new CyMenuItem(item, 2.0f);
        }
        
        private void queryMechismoResults(String name) {
            Task task = new AbstractTask() {
                @Override
                public void run(TaskMonitor monitor) throws Exception {
                    monitor.setTitle("Mechismo Results");
                    monitor.setStatusMessage("Loading Mechismo mutation results...");
                    monitor.setProgress(0.0d);
                    MechismoDataFetcher fetcher = new MechismoDataFetcher();
                    try {
                        org.reactome.mechismo.model.Interaction interaction = fetcher.loadMechismoInteraction(name);
                        if (interaction == null) {
                            monitor.setProgress(1.0d);
                            // Use a new thread to avoid being locked by the Cytoscape task thread.
                            SwingUtilities.invokeLater(new Thread() {
                                public void run() {
                                    JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                            "No mechismo analysis result is available for this interaction.",
                                            "No Result",
                                            JOptionPane.INFORMATION_MESSAGE);
                                    return;
                                }
                            });
                        }
                    }
                    catch(Exception e) {
                        monitor.setProgress(1.0d);
                        throw e;
                    }
                    monitor.setProgress(1.0d);
                }
            };
            TaskIterator taskIterator = new TaskIterator(task);
            TaskManager taskManager = PlugInObjectManager.getManager().getTaskManager();
            taskManager.execute(taskIterator);
        }
    
    }
    
    
    static class EdgeQueryFIMenuItem implements CyEdgeViewContextMenuFactory {
        
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view, final View<CyEdge> edgeView) {
            // Check the edge type first
            String edgeType = getEdgeType(view, edgeView);
            if (edgeType == null || !edgeType.equals("FI"))
                return null;
            JMenuItem edgeQueryFIItem = new JMenuItem("Query FI Source");
            edgeQueryFIItem.addActionListener(new ActionListener(){
                
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    queryFISource(view, edgeView);
                }
                
            });
            return new CyMenuItem(edgeQueryFIItem, 1.0f);
        }
        
        private void queryFISource(CyNetworkView view,
                                   View<CyEdge> edgeView) {
            CyTable nodeTable = view.getModel().getDefaultNodeTable();
            Long sourceSUID =  edgeView.getModel().getSource().getSUID();
            String source = nodeTable.getRow(sourceSUID).get("name", String.class);
            
            Long targetSUID = edgeView.getModel().getTarget().getSUID();
            String target = nodeTable.getRow(targetSUID).get("name", String.class);
            
            FISourceQueryHelper helper = new FISourceQueryHelper();
            helper.queryFISource(source, target, view);
        }
        
    }
    

    
}
