/*
 * Created on Dec 2, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.gk.graphEditor.PathwayEditor;
import org.gk.model.ReactomeJavaConstants;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.factorgraph.ContinuousVariable;
import org.reactome.factorgraph.ContinuousVariable.DistributionType;
import org.reactome.factorgraph.Observation;
import org.reactome.factorgraph.Variable;
import org.reactome.factorgraph.VariableAssignment;
import org.reactome.pathway.factorgraph.IPACalculator;
import org.reactome.r3.util.MathUtilities;

/**
 * @author gwu
 *
 */
public class SampleListComponent extends JPanel implements CytoPanelComponent {
    public static final String TITLE = "Sample List";
    // GUIs
    private JComboBox<String> sampleBox;
    private JPanel typePane;
    private JLabel typeBox;
    private JTable observationTable;
    private JTable inferenceTable;
    // Data to be displayed
    private Map<String, String> sampleToType;
    // Cached information for display
    private FactorGraphInferenceResults results;
    // For synchronization
    private GeneToPathwayEntityHandler observationTableHandler;
    
    public SampleListComponent() {
        init();
    }
    
    private void init() {
        initGUIs();
        PlugInUtilities.registerCytoPanelComponent(this);
    }

    private void initGUIs() {
        // Set up GUIs
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        initNorthPane();
        initCenerPane();
    }

