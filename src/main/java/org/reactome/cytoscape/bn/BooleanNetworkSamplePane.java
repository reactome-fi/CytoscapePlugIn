/*
 * Created on Apr 20, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

import org.gk.graphEditor.PathwayEditor;
import org.gk.graphEditor.Selectable;
import org.gk.graphEditor.SelectionMediator;
import org.gk.render.Node;
import org.reactome.booleannetwork.Attractor;
import org.reactome.booleannetwork.BooleanNetwork;
import org.reactome.booleannetwork.BooleanNetworkUtilities;
import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.booleannetwork.FuzzyLogicSimulator;
import org.reactome.booleannetwork.FuzzyLogicSimulator.ANDGateMode;
import org.reactome.cytoscape.util.PlugInObjectManager;

/**
 * This customized JPanel is used to set up initial values and then list simulation results.
 * network modeling.
 * @author gwu
 *
 */
public class BooleanNetworkSamplePane extends JPanel {
    private BooleanNetwork network;
    private PathwayEditor pathwayEditor;
    private JTable sampleTable;
    // To synchronize selection
    private VariableSelectionHandler selectionHandler;
    // For simulation
    private Double defaultValue = 1.0d; // Default is on
    
    /**
     * Default constructor.
     */
    public BooleanNetworkSamplePane() {
        init();
    }

