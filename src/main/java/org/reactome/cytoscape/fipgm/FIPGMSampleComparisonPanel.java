/*
 * Created on Apr 13, 2016
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import org.cytoscape.view.model.CyNetworkView;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.reactome.cytoscape.pgm.SampleComparisonPanel;
import org.reactome.cytoscape.service.FIVisualStyle;
import org.reactome.cytoscape.service.TableHelper;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.common.DataType;

/**
 * Used to perform sample comparison for PGM-FI results.
 * @author gwu
 *
 */
public class FIPGMSampleComparisonPanel extends SampleComparisonPanel {
    // To syn two tables selection
    private SelectionMediator mediator;
    private Selectable inferenceTableSelectionHandler;
    
    /**
     * Default constructor.
     */
    public FIPGMSampleComparisonPanel() {
    }
    
    @Override
    protected void modifyContentPane() {
        super.modifyContentPane();
    }

    @Override
    protected void createHighlightViewBtn() {
        highlightViewBtn = new JRadioButton("Highlight network with difference");
        highlightViewBtn.setSelected(false);
        PlugInObjectManager.getManager().registerRadioButton("HighlightNetwork",
                                                             highlightViewBtn);
    }

    @Override
    public void highlightView() {
        if (!highlightViewBtn.isSelected())
            return;
        CyNetworkView view = PlugInUtilities.getCurrentNetworkView();
        if (view == null || view.getModel() == null)
            return;
        FIPGMInferenceComparisonTableModel model = (FIPGMInferenceComparisonTableModel) inferenceTable.getModel();
        Map<String, Double> geneToDiff = model.getGeneToDiff();
        TableHelper tableHelper = new TableHelper();
        tableHelper.storeNodeAttributesByName(view.getModel(),
                                              FIVisualStyle.GENE_VALUE_ATT,
                                              geneToDiff);
        view.updateView();
    }

    public void compare(String sample1, String sample2) {
        outputResultLabel.setText("Comparing samples: " + sample1 + " and " + sample2);
        if (FIPGMResults.getResults() == null)
            return; // Nothing to be displayed.
        ComparisonTableModel model = (ComparisonTableModel) inferenceTable.getModel();
        model.setSamples(sample1, sample2);
        model = (ComparisonTableModel) observationTable.getModel();
        model.setSamples(sample1, sample2);
        highlightView();
    }

    @Override
    protected void installInferenceTableListeners() {
        inferenceTableSelectionHandler = new InferenceTableSelectionHandler();
        if (mediator == null)
            mediator = new SelectionMediator();
        mediator.addSelectable(inferenceTableSelectionHandler);
        inferenceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    mediator.fireSelectionEvent(inferenceTableSelectionHandler);
                }
            }
        });
    }

    @Override
    protected void installObservationTableListeners() {
        super.installObservationTableListeners();
        if (mediator == null)
            mediator = new SelectionMediator();
        mediator.addSelectable(observationTableSelectionHandler);
        observationTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    mediator.fireSelectionEvent(observationTableSelectionHandler);
                }
            }
        });
    }

    @Override
    protected ComparisonTableModel createInferenceTableModel() {
        return new FIPGMInferenceComparisonTableModel();
    }

    @Override
    protected ComparisonTableModel createObservationTableModel() {
        return new FIPGMObservationComparisonTableModel();
    }
    
    private Set<String> getDisplayedGenes() {
        CyNetworkView networkView = PlugInUtilities.getCurrentNetworkView();
        if (networkView == null)
            return null;
        Set<String> genes = PlugInUtilities.getDisplayedGenesInNetwork(networkView.getModel());
        return genes;
    }
    
    private class InferenceTableSelectionHandler implements Selectable {

        @Override
        public void setSelection(List selection) {
            ListSelectionModel selectionModel = inferenceTable.getSelectionModel();
            selectionModel.setValueIsAdjusting(true);
            selectionModel.clearSelection();
            if (selection != null && selection.size() > 0) {
                Set<String> genes = new HashSet<>();
                for (Object obj : selection) {
                    String name = obj.toString();
                    int index = name.indexOf("_");
                    if (index > 0)
                        genes.add(name.substring(0, index));
                }
                if (genes.size() > 0) {
                    TableModel model = inferenceTable.getModel();
                    int lastRow = -1;
                    for (int i = 0; i < model.getRowCount(); i++) {
                        String value = (String) model.getValueAt(i, 0);
                        if (genes.contains(value)) {
                            int row = inferenceTable.convertRowIndexToView(i);
                            if (row > lastRow)
                                lastRow = row;
                            selectionModel.addSelectionInterval(row, row);
                        }
                    }
                    if (lastRow > -1) {
                        Rectangle rect = inferenceTable.getCellRect(lastRow, 0, false);
                        inferenceTable.scrollRectToVisible(rect);
                    }
                }
            }
            selectionModel.setValueIsAdjusting(false);
        }

        @Override
        public List getSelection() {
            List<String> list = new ArrayList<>();
            int[] rows = inferenceTable.getSelectedRows();
            if (rows != null && rows.length > 0) {
                TableModel model = inferenceTable.getModel();
                for (int row : rows) {
                    String name = (String) model.getValueAt(inferenceTable.convertRowIndexToModel(row),
                                                            0);
                    for (DataType type : DataType.values())
                        list.add(name + "_" + type);
                }
            }
            return list;
        }
        
    }
    
    private class FIPGMObservationComparisonTableModel extends ObservationComparisonTableModel {

        @Override
        protected void resetData(String sample1, String sample2) {
            data.clear();
            resetData(sample1, 
                      sample2, 
                      FIPGMResults.getResults().getObservations(),
                      getDisplayedGenes());
        }
        
    }
    
    private class FIPGMInferenceComparisonTableModel extends ComparisonTableModel {

        @Override
        protected void resetData(String sample1, 
                                 String sample2) {
            data.clear();
            Set<String> genes = getDisplayedGenes();
            if (genes == null)
                return;
            FIPGMResults results = FIPGMResults.getResults();
            Map<String, Map<Variable, Double>> sampleToVarToScore = results.getSampleToVarToScore();
            Map<Variable, Double> varToScore1 = sampleToVarToScore.get(sample1);
            Map<Variable, Double> varToScore2 = sampleToVarToScore.get(sample2);
            // Usually these two maps should have the same keys. However, just in case
            Set<Variable> allVars = new HashSet<Variable>(varToScore1.keySet());
            allVars.addAll(varToScore2.keySet());
            for (Variable var : allVars) {
                if (!genes.contains(var.getName()))
                    continue;
                Double score1 = varToScore1.get(var);
                Double score2 = varToScore2.get(var);
                List<Object> row = new ArrayList<>();
                data.add(row);
                row.add(var.getName());
                row.add(score1);
                row.add(score2);
                Double diff = null;
                if (score1 != null && score2 != null)
                    diff = score1 - score2;
                row.add(diff);
            }
        }
        
        public Map<String, Double> getGeneToDiff() {
            Map<String, Double> geneToDiff = new HashMap<>();
            for (int i = 0; i < getRowCount(); i++) {
                String gene = (String) getValueAt(i, 0);
                Double diff = (Double) getValueAt(i, getColumnCount() - 1);
                geneToDiff.put(gene, diff);
            }
            return geneToDiff;
        }
    }
    
}
