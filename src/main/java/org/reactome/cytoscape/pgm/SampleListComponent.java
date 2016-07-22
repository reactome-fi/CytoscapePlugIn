/*
 * Created on Dec 2, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.gk.model.ReactomeJavaConstants;
import org.osgi.framework.BundleContext;
import org.reactome.cytoscape.service.AnimationPlayer;
import org.reactome.cytoscape.service.AnimationPlayerControl;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
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
public class SampleListComponent extends JPanel implements CytoPanelComponent, Selectable {
    public static final String TITLE = "Sample List";
    // GUIs
    protected JComboBox<String> sampleBox;
    private JPanel typePane;
    private JLabel typeBox;
    protected JTable observationTable;
    protected JTable inferenceTable;
    protected JRadioButton highlightViewBtn;
    // Data to be displayed
    private Map<String, String> sampleToType;
    // Cached information for display
    private FactorGraphInferenceResults results;
    // For synchronization
    private GeneToPathwayEntityHandler observationTableHandler;
    protected Selectable observationVarSelectionHandler;
    private InferenceTableSelectionHandler selectionHandler;
    private PathwayHighlightControlPanel highlightControlPane;
    private double minIPA;
    private double maxIPA;
    // A flag to block row selection sync
    protected boolean blockRowSelectionSync;
    
    public SampleListComponent() {
        init();
    }
    
    private void init() {
        initGUIs();
        PlugInUtilities.registerCytoPanelComponent(this);
        // Most likely SessionAboutToBeLoadedListener should be used in 3.1.0.
        SessionLoadedListener sessionListener = new SessionLoadedListener() {
            
            @Override
            public void handleEvent(SessionLoadedEvent e) {
                getParent().remove(SampleListComponent.this);
            }
        };
        BundleContext context = PlugInObjectManager.getManager().getBundleContext();
        context.registerService(SessionLoadedListener.class.getName(),
                                sessionListener, 
                                null);
        synchronizeSampleSelection();
    }
    
    /**
     * TODO: The implementation of this method may cause a memory leak. Need to remove the registration
     * of this object after it is closed.
     */
    private void synchronizeSampleSelection() {
        // Register this as a sample selection listener if needed
        final SelectionMediator mediator = FactorGraphRegistry.getRegistry().getSampleSelectionMediator();
        List<?> selectables = mediator.getSelectables();
        if (selectables == null || !selectables.contains(this))
            mediator.addSelectable(this);
        sampleBox.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    mediator.fireSelectionEvent(SampleListComponent.this);
                }
            }
        });
    }

    private void initGUIs() {
        // Set up GUIs
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEtchedBorder());
        initNorthPane();
        initCenterPane();
    }

    private void initCenterPane() {
        // Use JTabbedPane for showing observations and inference results
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.BOTTOM);
        
        // Set up the table for inference
        // Want to have a checkbox to color pathway diagram
        JPanel inferencePane = createInferencePane();
        tabbedPane.addTab("Inference", inferencePane);
        
        // Set up the table for observation
        SampleTableModel observationModel = createObservationTableModel();
        TableRowSorter<TableModel> observationSorter = new TableRowSorter<TableModel>(observationModel);
        observationTable = new JTable(observationModel);
        observationTable.setRowSorter(observationSorter);
        tabbedPane.addTab("Observation", new JScrollPane(observationTable));
        
        observationTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    handleObservationTableSelection();
                }
            }
        });
        enableViewSelectionSyn();
        
        // Synchronize variable selection across the whole application
        observationVarSelectionHandler = new GeneLevelSelectionHandler();
        ((GeneLevelSelectionHandler)observationVarSelectionHandler).setGeneLevelTable(observationTable);
        PlugInObjectManager.getManager().getObservationVarSelectionMediator().addSelectable(observationVarSelectionHandler);
        
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    protected void enableViewSelectionSyn() {
        selectionHandler = new InferenceTableSelectionHandler();
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.addSelectable(selectionHandler);
        
        observationTableHandler = new GeneToPathwayEntityHandler();
        observationTableHandler.setObservationTable(observationTable);
        PlugInObjectManager.getManager().getDBIdSelectionMediator().addSelectable(observationTableHandler);
    }

    protected SampleTableModel createObservationTableModel() {
        SampleTableModel observationModel = new ObservationTableModel();
        return observationModel;
    }

    // The following two methods are used for synchronizing sample selection with
    // other components showing samples.
    /**
     * Select a sample from selection.
     */
    @Override
    public void setSelection(List selection) {
        if (selection == null || selection.size() == 0)
            return;
        Object sample = selection.get(selection.size() - 1); // Used the last selected object
        sampleBox.setSelectedItem(sample);
    }

    /**
     * Get the displayed sample in this component.
     */
    @Override
    public List getSelection() {
        List<String> samples = new ArrayList<>();
        String sample = (String) sampleBox.getSelectedItem();
        if (sample != null)
            samples.add(sample);
        return samples;
    }

    private JPanel createInferencePane() {
        JPanel inferencePane = new JPanel();
        inferencePane.setBorder(BorderFactory.createEtchedBorder());
        inferencePane.setLayout(new BorderLayout());
        SampleTableModel inferenceModel = createInferenceTableModel();
        TableRowSorter<TableModel> inferenceSorter = new TableRowSorter<TableModel>(inferenceModel);
        inferenceTable = new JTable(inferenceModel);
        inferenceTable.setRowSorter(inferenceSorter);
        inferencePane.add(new JScrollPane(inferenceTable), BorderLayout.CENTER);
        highlightViewBtn = new JRadioButton("Highlight pathway for sample");
        highlightViewBtn.setSelected(false); // Default as false
        registerRadioButton();
        highlightViewBtn.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    handleViewBtnSelectionEvent();
            }
        });
        
        highlightViewBtn.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        inferencePane.add(highlightViewBtn, BorderLayout.SOUTH);
        inferenceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    handleInferenceTableSelection();
            }
        });
        return inferencePane;
    }
    
    protected void handleViewBtnSelectionEvent() {
        highlightViewFromSample();
    }

    protected void registerRadioButton() {
        PlugInObjectManager.getManager().registerRadioButton("HighlightPathway",
                                                             highlightViewBtn);
    }
    
    protected void handleInferenceTableSelection() {
        if (blockRowSelectionSync)
            return;
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.fireSelectionEvent(selectionHandler);
    }
    
    protected void handleObservationTableSelection() {
        if (blockRowSelectionSync)
            return;
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.fireSelectionEvent(observationTableHandler);
        mediator = PlugInObjectManager.getManager().getObservationVarSelectionMediator();
        mediator.fireSelectionEvent(observationVarSelectionHandler);
    }

    protected SampleTableModel createInferenceTableModel() {
        SampleTableModel inferenceModel = new InferenceTableModel();
        return inferenceModel;
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
        // For performing animation
        AnimationPlayerControl animiationControl = new AnimationPlayerControl();
        SampleListAnimationPlayer player = new SampleListAnimationPlayer();
        animiationControl.setPlayer(player);
        animiationControl.setInterval(500); // 0.5 second per sample
        samplePane.add(animiationControl);
        
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
                if (e.getStateChange() == ItemEvent.SELECTED)
                    handleSampleSelection();
            }
        });
        
        add(northPane, BorderLayout.NORTH);
    }

    private synchronized void handleSampleSelection() {
        String selectedSample = (String) sampleBox.getSelectedItem();
        if (sampleToType != null && sampleToType.size() > 0) {
            String type = sampleToType.get(selectedSample);
            if (type == null)
                typeBox.setText("");
            else
                typeBox.setText(type);
        }
        // Show observation
        // Make sure selection stuck
        blockRowSelectionSync = true;
        Set<String> observationSelectedValues = getSelectedValues(observationTable);
        Set<String> inferenceSelectedValues = getSelectedValues(inferenceTable);
        int[] inferenceSelectionRows = inferenceTable.getSelectedRows();
        SampleTableModel observationModel = (SampleTableModel) observationTable.getModel();
        observationModel.setSample(selectedSample);
        recoverTableSelections(observationTable, observationSelectedValues);
        SampleTableModel inferenceModel = (SampleTableModel) inferenceTable.getModel();
        inferenceModel.setSample(selectedSample);
        recoverTableSelections(inferenceTable, inferenceSelectedValues);
        highlightViewFromSample();
        blockRowSelectionSync = false;
    }
    
    private Set<String> getSelectedValues(JTable table) { 
        Set<String> values = new HashSet<>();
        int[] rows = table.getSelectedRows();
        if (rows != null && rows.length > 0) {
            for (int row : rows) {
                String value = table.getValueAt(row, 0).toString();
                values.add(value);
            }
        }
        return values;
    }
    
    private void recoverTableSelections(JTable table,
                                        Set<String> selectedValues) {
        if (selectedValues.size() == 0)
            return;
        table.clearSelection();
        int lastRow = -1;
        for (int i = 0; i < table.getRowCount(); i++) {
            String value = table.getValueAt(i, 0).toString();
            if (selectedValues.contains(value)) {
                table.addRowSelectionInterval(i, i);
                lastRow = i;
            }
        }
        if (lastRow > -1) {
            Rectangle rect = table.getCellRect(lastRow, 0, false);
            table.scrollRectToVisible(rect);
        }
    }
    
    protected void highlightViewFromSample() {
        if (!highlightViewBtn.isSelected() || highlightControlPane == null)
            return;
        InferenceTableModel model = (InferenceTableModel) inferenceTable.getModel();
        Map<String, Double> idToValue = model.getIdToValue();
        highlightControlPane.setIdToValue(idToValue);
        double[] minMax = highlightControlPane.calculateMinMaxValues(minIPA, maxIPA);
        highlightControlPane.resetMinMaxValues(minMax);
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
                                    PathwayEditor pathwayEditor,
                                    PathwayHighlightControlPanel highlightControlPane,
                                    double minIPA,
                                    double maxIPA) {
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
        this.highlightControlPane = highlightControlPane;
        this.minIPA = minIPA;
        this.maxIPA = maxIPA;
    }
    
    private List<String> getSampleNames(FactorGraphInferenceResults results) {
        List<String> sampleNames = new ArrayList<>();
        for (Observation<Number> obs : results.getObservations())
            sampleNames.add(obs.getName());
        Collections.sort(sampleNames);
        return sampleNames;
    }
    
    private class InferenceTableModel extends SampleTableModel {
        public InferenceTableModel() {
            startIndex = 1;
        }
        
        public List<Integer> getRowsForDBIds(Collection<Long> dbIds) {
            List<Integer> rtn = new ArrayList<>();
            int index = 0;
            for (List<Object> row : data) {
                Long dbId = (Long) row.get(0);
                if (dbIds.contains(dbId))
                    rtn.add(index);
                index ++;
            }
            return rtn;
        }
        
        public Map<String, Double> getIdToValue() {
            Map<String, Double> idToValue = new HashMap<>();
            for (List<Object> row : data) {
                String id = row.get(0).toString();
                Double value = (Double) row.get(startIndex + 1);
                idToValue.put(id, value);
            }
            return idToValue;
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
    
    /**
     * This small class is used to handle sample table selection.
     * @author gwu
     *
     */
    private class InferenceTableSelectionHandler implements Selectable {
        private List<Long> selectedIds;
        
        public InferenceTableSelectionHandler() {
            selectedIds = new ArrayList<>();
        }
        
        @Override
        public void setSelection(List selection) {
            ListSelectionModel selectionModel = inferenceTable.getSelectionModel();
            selectionModel.clearSelection();
            selectionModel.setValueIsAdjusting(true);
            int index = 0;
            InferenceTableModel inferenceModel = (InferenceTableModel) inferenceTable.getModel();
            List<Integer> rows = inferenceModel.getRowsForDBIds(selection);
            for (Integer modelRow : rows) {
                int viewRow = inferenceTable.convertRowIndexToView(modelRow);
                selectionModel.addSelectionInterval(viewRow, viewRow);
            }
            selectionModel.setValueIsAdjusting(false);
            // Need to scroll
            int selected = inferenceTable.getSelectedRow();
            if (selected > -1) {
                Rectangle rect = inferenceTable.getCellRect(selected, 0, false);
                inferenceTable.scrollRectToVisible(rect);
            }
        }

        @Override
        public List getSelection() {
            selectedIds.clear();
            InferenceTableModel model = (InferenceTableModel) inferenceTable.getModel();
            int[] selectedRows = inferenceTable.getSelectedRows();
            if (selectedRows != null) {
                for (int selectedRow : selectedRows) {
                    Long dbId = model.getDBId(inferenceTable.convertRowIndexToModel(selectedRow));
                    selectedIds.add(dbId);
                }
            }
            return selectedIds;
        }
    }
    
    protected class ObservationTableModel extends SampleTableModel {
        
        public ObservationTableModel() {
            startIndex = 0;
        }

        @Override
        protected void resetData() {
            data.clear();
            if (results == null || sample == null)
                return; // Cannot do anything here
            resetData(results.getObservations(),
                      results.getRandomObservations(),
                      null);
        }
        
        private boolean shouldAdd(Variable var,
                                  Set<String> targetGenes) {
            if (targetGenes == null)
                return true;
            // Check if this var is in the gene set
            String name = var.getName();
            int index = name.indexOf("_");
            String gene = name.substring(0, index);
            return targetGenes.contains(gene);
        }
        
        protected void resetData(List<Observation<Number>> observations,
                                 List<Observation<Number>> randomObservations,
                                 Set<String> targetGenes) {
            // Extract observation for this sample
            Observation<Number> observation = null;
            for (Observation<Number> obs : observations) {
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
            Map<Variable, List<Double>> varToRandomValues = getVarToRandomVarAssgns(randomObservations,
                                                                                    targetGenes);
            for (VariableAssignment<Number> varAssgn : varAssgns) {
                Variable var = varAssgn.getVariable();
                if (!shouldAdd(var, targetGenes))
                    continue;
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
                    else
                        pvalue = PlugInUtilities.calculateNominalPValue(value, randomValues, "left");
                }
            }
            else { // Discrete variable
                if (var.getStates() == 3) { // Three states: 0, 1, 2
                    if (value > 1)
                        pvalue = PlugInUtilities.calculateNominalPValue(value, randomValues, "right");
                    else
                        pvalue = PlugInUtilities.calculateNominalPValue(value, randomValues, "left");
                }
                else if (var.getStates() == 2) { // Two states: 0, 1
                    if (value > 0)
                        pvalue = PlugInUtilities.calculateNominalPValue(value, randomValues, "right");
                }
                // Don't support other states
            }
            return pvalue;
        }
        
        private Map<Variable, List<Double>> getVarToRandomVarAssgns(List<Observation<Number>> randomObservations,
                                                                    Set<String> targetGenes) {
            Map<Variable, List<Double>> varToAssgns = new HashMap<>();
            for (Observation<Number> obs : randomObservations) {
                Map<Variable, Number> varToAssgn = obs.getVariableToAssignment();
                for (Variable var : varToAssgn.keySet()) {
                    if (!shouldAdd(var, targetGenes))
                        continue; // Should not do anything
                    Number value = varToAssgn.get(var);
                    List<Double> values = varToAssgns.get(var);
                    if (values == null) {
                        values = new ArrayList<>();
                        varToAssgns.put(var, values);
                    }
                    values.add(value.doubleValue());
                }
            }
            // Need to sort for p-value calculation
            for (Variable var : varToAssgns.keySet()) {
                List<Double> values = varToAssgns.get(var);
                Collections.sort(values);
            }
            return varToAssgns;
        }
    }
    
    /**
     * Customized table for showing observations and infernece results.
     * @author gwu
     *
     */
    protected abstract class SampleTableModel extends AbstractTableModel {
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
        
        public SampleTableModel() {
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
    
    private class SampleListAnimationPlayer implements AnimationPlayer {
        
        public SampleListAnimationPlayer() {
        }

        @Override
        public void forward() {
            int selectedIndex = sampleBox.getSelectedIndex();
            if (selectedIndex == sampleBox.getItemCount() - 1)
                selectedIndex = -1;
            sampleBox.setSelectedIndex(selectedIndex + 1);
        }

        @Override
        public void backward() {
            int selectedIndex = sampleBox.getSelectedIndex();
            if (selectedIndex == 0)
                selectedIndex = sampleBox.getItemCount();
            sampleBox.setSelectedIndex(selectedIndex - 1);
        }
        
    }
    
}