    public Double getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Double defaultValue) {
        this.defaultValue = defaultValue;
    }

    private void init() {
        setLayout(new BorderLayout());
        
        sampleTable = createSampleTable();
        
        add(new JScrollPane(sampleTable), BorderLayout.CENTER);
        
        enableSelectionSync();
    }

    private JTable createSampleTable() {
        JTable table = new JTable();
        table.setDefaultRenderer(BooleanVariable.class, new DefaultTableCellRenderer() {

            @Override
            protected void setValue(Object value) {
                BooleanVariable var = (BooleanVariable) value;
                setText(var == null ? "" : var.getName());
            }
            
        });
        SampleTableModel model = new SampleTableModel();
        table.setModel(model);
        TableRowSorter<SampleTableModel> sorter = new TableRowSorter<SampleTableModel>(model);
        sorter.setComparator(0, new Comparator<BooleanVariable>() {
            public int compare(BooleanVariable var1, BooleanVariable var2) {
                return var1.getName().compareTo(var2.getName());
            }
        });
        sorter.setComparator(1, new Comparator<EntityType>() {
            public int compare(EntityType type1, EntityType type2) {
                return type1.toString().compareTo(type2.toString());
            }
        });
        table.setRowSorter(sorter);
        
        TableColumn typeColoumn = table.getColumnModel().getColumn(1);
        JComboBox<EntityType> typeEditor = new JComboBox<>();
        for (EntityType type : EntityType.values())
            typeEditor.addItem(type);
        typeColoumn.setCellEditor(new DefaultCellEditor(typeEditor));
        typeColoumn.setCellRenderer(new DefaultTableCellRenderer());
        
        TableColumn initCol = table.getColumnModel().getColumn(2);
        initCol.setCellEditor(new DefaultCellEditor(new JTextField()) {

            @Override
            public Object getCellEditorValue() {
                String text = super.getCellEditorValue().toString();
                return new Double(text);
            }
            
        });
        initCol.setCellRenderer(new DefaultTableCellRenderer());
        
        return table;
    }
    
    private void enableSelectionSync() {
        sampleTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                handleTableSelection();
            }
        });
        
        selectionHandler = new VariableSelectionHandler();
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.addSelectable(selectionHandler);
    }
    
    public void delete() {
        // Need a confirmation
        int reply = JOptionPane.showConfirmDialog(this,
                                                  "Are you sure you want to delete the selected simulation?",
                                                  "Delete?",
                                                  JOptionPane.OK_CANCEL_OPTION,
                                                  JOptionPane.WARNING_MESSAGE);
        if (reply != JOptionPane.OK_OPTION)
            return;
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        if (mediator.getSelectables() != null)
            mediator.getSelectables().remove(selectionHandler);
        
        getParent().remove(this);
    }
    
    private void handleTableSelection() {
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.fireSelectionEvent(selectionHandler);
    }
    
    public PathwayEditor getPathwayEditor() {
        return pathwayEditor;
    }

    public void setPathwayEditor(PathwayEditor pathwayEditor) {
        this.pathwayEditor = pathwayEditor;
    }

    public void setBooleanNetwork(BooleanNetwork network) {
        this.network = network;
        SampleTableModel model = (SampleTableModel) sampleTable.getModel();
        model.setBooleanNetwork(network,
                                getDisplayedIds(),
                                defaultValue);
    }
    
    private Set<String> getDisplayedIds() {
        Set<String> ids = new HashSet<>();
        if (pathwayEditor != null) {
            for (Object o : pathwayEditor.getRenderable().getComponents()) {
                if (o instanceof Node) {
                    Node node = (Node) o;
                    if (node.getReactomeId() != null)
                        ids.add(node.getReactomeId().toString());
                }
            }
        }
        return ids;
    }
    
    public boolean isSimulationPerformed() {
        SampleTableModel model = (SampleTableModel) sampleTable.getModel();
        return model.isSimulationPerformed();
    }
    
    public void simulate() {
        SampleTableModel model = (SampleTableModel) sampleTable.getModel();
        if (model.isSimulationPerformed()) {
            JOptionPane.showMessageDialog(this,
                                          "Simulation has been performed for this sample.\n" + 
                                           "Create another sample for new simulation.",
                                           "New Simulation",
                                           JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        model.commitValues();
        // There are other variables that need values
        for (BooleanVariable var : network.getVariables()) {
            if (var.getValue() == null)
                var.setValue(defaultValue); // Default
        }
        Map<String, Number> stimulation = model.getStimulation();
        
        FuzzyLogicSimulator simulator = new FuzzyLogicSimulator();
        simulator.setAndGateMode(ANDGateMode.PROD);
        simulator.simulate(network, stimulation);
        
        if (!simulator.isAttractorReached()) {
            JOptionPane.showMessageDialog(this,
                                          "Attractor cannot be reached!",
                                          "No Attractor",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Attractor attractor = simulator.getAttractor();
        model.addAttractor(attractor);
    }
    
    private class VariableSelectionHandler implements Selectable {
        private List<Long> selectedIds; // Use a single List object to save some GC
        
        public VariableSelectionHandler() {
            selectedIds = new ArrayList<>();
        }

        @Override
        public void setSelection(List selection) {
            ListSelectionModel selectionModel = sampleTable.getSelectionModel();
            selectionModel.clearSelection();
            selectionModel.setValueIsAdjusting(true);
            int index = 0;
            SampleTableModel inferenceModel = (SampleTableModel) sampleTable.getModel();
            List<Integer> rows = inferenceModel.getRowsForSelectedIds(selection);
            for (Integer modelRow : rows) {
                int viewRow = sampleTable.convertRowIndexToView(modelRow);
                selectionModel.addSelectionInterval(viewRow, viewRow);
            }
            selectionModel.setValueIsAdjusting(false);
            // Need to scroll
            int selected = sampleTable.getSelectedRow();
            if (selected > -1) {
                Rectangle rect = sampleTable.getCellRect(selected, 0, false);
                sampleTable.scrollRectToVisible(rect);
            }
        }

        @Override
        public List getSelection() {
            selectedIds.clear();
            SampleTableModel model = (SampleTableModel) sampleTable.getModel();
            int[] selectedRows = sampleTable.getSelectedRows();
            if (selectedRows != null) {
                for (int selectedRow : selectedRows) {
                    int modelRow = sampleTable.convertRowIndexToModel(selectedRow);
                    BooleanVariable var = (BooleanVariable) model.getValueAt(modelRow, 0);
                    String reactomeId = var.getProperty("reactomeId");
                    if (reactomeId != null)
                        selectedIds.add(new Long(reactomeId));
                }
            }
            return selectedIds;
        }
    }
    
    private class SampleTableModel extends AbstractTableModel {
        private List<String> tableHeaders;
        private List<List<Object>> values;
        
        public SampleTableModel() {
            tableHeaders = new ArrayList<>();
            // They should be there always
            tableHeaders.add("Entity");
            tableHeaders.add("Type");
            tableHeaders.add("Initial");
            values = new ArrayList<>();
        }
        
        public void addAttractor(Attractor attractor) {
            Map<BooleanVariable, List<Number>> varToValues = attractor.getVarToValues();
            List<Number> attractorValues = varToValues.values().iterator().next();
            int cycleSize = attractorValues.size();
            // Need to add table headers
            if (cycleSize == 1) 
                tableHeaders.add("Attractor");
            else {
                for (int i = 0; i < cycleSize; i++)
                    tableHeaders.add("Attractor(" + i + ")");
            }
            for (List<Object> row : values) {
                BooleanVariable var = (BooleanVariable) row.get(0);
                List<Number> varValues = varToValues.get(var);
                if (varValues == null)
                    continue;
                row.addAll(varValues);
            }
            // Need to fire table structure change since the number of column has been changed.
            // The side effect of this call is to disable editing, which is needed here.
            fireTableStructureChanged();
        }
        
        public boolean isSimulationPerformed() {
            return tableHeaders.size() > 3; // Show have attractor results displayed
        }
        
        public Map<String, Number> getStimulation() {
            Map<String, Number> stimulation = new HashMap<>();
            for (List<Object> rowValues : values) {
                BooleanVariable var = (BooleanVariable) rowValues.get(0);
                EntityType type = (EntityType) rowValues.get(1);
                if (type == EntityType.Respondent)
                    continue;
                Number init = (Number) rowValues.get(2);
                stimulation.put(var.getName(), init);
            }
            return stimulation;
        }
        
        /**
         * Commit values to displayed BooleanVariables.
         */
        public void commitValues() {
            for (List<Object> rowValues : values) {
                BooleanVariable var = (BooleanVariable) rowValues.get(0);
                Number init = (Number) rowValues.get(2);
                var.setValue(init);
            }
        }
        
        public List<Integer> getRowsForSelectedIds(List<Long> ids) {
            List<Integer> rtn = new ArrayList<>();
            for (int i = 0; i < values.size(); i++) {
                List<Object> rowValues = values.get(i);
                BooleanVariable var = (BooleanVariable) rowValues.get(0);
                String reactomeId = var.getProperty("reactomeId");
                if (reactomeId == null)
                    continue;
                if (ids.contains(new Long(reactomeId)))
                    rtn.add(i);
            }
            return rtn;
        }
        
        public void setBooleanNetwork(BooleanNetwork network,
                                      Set<String> displayedIds,
                                      Double defaultValue) {
            List<BooleanVariable> variables = BooleanNetworkUtilities.getSortedVariables(network);
            values.clear();
            for (BooleanVariable var : variables) {
                String reactomeId = var.getProperty("reactomeId");
                if (reactomeId == null || !displayedIds.contains(reactomeId))
                    continue;
                List<Object> rowValues = new ArrayList<>();
                values.add(rowValues);
                rowValues.add(var);
                rowValues.add(EntityType.Respondent);
                // Use default 1.0
                rowValues.add(defaultValue);
            }
            // Have to call fire data changed, not structure changed. Otherwise,
            // Editing cannot work!!!
            fireTableDataChanged();
        }
        
        @Override
        public int getRowCount() {
            return values.size();
        }

        @Override
        public int getColumnCount() {
            return tableHeaders.size();
        }

        @Override
        public String getColumnName(int column) {
            return tableHeaders.get(column);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return BooleanVariable.class;
            if (columnIndex == 1)
                return EntityType.class;
            return Number.class;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            // If a simulation has been displayed, don't edit 
            if (tableHeaders.size() > 3)
                return false;
            // Only type and initial values can be edited
            if (columnIndex == 1 || columnIndex == 2)
                return true;
            else
                return false;
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            List<Object> rowValues = values.get(rowIndex);
            rowValues.set(columnIndex, aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            List<Object> rowValues = values.get(rowIndex);
            if (columnIndex < rowValues.size())
                return rowValues.get(columnIndex);
            return null;
        }
    }
    
    private enum EntityType {
        Stimulation,
        Inhibition,
        Respondent
    }
    
}
