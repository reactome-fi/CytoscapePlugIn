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
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.cytoscape.service.GeneLevelSelectionHandler;
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
    protected JTable inferenceTable;
    protected JTable observationTable;
    // Used to synchronize selection
    private Selectable inferenceTableSelectionHandler;
    protected Selectable observationTableSelectionHandler;
    // Results to be displayed
    private FactorGraphInferenceResults fgResults;
    
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
        
        createHighlightViewBtn();
        
        highlightViewBtn.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    highlightView();
            }
        });
        controlToolBar.add(highlightViewBtn);
        controlToolBar.add(closeBtn);
        
        // Show two tables in a tab
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        inferenceTable = new JTable();
        ComparisonTableModel inferenceModel = createInferenceTableModel();
        inferenceTable.setModel(inferenceModel);
        inferenceTable.setAutoCreateRowSorter(true);
        tabbedPane.addTab("Inference", new JScrollPane(inferenceTable));
        observationTable = new JTable();
        ComparisonTableModel observationModel = createObservationTableModel();
        observationTable.setModel(observationModel);
        observationTable.setAutoCreateRowSorter(true);
        tabbedPane.add("Observation", new JScrollPane(observationTable));
        add(tabbedPane, BorderLayout.CENTER);
        
        installListeners();
    }
    
    
    
    @Override
    protected void createHighlightViewBtn() {
        highlightViewBtn = new JRadioButton("Highlight pathway with difference");
        PlugInObjectManager.getManager().registerRadioButton("HighlightPathway",
                                                             highlightViewBtn);
        // This should be selected as default
        highlightViewBtn.setSelected(false);
    }

    protected ComparisonTableModel createInferenceTableModel() {
        return new InferenceComparisonTableModel();
    }
    
    protected ComparisonTableModel createObservationTableModel() {
        return new ObservationComparisonTableModel();
    }
    
    @Override
    public Map<String, Double> getReactomeIdToIPADiff() {
        InferenceComparisonTableModel tableModel = (InferenceComparisonTableModel) inferenceTable.getModel();
        return tableModel.getDBIdToDiff();
    }

    /**
     * Synchronize selections.
     */
    private void installListeners() {
        installInferenceTableListeners();
        installObservationTableListeners();
    }

    protected void installInferenceTableListeners() {
        inferenceTableSelectionHandler = new InferenceTableSelectionHandler();
        PlugInObjectManager.getManager().getDBIdSelectionMediator().addSelectable(inferenceTableSelectionHandler);
        inferenceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    SelectionMediator selectionMediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
                    selectionMediator.fireSelectionEvent(inferenceTableSelectionHandler);
                }
            }
        });
    }

    protected void installObservationTableListeners() {
        observationTableSelectionHandler = new GeneLevelSelectionHandler();
        ((GeneLevelSelectionHandler)observationTableSelectionHandler).setGeneLevelTable(observationTable);
        PlugInObjectManager.getManager().getObservationVarSelectionMediator().addSelectable(observationTableSelectionHandler);
        observationTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    PlugInObjectManager.getManager().getObservationVarSelectionMediator().fireSelectionEvent(observationTableSelectionHandler);
                }
            }
        });
    }
    
    @Override
    protected void handleGraphEditorSelection(PathwayEditor editor) {
    }

    public void setData(String sample1,
                        String sample2,
                        FactorGraphInferenceResults results) {
        this.fgResults = results;
        // Need to make sure the original selection is kept
        outputResultLabel.setText("Comparing samples: " + sample1 + " and " + sample2);
        ComparisonTableModel model = (ComparisonTableModel) inferenceTable.getModel();
        model.setSamples(sample1, sample2);
        model = (ComparisonTableModel) observationTable.getModel();
        model.setSamples(sample1, sample2);
        highlightView(); // Need to highlight with new values
    }
    
    private class InferenceComparisonTableModel extends ComparisonTableModel {
        
        public InferenceComparisonTableModel() {
        }
        
        public Long getDbId(int row) {
            Object text = data.get(row).get(0);
            if (text == null)
                return null;
            return (Long) text;
        }
        
        public Map<String, Double> getDBIdToDiff() {
            Map<String, Double> dbIdToDiff = new HashMap<>();
            for (List<Object> row : data) {
                Object dbId = row.get(0);
                if (dbId == null)
                    continue;
                dbIdToDiff.put(dbId.toString(),
                               (Double) row.get(row.size() - 1));
            }
            return dbIdToDiff;
        }

        @Override
        protected void resetData(String sample1, 
                                 String sample2) {
            data.clear();
            if (fgResults == null)
                return;
            Set<Variable> pathwayVars = PlugInUtilities.getPathwayVars(fgResults.getFactorGraph());
            List<VariableInferenceResults> varResults = fgResults.getVariableInferenceResults(pathwayVars);
            // In order to calculate p-values
            Map<Variable, List<Double>> varToRandomIPAs = fgResults.generateRandomIPAs(varResults);
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
    
    protected class ObservationComparisonTableModel extends ComparisonTableModel {
        
        public ObservationComparisonTableModel() {
        }

        @Override
        protected void resetData(String sample1, 
                                 String sample2) {
            data.clear();
            resetData(sample1, 
                      sample2, 
                      fgResults.getObservations(),
                      null);
        }
        
        protected void resetData(String sample1, 
                                 String sample2,
                                 List<Observation<Number>> observations,
                                 Set<String> genes) {
            Observation<Number> observation1 = getSampleObservation(sample1, observations);
            if (observation1 == null)
                throw new IllegalStateException("Cannot find observation for " + sample1);
            Observation<Number> observation2 = getSampleObservation(sample2, observations);
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
                if (!shouldAdd(var, genes))
                    continue;
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
        
        protected boolean shouldAdd(Variable var, Set<String> genes) {
            if (genes == null)
                return true;
            String name = var.getName();
            int index = name.indexOf("_");
            if (index < 0)
                return false;
            return genes.contains(name.substring(0, index));
        }
        
        private Map<Variable, Number> getVarToValue(List<VariableAssignment<Number>> varAssgns) {
            Map<Variable, Number> varToValue = new HashMap<Variable, Number>();
            for (VariableAssignment<Number> varAssgn : varAssgns) {
                varToValue.put(varAssgn.getVariable(),
                               varAssgn.getAssignment());
            }
            return varToValue;
        }

        private Observation<Number> getSampleObservation(String sample,
                                                         List<Observation<Number>> observations) {
            // Extract observation for this sample
            Observation<Number> observation = null;
            for (Observation<Number> obs : observations) {
                if (obs.getName().equals(sample)) {
                    observation = obs;
                    break;
                }
            }
            return observation;
        }
        
    }
    
    protected abstract class ComparisonTableModel extends AbstractTableModel {
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
                               String sample2) {
            columnNames[1] = sample1;
            columnNames[2] = sample2;
            resetData(sample1, sample2);
            fireTableStructureChanged(); // So the column names can be updated
        }
        
        protected abstract void resetData(String sample1, 
                                          String sample2);
        
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
            ListSelectionModel selectionModel = inferenceTable.getSelectionModel();
            selectionModel.setValueIsAdjusting(true);
            int lastRow = -1;
            for (int i = 0; i < inferenceTable.getRowCount(); i++) {
                int modelRow = inferenceTable.convertRowIndexToModel(i);
                Long dbId = model.getDbId(modelRow);
                if (selection.contains(dbId)) {
                    inferenceTable.addRowSelectionInterval(i, i);
                    lastRow = i;
                }
            }
            selectionModel.setValueIsAdjusting(false);
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
    
}
