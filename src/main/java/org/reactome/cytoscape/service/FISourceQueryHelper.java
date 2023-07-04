/*
 * Created on Feb 14, 2014
 *
 */
package org.reactome.cytoscape.service;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.cytoscape.view.model.CyNetworkView;
import org.gk.util.ProgressPane;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.funcInt.Interaction;
import org.reactome.funcInt.ReactomeSource;

/**
 * This class is used to help doing FI source query.
 * @author gwu
 *
 */
public class FISourceQueryHelper {
    
    /**
     * Default constructor.
     */
    public FISourceQueryHelper() {
    }
    
    public void queryFISource(String partner1,
                              String partner2) {
        queryFISource(partner1,
                      partner2, 
                      PlugInObjectManager.getManager().getCytoscapeDesktop());
    }
    
    /**
     * An overloaded method to query the source for a FI specified by its
     * two participating partners.
     * @param partner1
     * @param partner2
     * @param parent
     */
    public void queryFISource(String partner1,
                              String partner2,
                              Component parent) {
        try {
            RESTFulFIService fiService = new RESTFulFIService();
            List<Interaction> interactions = fiService.queryEdge(partner1, partner2);
            //There should be exactly one reaction
            if (interactions.isEmpty()) {
                JOptionPane.showMessageDialog(parent,
                                              "No FI source can be found for FI, " + partner1 + " - " + partner2 + ".",
                                              "Error in FI Source Query",
                                              JOptionPane.ERROR_MESSAGE);
                return;
            }
            Window parentWindow = (Window) SwingUtilities.getAncestorOfClass(Window.class, parent);
            displayInteraction(interactions,
                               partner1 + " - " + partner2,
                               parentWindow);
        }
        catch (Exception e) {
            PlugInUtilities.showErrorMessage("Error in FI Source Query", 
                                             "Error in fetching the FI source: " + e.getMessage());
        }
    }
                              
    /**
     * Query the FI source for a FI specified by its two partners.
     * @param partner1
     * @param partner2
     * @param view
     */
    public void queryFISource(String partner1,
                              String partner2,
                              CyNetworkView view) {
        ProgressPane progPane = new ProgressPane();
        progPane.setText("Querying FI Source");
        // Use the main JFrame as the anchor for querying since there is nothing
        // provided.
        JFrame frame = PlugInObjectManager.getManager().getCytoscapeDesktop();
        frame.setGlassPane(progPane);
        frame.getGlassPane().setVisible(true);
        try {
            RESTFulFIService fiService = new RESTFulFIService(view);
            List<Interaction> interactions = fiService.queryEdge(partner1, partner2);
            //There should be exactly one reaction
            if (interactions.isEmpty()) {
                PlugInUtilities.showErrorMessage("Error in FI Source Query", 
                                                 "No FI source can be found for FI, " + partner1 + " - " + partner2 + ".");
                frame.getGlassPane().setVisible(false);
                return;
            }
            displayInteraction(interactions,
                               partner1 + " - " + partner2,
                               frame);
        }
        catch (Exception e) {
            PlugInUtilities.showErrorMessage("Error in FI Source Query", 
                                             "Error in fetching the FI source: " + e.getMessage());
        }
        frame.getGlassPane().setVisible(false);
    }
    
    private void displayInteraction(List<Interaction> interactions, 
                                    String fi,
                                    Window parentWindow) {
        JDialog dialog = new JDialog(parentWindow);
        dialog.setTitle("Interaction Info");
        dialog.setSize(400, 300);
        dialog.setLocationRelativeTo(parentWindow);
        JPanel supportPanel = createSupportPane(interactions);
        dialog.getContentPane().add(supportPanel, 
                                    BorderLayout.CENTER);
        // Add a label
        JLabel label = new JLabel("Interaction: " + fi);
        // Add an etched border for label
        Border emptyBorder = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        Border etchedBorder = BorderFactory.createEtchedBorder();
        label.setBorder(BorderFactory.createCompoundBorder(etchedBorder, emptyBorder));
        Font font = label.getFont();
        label.setFont(font.deriveFont(Font.BOLD));
        dialog.getContentPane().add(label, BorderLayout.NORTH);
        dialog.setModal(true);
        dialog.setVisible(true);
    }
    
