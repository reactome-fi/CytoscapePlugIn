/*
 * Created on Oct 8, 2015
 *
 */
package org.reactome.cytoscape3;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.apache.commons.math.MathException;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.ServiceProperties;
import org.gk.util.DialogControlPane;
import org.gk.util.ProgressPane;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.reactome.cytoscape.fipgm.FIPGMResults;
import org.reactome.cytoscape.fipgm.FilterableTTestTablePlotPane;
import org.reactome.cytoscape.pgm.ObservationDataDialog;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;

/**
 * Handle some specific popup menus related to a FI network generated for the FI PGM model.
 * @author gwu
 *
 */
public class PGMFINetworkPopupMenuHandler extends GeneSetFINetworkPopupMenuHandler {
    
    /**
     * Default constructor.
     */
    public PGMFINetworkPopupMenuHandler() {
    }

    @Override
    protected void installMenus() {
        super.installMenus();
        ShowObservationsMenu menu = new ShowObservationsMenu();
        installOtherNetworkMenu(menu, "Show Observation");
        // Add a popup menu for nodes
        ShowGeneObservationsMenu geneMenu = new ShowGeneObservationsMenu();
        addNodePopup(geneMenu, "Show Observation");
        
        ShowGeneInferenceMenu infResultsMenu = new ShowGeneInferenceMenu();
        addNodePopup(infResultsMenu, "Show Inference Results");
    }

    private void addNodePopup(CyNodeViewContextMenuFactory geneMenu, String title) {
        Properties geneMenuProp = new Properties();
        geneMenuProp.setProperty(ServiceProperties.TITLE, title);
        geneMenuProp.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(PlugInObjectManager.getManager().getBundleContext(), 
                     geneMenu,
                     CyNodeViewContextMenuFactory.class, 
                     geneMenuProp);
    }
    
    private void showInferenceResults(CyNetworkView view) {
        ProgressPane progressPane = new ProgressPane();
        progressPane.setText("Showing results for genes...");
        progressPane.setIndeterminate(true);
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        frame.setGlassPane(progressPane);
        frame.getGlassPane().setVisible(true);
        FIPGMResults results = FIPGMResults.getResults();
        
        List<CyNode> selectedNodes = CyTableUtil.getNodesInState(view.getModel(),
                                                                 CyNetwork.SELECTED,
                                                                 true);
        if (selectedNodes == null || selectedNodes.size() == 0) {
            JOptionPane.showMessageDialog(frame,
                                          "Error in showing results: choose one or more genes first to show results!",
                                          "Error in Showing Results",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
            return;
        }
        Map<String, Variable> nameToVar = results.getNameToVariable();
        Set<Variable> selectedVars = new HashSet<>();
        CyTable nodeTable = view.getModel().getDefaultNodeTable();
        for (CyNode node : selectedNodes) {
            String nodeName = nodeTable.getRow(node.getSUID()).get("name", String.class);
            if (nodeName == null || !nameToVar.containsKey(nodeName))
                continue; // Just in case
            selectedVars.add(nameToVar.get(nodeName));
        }
        if (selectedVars.size() == 0) {
            JOptionPane.showMessageDialog(frame,
                                          "Error in showing results: no results for selected nodes!",
                                          "Error in Showing Results",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
            return;
        }
        Map<String, Map<Variable, Double>> sampleToVarToScore = FIPGMResults.getResults().getSampleToVarToScore(selectedVars);
        Map<String, Map<Variable, Double>> randomSampleToVarToScore = FIPGMResults.getResults().getRandomSampleToVarToScore(selectedVars);
        NodeResultsDialog dialog = new NodeResultsDialog(view);
        try {
            dialog.showResults(sampleToVarToScore, randomSampleToVarToScore);
            dialog.setSize(750, 600);
            dialog.setModal(false);
            frame.getGlassPane().setVisible(false);
            dialog.setVisible(true);
        }
        catch(Exception e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in showing results: " + e.getMessage(),
                                          "Error in Showing Results",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
        }
    }
    
    private void handleTableSelection(JTable table,
                                      CyNetworkView view) {
        if (view == null)
            return;
        int[] selectedRows = table.getSelectedRows();
        if (selectedRows == null || selectedRows.length == 0)
            return;
        // Get selected genes
        Set<String> selectedGenes = new HashSet<>();
        for (int selectedRow : selectedRows) {
            String name = (String) table.getValueAt(selectedRow, 0); // The first should be variable name
            int index = name.indexOf("_");
            if (index > 0)
                name = name.substring(0, index);
            selectedGenes.add(name);
        }
        if (selectedGenes.size() == 0)
            return;
        TableHelper tableHelper = new TableHelper();
        tableHelper.selectNodes(view, 
                                "name", 
                                selectedGenes);
    }
    
    private void showObservations(CyNetworkView view) {
        ProgressPane progressPane = new ProgressPane();
        progressPane.setText("Showing observations for genes...");
        progressPane.setIndeterminate(true);
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        frame.setGlassPane(progressPane);
        frame.getGlassPane().setVisible(true);
        FIPGMResults results = FIPGMResults.getResults();
        List<Observation<Number>> observations = results.getObservations();
        if (observations == null || observations.size() == 0) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Observations have not been loaded. Note: Observations have not been saved in\n"
                                        + "analysis results.",
                                          "Empty Observations",
                                          JOptionPane.INFORMATION_MESSAGE);
            frame.getGlassPane().setVisible(false);
            return;
        }
        ObservationDataDialog dialog = new FIPGMObservationDialog(view);
        List<CyNode> selectedNodes = CyTableUtil.getNodesInState(view.getModel(),
                                                                 CyNetwork.SELECTED,
                                                                 true);
        if (selectedNodes != null && selectedNodes.size() > 0)
            dialog.setTargetGenes(PlugInUtilities.getSelectedGenesInNetwork(view.getModel()));
        else
            dialog.setTargetGenes(PlugInUtilities.getDisplayedGenesInNetwork(view.getModel()));
        try {
            dialog.setObservations(observations, 
                                   results.getRandomObservations());
            dialog.setSize(750, 600);
            dialog.setModal(false);
            frame.getGlassPane().setVisible(false);
            dialog.setVisible(true);
        }
        catch(Exception e) { // Want to catch any exception in addition to MathException.
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in showing observations: " + e.getMessage(),
                                          "Error in Showing Observations",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
            return;
        }
    }
    
