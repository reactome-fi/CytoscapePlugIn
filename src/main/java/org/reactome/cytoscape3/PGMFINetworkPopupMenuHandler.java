/*
 * Created on Oct 8, 2015
 *
 */
package org.reactome.cytoscape3;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import org.apache.commons.math.MathException;
import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.fipgm.FIPGMResults;
import org.reactome.cytoscape.pgm.ObservationDataDialog;
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
    }
    
    private void showObservations(CyNetworkView view) {
        ProgressPane progressPane = new ProgressPane();
        progressPane.setText("Showing observations for all FI genes...");
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
        ObservationDataDialog dialog = new ObservationDataDialog();
        // This line has to be placed before the line after it!
//        dialog.getTTestTablePlotPane().setMaximumRowsForPlot(250);
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
    
}
