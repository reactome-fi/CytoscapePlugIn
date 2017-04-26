/*
 * Created on Apr 25, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

import org.reactome.booleannetwork.Attractor;
import org.reactome.booleannetwork.BooleanNetwork;
import org.reactome.booleannetwork.BooleanNetworkUtilities;
import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.cytoscape.bn.BooleanNetworkSamplePane.EntityType;
import org.reactome.cytoscape.util.PlugInUtilities;

public class SimulationTableModel extends AbstractTableModel implements VariableTableModelInterface {
    private List<String> tableHeaders;
    private List<List<Object>> values;
    private String simulationName;
    
    public SimulationTableModel() {
        tableHeaders = new ArrayList<>();
        // They should be there always
        tableHeaders.add("Entity");
        tableHeaders.add("Type");
        tableHeaders.add("Initial");
        values = new ArrayList<>();
    }
    
    public String getSimulationName() {
        return simulationName;
    }

    public void setSimulationName(String simulationName) {
        this.simulationName = simulationName;
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
    
    public List<BooleanVariable> getVariables() {
        List<BooleanVariable> variables = new ArrayList<>();
        for (List<Object> row : values)
            variables.add((BooleanVariable) row.get(0));
        return variables;
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
            rowValues.add(PlugInUtilities.getBooleanDefaultValue(var, defaultValue));
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