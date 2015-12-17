/*
 * Created on Oct 15, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import org.apache.commons.math.MathException;
import org.cytoscape.view.model.CyNetworkView;
import org.gk.graphEditor.PathwayEditor;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.Variable;

/**
 * This is used to show impact scores for displayed genes in a FI network.
 * @author gwu
 *
 */
public class ImpactGeneValueTablePane extends ImpactSampleValueTablePane {
    public static final String TITLE = "Impact Gene Values";
    
    private FilterableTTestTablePlotPane tTestPane;
    private JRadioButton highlightNetworkBtn;
    // Two flags to control action
    private boolean isFromTable;
    private boolean isFromNetwork;
    
    /**
     * @param title
     */
    public ImpactGeneValueTablePane() {
    }
    
    @Override
    public String getTitle() {
        return TITLE;
    }

    @Override
    protected void addTablePlotPane() {
        // Add a JSplitPane for the table and a new graph pane to display graphs
        for (int i = 0; i < getComponentCount(); i++) {
            Component comp = getComponent(i);
            if (comp instanceof JScrollPane) {
                remove(comp);
                break;
            }
        }
        tTestPane = new FilterableTTestTablePlotPane();
        tTestPane.hideFilterPane();
        add(tTestPane, BorderLayout.CENTER);
        tTestPane.getResultTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                doTableSelection(e);
            }
        });
        adjustGUIs();
    }
    
    /**
     * A method to display results for all genes in the FI network.
     */
    private void showAllResults() {
        PGMImpactResultLoadTask task = new PGMImpactResultLoadTask();
        task.setUsedToShowResults(true);
        Thread t = new Thread(task);
        t.start();
    }
    
    @Override
    protected void adjustGUIs() {
        super.adjustGUIs();
        // Just want to insert a new button
        // Re-create control tool bars
        for (int i = 0; i < controlToolBar.getComponentCount(); i++) {
            controlToolBar.remove(i);
        }
        controlToolBar.add(ipaLabel);
        controlToolBar.add(closeGlue);
        
        // For network size-based highlighting
        highlightNetworkBtn = new JRadioButton("Highlight Network");
        PlugInObjectManager.getManager().registerRadioButton("HighlightNetwork", highlightNetworkBtn);
        highlightNetworkBtn.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    _highlightNetwork();
            }
        });
        controlToolBar.add(highlightNetworkBtn);
        
        JButton showAllResultsBtn = new JButton("Show All Results");
        showAllResultsBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                showAllResults();
            }
        });
        controlToolBar.add(showAllResultsBtn);
        controlToolBar.add(closeBtn);
    }
    
    public void selectViewButtonWithoutFiringEvent() {
        ItemListener[] listeners = highlightNetworkBtn.getItemListeners();
        if (listeners != null && listeners.length > 0)
            for (ItemListener l : listeners)
                highlightNetworkBtn.removeItemListener(l);
        highlightNetworkBtn.setSelected(true);
        if (listeners != null && listeners.length > 0)
            for (ItemListener l : listeners)
                highlightNetworkBtn.addItemListener(l);
    }
    
    private void _highlightNetwork() {
        Map<String, Double> geneToScore = getGeneToScore();
        TableHelper tableHelper = new TableHelper();
        tableHelper.storeNodeAttributesByName(view.getModel(),
                                              FIVisualStyle.GENE_VALUE_ATT,
                                              geneToScore);
        FIPGMImpactVisualStyle style = new FIPGMImpactVisualStyle();
        // Need to recreate visual style so that node sizes can be reset.
        style.setVisualStyle(view, true);
        view.updateView();
    }
    
    private Map<String, Double> getGeneToScore() {
        TableModel model = (TableModel) tTestPane.getResultTable().getModel();
        Map<String, Double> geneToScore = new HashMap<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            String gene = (String) model.getValueAt(i, 0);
            Double score = new Double(model.getValueAt(i, 1).toString());
            geneToScore.put(gene, score);
        }
        return geneToScore;
    }

    @Override
    protected void doContentTablePopup(MouseEvent e) {
    }

    @Override
    protected void handleGraphEditorSelection(PathwayEditor editor) {
    }
    
    @Override
    public void setNetworkView(CyNetworkView view) {
        if (this.view == view)
            return; // There is no need to do anything
        super.setNetworkView(view);
        showResults();
    }

    @Override
    protected void doTableSelection(ListSelectionEvent e) {
        if (isFromNetwork)
            return;
        isFromTable = true;
        // Get a set of names for selection
        Set<String> selectedNames = new HashSet<>();
        JTable table = tTestPane.getResultTable();
        if (table.getSelectedRowCount() > 0) {
            int[] rows = table.getSelectedRows();
            for (int row : rows) {
                String name = (String) table.getValueAt(row, 0);
                selectedNames.add(name);
            }
        }
        selectNodes(view, "name", selectedNames);
        isFromTable = false;
    }
    
    @Override
    public void setVariables(List<Variable> variables) {
        if (isFromTable)
            return;
        isFromNetwork = true;
        JTable tTestResultTable = tTestPane.getResultTable();
        tTestResultTable.clearSelection();
        if (variables.size() > 0) {
            // Get a set of names for selection
            Set<String> names = new HashSet<>();
            for (Variable var : variables)
                names.add(var.getName());
            // Find the row index in the table model
            TableModel model = tTestResultTable.getModel();
            int selected = -1;
            for (int i = 0; i < model.getRowCount(); i++) {
                String tmp = (String) model.getValueAt(i, 0);
                if (names.contains(tmp)) {
                    int viewIndex = tTestResultTable.convertRowIndexToView(i);
                    tTestResultTable.getSelectionModel().addSelectionInterval(viewIndex, viewIndex);
                    if (selected == -1)
                        selected = viewIndex;
                }
            }
            if (selected > -1) {
                Rectangle rect = tTestResultTable.getCellRect(selected, 0, false);
                tTestResultTable.scrollRectToVisible(rect);
            }
        }
        isFromNetwork = false;
    }

    /**
     * Show inference results for the displayed FI network.
     */
    public void showResults() {
        if (view == null)
            return; // There is no need to do this. This may occur during a network view switch when a new network is created.
        Collection<Variable> variables = nodeToVar.values();
        Map<String, Map<Variable, Double>> sampleToVarToScore = FIPGMResults.getResults().getSampleToVarToScore(variables);
        Map<String, Map<Variable, Double>> randomSampleToVarToScore = FIPGMResults.getResults().getRandomSampleToVarToScore(variables);
        try {
            tTestPane.setSampleResults(sampleToVarToScore, randomSampleToVarToScore);
        }
        catch(MathException e) {
            JOptionPane.showMessageDialog(PlugInObjectManager.getManager().getCytoscapeDesktop(),
                                          "Error in displaying results: " + e,
                                          "Error in Result Display",
                                          JOptionPane.ERROR_MESSAGE);
        }
    }
    
}