    private class ShowGeneInferenceMenu implements CyNodeViewContextMenuFactory {
        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView, View<CyNode> nodeView) {
            JMenuItem showInferenceResults = new JMenuItem("Show Inference Results");
            showInferenceResults.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // It is kind of slow to show observations since t-test is needed to
                    // perform for all genes. So use a thread
                    Thread t = new Thread() {
                        public void run() {
                            showInferenceResults(netView);
                        }
                    };
                    t.start();
                }
            });
            return new CyMenuItem(showInferenceResults, 1010.0f); // We want it to display at the bottom.
        }
    }
    
    /**
     * To show the observation data. 
     */
    private class ShowGeneObservationsMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView, View<CyNode> nodeView) {
            JMenuItem showObservations = new JMenuItem("Show Observation");
            showObservations.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // It is kind of slow to show observations since t-test is needed to
                    // perform for all genes. So use a thread
                    Thread t = new Thread() {
                        public void run() {
                            showObservations(netView);
                        }
                    };
                    t.start();
                }
            });
            return new CyMenuItem(showObservations, 1000.0f); // We want it to display at the bottom.
        }
    }
    
    /**
     * To show the observation data. 
     */
    private class ShowObservationsMenu implements CyNetworkViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView view) {
            JMenuItem showObservations = new JMenuItem("Show Observation");
            showObservations.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // It is kind of slow to show observations since t-test is needed to
                    // perform for all genes. So use a thread
                    Thread t = new Thread() {
                        public void run() {
                            showObservations(view);
                        }
                    };
                    t.start();
                }
            });
            return new CyMenuItem(showObservations, 1000.0f); // We want it to display at the bottom.
        }
    }
    
    private class NodeResultsDialog extends JDialog {
        // To show results by doing t-test
        private FilterableTTestTablePlotPane tTestPlotPane;
        private CyNetworkView view;
        
        public NodeResultsDialog(CyNetworkView view) {
            this.view = view;
            init();
        }
        
        private void init() {
            tTestPlotPane = new FilterableTTestTablePlotPane();
            tTestPlotPane.hideFilterPane();
            tTestPlotPane.getNoteLabel().setVisible(false);
            tTestPlotPane.gettTestPlotPane().getBottomTitleLabel().setText("Total displayed genes: ");
            tTestPlotPane.getResultTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    handleTableSelection(tTestPlotPane.getResultTable());
                }
            });
            getContentPane().add(tTestPlotPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            controlPane.getCancelBtn().setVisible(false);
            controlPane.getOKBtn().addActionListener(new ActionListener() {
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose(); 
                }
            });
            getContentPane().add(controlPane, BorderLayout.SOUTH);
        }
        
        private void handleTableSelection(JTable table) {
            PGMFINetworkPopupMenuHandler.this.handleTableSelection(table, view);
        }
        
        public void showResults(Map<String, Map<Variable, Double>> sampleToVarToScore,
                                Map<String, Map<Variable, Double>> randomSampleToVarToScore) throws MathException {
            tTestPlotPane.setSampleResults(sampleToVarToScore, randomSampleToVarToScore);
            tTestPlotPane.gettTestPlotPane().getBottomPValueLabel().setText(sampleToVarToScore.values().size() + "");
        }
    }
    
    private class FIPGMObservationDialog extends ObservationDataDialog {
        private CyNetworkView view;
        private ServiceRegistration registration;
        private boolean isRemoved;
        private List<CyNode> preSelected;
        // Two flags
        private boolean isFromTable;
        private boolean isFromNetwork;
        
        public FIPGMObservationDialog(CyNetworkView view) {
            this.view = view;
        }
        
        @Override
        protected void init() {
            super.init();
            // Want to enable selection from the network view
            RowsSetListener selectionListener = new RowsSetListener() {
                
                @Override
                public void handleEvent(RowsSetEvent e) {
                    if (e.containsColumn(CyNetwork.SELECTED)) {
                        handleNodeSelection();
                    }
                }
            };
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            registration = context.registerService(RowsSetListener.class.getName(),
                                                   selectionListener,
                                                   null);
            isRemoved = false;
            addWindowListener(new WindowAdapter() {

                /**
                 * This is used to handle the window closing default action.
                 */
                @Override
                public void windowClosing(WindowEvent e) {
                    unregisterService();
                }

                /**
                 * This is used to handle the OK button.
                 */
                @Override
                public void windowClosed(WindowEvent e) {
                    unregisterService();
                }
                
            });
        }
        
        private void unregisterService() {
            if (isRemoved)
                return;
            BundleContext context = PlugInObjectManager.getManager().getBundleContext();
            registration.unregister();
            isRemoved = true;
        }
        
        private void handleNodeSelection() {
            if (isFromTable)
                return;
            if (view == null || view.getModel() == null || view.getModel().getDefaultNodeTable() == null)
                return;
            CyNetwork network = view.getModel();
            List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network,
                                                                     CyNetwork.SELECTED,
                                                                     true);
            if (selectedNodes.equals(preSelected))
                return;
            isFromNetwork = true;
            selectNodes(selectedNodes);
            preSelected = selectedNodes;
            isFromNetwork = false;
        }
        
        private void selectNodes(List<CyNode> selectedNodes) {
            Set<String> geneNames = new HashSet<String>();
            CyNetwork network = view.getModel();
            for (CyNode node : selectedNodes) {
                geneNames.add(network.getRow(node).get(CyNetwork.NAME, String.class));
            }
            // Will select table rows
            JTable table = getTTestTablePlotPane().getTable();
            table.clearSelection();
            int firstRow = -1;
            for (int i = 0; i < table.getRowCount(); i++) {
                String name = (String) table.getValueAt(i, 0);
                int index = name.indexOf("_");
                String geneName = name.substring(0,  index);
                if (geneNames.contains(geneName)) {
                    table.addRowSelectionInterval(i, i);
                    if (firstRow < 0)
                        firstRow = i;
                }
            }
            if (firstRow > 0) {
                // Need to scroll
                Rectangle rect = table.getCellRect(firstRow, 0, false);
                table.scrollRectToVisible(rect);
            }
        }

        @Override
        protected void handleTableSelection(JTable table) {
            if (isFromNetwork)
                return;
            isFromTable = true;
            PGMFINetworkPopupMenuHandler.this.handleTableSelection(table, view);
            isFromTable = false;
        }
        
    }
    
}
