/*
 * Created on Apr 11, 2016
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.VariableAssignment;
import org.reactome.pathway.factorgraph.IPACalculator;

/**
 * This customized JDialog is used to perform two sample comparison display.
 * @author gwu
 *
 */
public class SampleComparisonPanel extends IPAPathwaySummaryPane {
    public static final String TITLE = "Sample Comparison";
    private JTable inferenceTable;
    private JTable observationTable;
    // Used to synchronize selection
    private Selectable inferenceTableSelectionHandler;
    private Selectable observationTableSelectionHandler;
    
    public SampleComparisonPanel() {
        super(TITLE);
    }
    
    @Override
    protected void synchronizeSampleSelection() {
    }

    @Override
    protected void modifyContentPane() {
        // Modifing existing controls
        // Re-create control tool bars
        for (int i = 0; i < controlToolBar.getComponentCount(); i++) {
            controlToolBar.remove(i);
        }
        // Add a label
        outputResultLabel = new JLabel("Comparison between ");
        controlToolBar.add(outputResultLabel);
        controlToolBar.add(closeGlue);
        highlightPathwayBtn = new JRadioButton("Highlight pathway with difference");
        highlightPathwayBtn.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    highlightPathway();
            }
        });
        PlugInObjectManager.getManager().registerRadioButton("HighlightPathway",
                                                             highlightPathwayBtn);
        // This should be un-selected as default
        highlightPathwayBtn.setSelected(false);
        controlToolBar.add(highlightPathwayBtn);
        controlToolBar.add(closeBtn);
        
        // Show two tables in a tab
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        inferenceTable = new JTable();
        InferenceComparisonTableModel inferenceModel = new InferenceComparisonTableModel();
        inferenceTable.setModel(inferenceModel);
        inferenceTable.setAutoCreateRowSorter(true);
        tabbedPane.addTab("Inference", new JScrollPane(inferenceTable));
        observationTable = new JTable();
        ObservationComparisonTableModel observationModel = new ObservationComparisonTableModel();
        observationTable.setModel(observationModel);
        observationTable.setAutoCreateRowSorter(true);
        tabbedPane.add("Observation", new JScrollPane(observationTable));
        add(tabbedPane, BorderLayout.CENTER);
        
        installListeners();
    }
    
    /**
     * Synchronize selections.
     */
    private void installListeners() {
        final SelectionMediator selectionMediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        inferenceTableSelectionHandler = new InferenceTableSelectionHandler();
        observationTableSelectionHandler = new ObservationTableSelectionHandler();
        selectionMediator.addSelectable(inferenceTableSelectionHandler);
        selectionMediator.addSelectable(observationTableSelectionHandler);
        
        inferenceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    selectionMediator.fireSelectionEvent(inferenceTableSelectionHandler);
            }
        });
    }
    
    @Override
    protected void handleGraphEditorSelection(PathwayEditor editor) {
    }

    public void setData(String sample1,
                        String sample2,
                        FactorGraphInferenceResults results) {
        // Need to make sure the original selection is kept
        outputResultLabel.setText("Comparing samples: " + sample1 + " and " + sample2);
        ComparisonTableModel model = (ComparisonTableModel) inferenceTable.getModel();
        model.setSamples(sample1, sample2, results);
        model = (ComparisonTableModel) observationTable.getModel();
        model.setSamples(sample1, sample2, results);
    }
    
    private class InferenceComparisonTableModel extends ComparisonTableModel {
        
        public InferenceComparisonTableModel() {
        }
        
        public Long getDbId(int row) {
            Object text = data.get(row).get(0);
            if (text == null)
                return null;
            return new Long(text.toString());
        }

        @Override
        protected void resetData(String sample1, 
                                 String sample2, 
                                 FactorGraphInferenceResults results) {
            data.clear();
            if (results == null)
                return;
            Set<Variable> pathwayVars = PlugInUtilities.getPathwayVars(results.getFactorGraph());
            List<VariableInferenceResults> varResults = results.getVariableInferenceResults(pathwayVars);
            // In order to calculate p-values
            Map<Variable, List<Double>> varToRandomIPAs = results.generateRandomIPAs(varResults);
            for (VariableInferenceResults varResult : varResults) {
                Variable var = varResult.getVariable();
                Map<String, ArrayList<Double>> sampleToValues = varResult.getSampleToValues();
                List<Double> posterior = sampleToValues.get(sample1);
                List<Double> prior = varResult.getPriorValues();
                Double ipa1 = IPACalculator.calculateIPA(prior, posterior);
                posterior = sampleToValues.get(sample2);
                prior = varResult.getPriorValues();
                Double ipa2 = IPACalculator.calculateIPA(prior, posterior);
                Double diff = ipa1 - ipa2;
                List<Object> row = new ArrayList<>();
                data.add(row);
                String propValue = var.getProperty(ReactomeJavaConstants.DB_ID);
                if (propValue != null) {
                    Long dbId = new Long(var.getProperty(ReactomeJavaConstants.DB_ID));
                    row.add(dbId);
                }
                else
                    row.add(null); // Use for the position filling
                row.add(var.getName());
                row.add(ipa1);
                row.add(ipa2);
                row.add(diff);
                // For the time being, we will not calculate p-values
            }
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            // Since the first value in the list is DB_ID or null
            return super.getValueAt(rowIndex, columnIndex + 1);
        }
        
    }
    
    private class ObservationComparisonTableModel extends ComparisonTableModel {
        
        public ObservationComparisonTableModel() {
        }

        @Override
        protected void resetData(String sample1, 
                                 String sample2,
                                 FactorGraphInferenceResults results) {
            Observation<Number> observation1 = getSampleObservation(sample1, results);
            if (observation1 == null)
                throw new IllegalStateException("Cannot find observation for " + sample1);
            Observation<Number> observation2 = getSampleObservation(sample2, results);
            if (observation2 == null)
                throw new IllegalStateException("Cannot find observation for " + sample2);
            // Get a list of variables
            List<VariableAssignment<Number>> varAssgns1 = observation1.getVariableAssignments();
            Map<Variable, Number> varToValue1 = getVarToValue(varAssgns1);
            List<VariableAssignment<Number>> varAssgns2 = observation2.getVariableAssignments();
            Map<Variable, Number> varToValue2 = getVarToValue(varAssgns2);
            // Get all variables
            Set<Variable> allVars = new HashSet<Variable>(varToValue1.keySet());
            allVars.addAll(varToValue2.keySet());
            for (Variable var : allVars) {
                Number value1 = varToValue1.get(var);
                Number value2 = varToValue2.get(var);
                List<Object> row = new ArrayList<>();
                data.add(row);
                row.add(var.getName());
                row.add(value1);
                row.add(value2);
                if (value1 == null || value2 == null)
                    row.add(null);
                else
                    row.add(value1.doubleValue() - value2.doubleValue());
            }
        }
        
        private Map<Variable, Number> getVarToValue(List<VariableAssignment<Number>> varAssgns) {
            Map<Variable, Number> varToValue = new HashMap<Variable, Number>();
            for (VariableAssignment<Number> varAssgn : varAssgns) {
                varToValue.put(varAssgn.getVariable(),
                               varAssgn.getAssignment());
            }
            return varToValue;
        }

        protected Observation<Number> getSampleObservation(String sample,
                                                           FactorGraphInferenceResults results) {
            // Extract observation for this sample
            Observation<Number> observation = null;
            for (Observation<Number> obs : results.getObservations()) {
                if (obs.getName().equals(sample)) {
                    observation = obs;
                    break;
                }
            }
            return observation;
        }
        
    }
    
    private abstract class ComparisonTableModel extends AbstractTableModel {
        private String[] columnNames = new String[] {
                "Entity",
                "Sample1",
                "Sample2",
                "Difference"
        };
        // Values to be displayed. Use List for easy handling
        protected List<List<Object>> data;
        
        protected ComparisonTableModel() {
            data = new ArrayList<>();
        }
        
        public void setSamples(String sample1, 
                               String sample2,
                               FactorGraphInferenceResults results) {
            columnNames[1] = sample1;
            columnNames[2] = sample2;
            resetData(sample1, sample2, results);
            fireTableStructureChanged(); // So the column names can be updated
        }
        
        protected abstract void resetData(String sample1, 
                                          String sample2,
                                          FactorGraphInferenceResults results);
        
        @Override
        public int getRowCount() {
            if (data == null)
                return 0;
            return data.size();
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return String.class;
            return Double.class;
        }
        
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (data == null)
                return null;
            return data.get(rowIndex).get(columnIndex);
        }
    }
    
    private class InferenceTableSelectionHandler implements Selectable {
        private List<Long> selectedIds = null;

        @Override
        public void setSelection(List selection) {
            inferenceTable.clearSelection();
            if (selection == null || selection.size() == 0)
                return;
            InferenceComparisonTableModel model = (InferenceComparisonTableModel) inferenceTable.getModel();
            int lastRow = -1;
            for (int i = 0; i < inferenceTable.getRowCount(); i++) {
                int modelRow = inferenceTable.convertRowIndexToModel(i);
                Long dbId = model.getDbId(modelRow);
                if (selection.contains(dbId)) {
                    inferenceTable.addRowSelectionInterval(i, i);
                    lastRow = i;
                }
            }
            if (lastRow > -1) {
                Rectangle rect = inferenceTable.getCellRect(lastRow, 0, false);
                inferenceTable.scrollRectToVisible(rect);
            }
        }

        @Override
        public List getSelection() {
            if (selectedIds == null)
                selectedIds = new ArrayList<>();
            else
                selectedIds.clear();
            selectedIds = new ArrayList<>();
            int[] rows = inferenceTable.getSelectedRows();
            if (rows != null && rows.length > 0) {
                InferenceComparisonTableModel model = (InferenceComparisonTableModel) inferenceTable.getModel();
                for (int row : rows) {
                    Long dbId = model.getDbId(inferenceTable.convertRowIndexToModel(row));
                    if (dbId != null)
                        selectedIds.add(dbId);
                }
            }
            return selectedIds;
        }
        
    }
    
    private class ObservationTableSelectionHandler implements Selectable {

        @Override
        public void setSelection(List selection) {
        }

        @Override
        public List getSelection() {
            
            return null;
        }
        
    }
}
