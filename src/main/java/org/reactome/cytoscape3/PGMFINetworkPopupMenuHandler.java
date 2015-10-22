/*
 * Created on Oct 8, 2015
 *
 */
package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTable;

import org.apache.commons.math.MathException;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.work.ServiceProperties;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.fipgm.FIPGMResults;
import org.reactome.cytoscape.pgm.ObservationDataDialog;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Observation;

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
        installOtherNetworkMenu(menu, "Show Observations");
        // Add a popup menu for nodes
        ShowGeneObservationsMenu geneMenu = new ShowGeneObservationsMenu();
        Properties geneMenuProp = new Properties();
        geneMenuProp.setProperty(ServiceProperties.TITLE, "Show Observation");
        geneMenuProp.setProperty(ServiceProperties.PREFERRED_MENU, PREFERRED_MENU);
        addPopupMenu(PlugInObjectManager.getManager().getBundleContext(), 
                     geneMenu,
                     CyNodeViewContextMenuFactory.class, 
                     geneMenuProp);
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
        catch(MathException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in showing observations: " + e.getMessage(),
                                          "Error in Showing Observations",
                                          JOptionPane.ERROR_MESSAGE);
            frame.getGlassPane().setVisible(false);
            return;
        }
    }
    
    /**
     * To show the observation data. 
     */
    private class ShowGeneObservationsMenu implements CyNodeViewContextMenuFactory {

        @Override
        public CyMenuItem createMenuItem(final CyNetworkView netView, View<CyNode> nodeView) {
            JMenuItem showObservations = new JMenuItem("Show Observations");
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
            JMenuItem showObservations = new JMenuItem("Show Observations");
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
    
    private class FIPGMObservationDialog extends ObservationDataDialog {
        private CyNetworkView view;
        
        public FIPGMObservationDialog(CyNetworkView view) {
            this.view = view;
        }

        @Override
        protected void handleTableSelection(JTable table) {
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
                String gene = name.substring(0, index);
                selectedGenes.add(gene);
            }
            if (selectedGenes.size() == 0)
                return;
            TableHelper tableHelper = new TableHelper();
            tableHelper.selectNodes(view, 
                                    "name", 
                                    selectedGenes);
        }
        
    }
    
}