    private void initCenerPane() {
        // Use JTabbedPane for showing observations and inference results
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        
        // Set up the table for observation
        SampleObservationModel observationModel = new SampleObservationModel();
        TableRowSorter<TableModel> observationSorter = new TableRowSorter<TableModel>(observationModel);
        observationTable = new JTable(observationModel);
        observationTable.setRowSorter(observationSorter);
        tabbedPane.addTab("Observation", new JScrollPane(observationTable));
        observationTableHandler = new GeneToPathwayEntityHandler();
        observationTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    observationTableHandler.handleTableSelection(observationTable,
                                                                 0);
                }
            }
        });
        
        
        // Set up the table for inference
        SampleInferenceModel inferenceModel = new SampleInferenceModel();
        TableRowSorter<TableModel> inferenceSorter = new TableRowSorter<TableModel>(inferenceModel);
        inferenceTable = new JTable(inferenceModel);
        inferenceTable.setRowSorter(inferenceSorter);
        tabbedPane.addTab("Inference", new JScrollPane(inferenceTable));
        inferenceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    handleInferenceTableSelection();
            }
        });
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private void handleInferenceTableSelection() {
        if (observationTableHandler.getPathwayEditor() == null)
            return;
        PathwayEditor pathwayEditor = observationTableHandler.getPathwayEditor();
        SampleInferenceModel model = (SampleInferenceModel) inferenceTable.getModel();
        List<Long> dbIds = new ArrayList<>();
        int[] selectedRows = inferenceTable.getSelectedRows();
        if (selectedRows != null) {
            for (int selectedRow : selectedRows) {
                Long dbId = model.getDBId(inferenceTable.convertRowIndexToModel(selectedRow));
                dbIds.add(dbId);
            }
        }
        PlugInUtilities.selectByDbIds(pathwayEditor,
                                      dbIds);
    }

    private void initNorthPane() {
        // There are two panels in the north: one for listing samples
        // another for showing sample type if any
        JPanel northPane = new JPanel();
        northPane.setLayout(new BoxLayout(northPane, BoxLayout.Y_AXIS));
        
        // Show a list of samples
        JLabel sampleLabel = new JLabel("Choose sample:");
        sampleBox = new JComboBox<>();
        sampleBox.setEditable(false);
        DefaultComboBoxModel<String> sampleModel = new DefaultComboBoxModel<>();
        sampleBox.setModel(sampleModel);
        JPanel samplePane = new JPanel();
        samplePane.setBorder(BorderFactory.createEtchedBorder());
        samplePane.add(sampleLabel);
        samplePane.add(sampleBox);
        northPane.add(samplePane);
        
        // Show type information
        JLabel typeLabel = new JLabel("Type:");
        typeBox = new JLabel();
        typeBox.setOpaque(true);
        typeBox.setBackground(Color.WHITE);
        typePane = new JPanel();
        typePane.setBorder(BorderFactory.createEtchedBorder());
        typePane.add(typeLabel);
        typePane.add(typeBox);
        northPane.add(typePane);
        
        // Link these two boxes together
        sampleBox.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                handleSampleSelection();
            }
        });
        
        add(northPane, BorderLayout.NORTH);
    }

    private void handleSampleSelection() {
        String selectedSample = (String) sampleBox.getSelectedItem();
        if (sampleToType != null && sampleToType.size() > 0) {
            String type = sampleToType.get(selectedSample);
            if (type == null)
                typeBox.setText("");
            else
                typeBox.setText(type);
        }
        // Show observation
        SampleObservationModel observationModel = (SampleObservationModel) observationTable.getModel();
        observationModel.setSample(selectedSample);
        SampleInferenceModel inferenceModel = (SampleInferenceModel) inferenceTable.getModel();
        inferenceModel.setSample(selectedSample);
    }
    
    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.EAST;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getTitle() {
        return TITLE;
    }
    
    /**
     * Set the inference results to be displayed in this component.
     * @param results
     */
    public void setInferenceResults(FactorGraphInferenceResults results,
                                    PathwayEditor pathwayEditor) {
        this.results = results;
        observationTableHandler.enableDiagramSelection(pathwayEditor);
        // Initialize the sample box
        DefaultComboBoxModel<String> sampleModel = (DefaultComboBoxModel<String>) sampleBox.getModel();
        sampleModel.removeAllElements();
        List<String> sampleNames = getSampleNames(results);
        for (String sampleName : sampleNames)
            sampleModel.addElement(sampleName);
        sampleBox.setSelectedIndex(0);
        sampleToType = results.getSampleToType();
        if (sampleToType == null || sampleToType.size() == 0)
            typePane.setVisible(false);
        else {
            typePane.setVisible(true);
            // Somehow we have to set the type manually
            handleSampleSelection();
        }
    }
    
    private List<String> getSampleNames(FactorGraphInferenceResults results) {
        List<String> sampleNames = new ArrayList<>();
        for (Observation<Number> obs : results.getObservations())
            sampleNames.add(obs.getName());
        Collections.sort(sampleNames);
        return sampleNames;
    }
    
    private class SampleInferenceModel extends SampleModel {
        public SampleInferenceModel() {
            startIndex = 1;
        }

        @Override
        protected void resetData() {
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
                List<Double> posterior = sampleToValues.get(sample);
                List<Double> prior = varResult.getPriorValues();
                Double ipa = IPACalculator.calculateIPA(prior, posterior);
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
                row.add(ipa);
                // Calculate p-value
                // RandomIPAs should be sorted already
                List<Double> randomIPAs = varToRandomIPAs.get(var);
                Double pvalue = PlugInUtilities.calculateIPAPValue(ipa, randomIPAs);
                row.add(pvalue);
            }
            int totalPermutation = varToRandomIPAs.values().iterator().next().size();
            calculateFDRs(totalPermutation);
        }
        
        public Long getDBId(int rowIndex) {
            if (rowIndex >= data.size())
                return null;
            List<Object> row = data.get(rowIndex);
            return (Long) row.get(0);
        }
        
    }
    
    private class SampleObservationModel extends SampleModel {
        
        public SampleObservationModel() {
            startIndex = 0;
        }

        @Override
        protected void resetData() {
            data.clear();
            if (results == null)
                return; // Cannot do anything here
            // Extract observation for this sample
            Observation<Number> observation = null;
            for (Observation<Number> obs : results.getObservations()) {
                if (obs.getName().equals(sample)) {
                    observation = obs;
                    break;
                }
            }
            if (observation == null)
                throw new IllegalStateException("Cannot find observation for " + sample);
            // Get a list of variables
            List<VariableAssignment<Number>> varAssgns = observation.getVariableAssignments();
            // To calculate p-values
            Map<Variable, List<Double>> varToRandomValues = getVarToRandomVarAssgns();
            for (VariableAssignment<Number> varAssgn : varAssgns) {
                Variable var = varAssgn.getVariable();
                List<Object> row = new ArrayList<>();
                data.add(row);
                row.add(var.getName());
                row.add(varAssgn.getAssignment());
                List<Double> randomValues = varToRandomValues.get(var);
                // If we cannot find a list of random values, we just assume it has the largest p-value
                if (randomValues == null) 
                    row.add(1.0d);
                else {
                    Double pvalue = calculatePValue(varAssgn, randomValues);
                    row.add(pvalue);
                }
            }
            // To calculate FDRs
            int totalPermutation = varToRandomValues.values().iterator().next().size();
            calculateFDRs(totalPermutation);
        }
        
        private double calculatePValue(VariableAssignment<Number> varAssgn,
                                       List<Double> randomValues) {
            Variable var = varAssgn.getVariable();
            double value = varAssgn.getAssignment().doubleValue();
            double pvalue = 1.0d;
            if (var instanceof ContinuousVariable) {
                ContinuousVariable cVar = (ContinuousVariable) var;
                if (cVar.getDistributionType() == DistributionType.ONE_SIDED) {
                    pvalue = PlugInUtilities.calculateNominalPValue(value, 
                                                                    randomValues, 
                                                                    "right");
                }
                else { // What we should do if this is two-sides
                    double mean = MathUtilities.calculateMean(randomValues);
                    if (value > mean)
                        pvalue = PlugInUtilities.calculateNominalPValue(value, randomValues, "right");
                    else if (value < mean)
                        pvalue = PlugInUtilities.calculateNominalPValue(value, randomValues, "left");
                    // Otherwise pvalue = 1.0d;
                }
            }
            else { // Discrete variable
                if (var.getStates() == 3) { // Three states: 0, 1, 2
                    if (value < 1)
                        pvalue = PlugInUtilities.calculateNominalPValue(value, randomValues, "left");
                    else if (value > 1)
                        pvalue = PlugInUtilities.calculateNominalPValue(value, randomValues, "right");
                }
                else if (var.getStates() == 2) { // Two states: 0, 1
                    if (value > 0)
                        pvalue = PlugInUtilities.calculateNominalPValue(value, randomValues, "right");
                }
                // Don't support other states
            }
            return pvalue;
        }
        
        private Map<Variable, List<Double>> getVarToRandomVarAssgns() {
            Map<Variable, List<Double>> varToAssgns = new HashMap<>();
            List<Observation<Number>> randomObservations = results.getRandomObservations();
            for (Observation<Number> obs : randomObservations) {
                Map<Variable, Number> varToAssgn = obs.getVariableToAssignment();
                for (Variable var : varToAssgn.keySet()) {
                    Number value = varToAssgn.get(var);
                    List<Double> values = varToAssgns.get(var);
                    if (values == null) {
                        values = new ArrayList<>();
                        varToAssgns.put(var, values);
                    }
                    values.add(value.doubleValue());
                }
            }
            return varToAssgns;
        }
    }
    
    /**
     * Customized table for showing observations and infernece results.
     * @author gwu
     *
     */
    private abstract class SampleModel extends AbstractTableModel {
        private String[] headers = new String[] {
                "Name",
                "Value",
                "p-value",
                "FDR"
        };
        // For showing data
        protected List<List<Object>> data;
        // Selected sample
        protected String sample;
        // The first value to be displayed in data for each row
        // We can store some information in each row that doesn't
        // need to be displayed
        protected int startIndex;
        
        public SampleModel() {
            data = new ArrayList<>();
        }
        
        public void setSample(String sample) {
            this.sample = sample;
            resetData();
            fireTableDataChanged();
        }
        
        protected abstract void resetData();
        
        @Override
        public String getColumnName(int column) {
            return headers[column];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return String.class;
            return Double.class;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return headers.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            // This is weird: in theory, we should not check this.
            // But sometime, we just get exception
            if (rowIndex >= data.size())
                return null;
            List<Object> row = data.get(rowIndex);
            if (columnIndex + startIndex < row.size())
                return row.get(columnIndex + startIndex);
            return null;
        }

        protected void calculateFDRs(int totalPermutation) {
            // Sort data based on p-values
            Collections.sort(data, new Comparator<List<Object>>() {
                public int compare(List<Object> row1, List<Object> row2) {
                    Double pvalue1 = (Double) row1.get(row1.size() - 1);
                    Double pvalue2 = (Double) row2.get(row2.size() - 1);
                    return pvalue1.compareTo(pvalue2);
                }
            }); 
            // Get a list of p-values for FDRs calculation
            List<Double> pvalues = new ArrayList<>();
            for (List<Object> row : data) {
                Double pvalue = (Double) row.get(row.size() - 1);
                if (pvalue.equals(0.0d)) 
                    pvalue = 1.0d / (totalPermutation + 1); // Use the closest double value for a conservative calculation
                pvalues.add(pvalue);
            }
            List<Double> fdrs = MathUtilities.calculateFDRWithBenjaminiHochberg(pvalues);
            for (int i = 0; i < fdrs.size(); i++) {
                Double fdr = fdrs.get(i);
                List<Object> row = data.get(i);
                row.add(fdr);
            }
        }
        
    }
    
}