    /**
     * @param interactions
     * @return
     */
    private JPanel createSupportPane(List<Interaction> interactions) {
        JPanel supportPane = new JPanel();
        supportPane.setBorder(BorderFactory.createEtchedBorder());
        supportPane.setLayout(new BorderLayout());
        JTabbedPane supportTabbedPane = new JTabbedPane();
        List<Interaction> predictedFIs = new ArrayList<Interaction>();
        List<Interaction> pathwayFIs = new ArrayList<Interaction>();
        //It is possible that one pair of FIs may actually come from two
        //different sources due to the fact that data comes from normalized
        //amino acid data from Uniprot.
        // The above statement should not be correct. A FI should be either predicted
        // or extracted from pathways. However, such checking actually makes programming
        // simpler. ---- by Guanming Wu.
        for (Interaction i : interactions) {
            if (i.getEvidence() == null)
                pathwayFIs.add(i);
            else
                predictedFIs.add(i);
        }
        if (pathwayFIs.isEmpty()) {
            //Grab the FIs with the highest score.
            Interaction highest = null;
            for (Interaction i : predictedFIs) {
                if (highest == null)
                    highest = i;
                else if (highest.getEvidence().getProbability() < i.getEvidence().getProbability())
                    highest = i;
            }
            JTable evidenceTable = new JTable();
            EvidenceTableModel evidenceModel = new EvidenceTableModel();
            TableRowSorter<EvidenceTableModel> sorter = new TableRowSorter<>(evidenceModel);
            evidenceTable.setRowSorter(sorter);
            evidenceModel.setEvidence(highest.getEvidence());
            evidenceTable.setModel(evidenceModel);
            supportTabbedPane.addTab("Support Evidence",
                                     new JScrollPane(evidenceTable));
        }
        else {
            // Only allow pathway FIs.
            Set<ReactomeSource> allSources = new HashSet<ReactomeSource>();
            for (Interaction interaction : pathwayFIs)
                allSources.addAll(interaction.getReactomeSources());
            setReactomeSourceTab(supportTabbedPane, allSources);
        }
        supportPane.add(supportTabbedPane, BorderLayout.CENTER);
        return supportPane;
    }
    
    private void setReactomeSourceTab(JTabbedPane supportTabbedPane,
                                      Set<ReactomeSource> sources) {
        final JTable sourceTable = new JTable();
        sourceTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) 
                    doSourceTablePopup(sourceTable, e);
                else if (e.getClickCount() == 2) {
                    showReactomeSource(sourceTable);
                }
            }
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    doSourceTablePopup(sourceTable, e);
            }
        });
        sourceTable.setToolTipText("Double click or right clik to use popup menu for details");
        ReactomeSourceTableModel sourceModel = new ReactomeSourceTableModel();
        sourceTable.setAutoCreateRowSorter(true);
        // Make sure Long is rendered as the string.
        DefaultTableCellRenderer longRenderer = new DefaultTableCellRenderer();
        longRenderer.setHorizontalAlignment(JLabel.LEFT);
        sourceTable.setDefaultRenderer(Long.class, longRenderer);
        sourceModel.setReactomeSources(sources);
        sourceTable.setModel(sourceModel);
        supportTabbedPane.addTab("Reactome Sources",
                                 new JScrollPane(sourceTable));
    }
    
    private void doSourceTablePopup(final JTable table, 
                                    MouseEvent e) {
        // Work for one selection only
        if (table.getSelectedRowCount() != 1)
            return;
        JPopupMenu popup = new JPopupMenu();
        JMenuItem viewReactomeSource = new JMenuItem("View Reactome Source");
        viewReactomeSource.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showReactomeSource(table);
            }
        });
        popup.add(viewReactomeSource);
        popup.show(table, e.getX(), e.getY());
    }
    
    private void showReactomeSource(JTable table) {
        // Work for one selection only
        if (table.getSelectedRowCount() != 1)
            return;
        ReactomeSourceTableModel tableModel = (ReactomeSourceTableModel) table.getModel();
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        Long id = (Long) tableModel.getValueAt(modelRow,
                                               0);
        ReactomeSourceView sourceView = new ReactomeSourceView();
        sourceView.viewReactomeSource(id, table);
    }
    
}
