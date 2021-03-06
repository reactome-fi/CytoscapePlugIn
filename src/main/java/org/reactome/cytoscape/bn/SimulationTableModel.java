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
import org.reactome.booleannetwork.SimulationConfiguration;
import org.reactome.booleannetwork.SimulationResults;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.pathway.booleannetwork.BNPerturbationAnalyzer;
import org.reactome.pathway.booleannetwork.BNPerturbationInjector;
import org.reactome.pathway.booleannetwork.ModificationType;

public class SimulationTableModel extends AbstractTableModel implements VariableTableModelInterface {
    private List<String> tableHeaders;
    private List<List<Object>> values;
    private String simulationName;
    private Double defaultValue;
    // For comparison
    private SimulationResults simulationResults;
    // For easy search based on BooleanVariable
    private Map<BooleanVariable, List<Object>> varToValues;
    
    public SimulationTableModel() {
        tableHeaders = new ArrayList<>();
        // They should be there always
        tableHeaders.add("Entity");
        tableHeaders.add("Type");
        tableHeaders.add("Initial");
        tableHeaders.add("Modification");
        tableHeaders.add("Strength");
        values = new ArrayList<>();
        varToValues = new HashMap<>();
    }
    
    public SimulationResults getSimulationResults() {
        return simulationResults;
    }

    public void setSimulationResults(SimulationResults simulationResults) {
        this.simulationResults = simulationResults;
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
        return simulationResults != null;
    }
    
    public SimulationConfiguration getConfiguration() {
        SimulationConfiguration configuration = new SimulationConfiguration();
        Map<BooleanVariable, Number> stimulation = getVarToValue(EntityType.Stimulation);
        configuration.setStimulation(stimulation);
        Map<BooleanVariable, Number> initial = getVarToValue(EntityType.Respondent);
        configuration.setInitial(initial);
        Map<BooleanVariable, Double> inhibition = getVarToModification(ModificationType.Inhibition);
        configuration.setInhibition(inhibition);
        Map<BooleanVariable, Double> activation = getVarToModification(ModificationType.Activation);
        configuration.setActivation(activation);
        configuration.setDefaultValue(this.defaultValue);
        return configuration;
    }
    
    private Map<BooleanVariable, Double> getVarToModification(ModificationType requiredType) {
        Map<BooleanVariable, Double> varToValue = new HashMap<>();
        for (List<Object> rowValues : values) {
            BooleanVariable var = (BooleanVariable) rowValues.get(0);
            ModificationType type = (ModificationType) rowValues.get(3);
            if (type != requiredType)
                continue;
            Double value = (Double) rowValues.get(4);
            varToValue.put(var, value);
        }
        return varToValue;
    }
    
    private Map<BooleanVariable, Number> getVarToValue(EntityType requiredType) {
        Map<BooleanVariable, Number> varToValue = new HashMap<>();
        for (List<Object> rowValues : values) {
            BooleanVariable var = (BooleanVariable) rowValues.get(0);
            EntityType type = (EntityType) rowValues.get(1);
            if (type != requiredType)
                continue;
            Number value = (Number) rowValues.get(2);
            varToValue.put(var, value);
        }
        return varToValue;
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
                                  Double defaultValue,
                                  Map<String, Double> proteinInhibition,
                                  Map<String, Double> proteinActivation) {
        this.defaultValue = defaultValue;
        List<BooleanVariable> variables = BooleanNetworkUtilities.getSortedVariables(network);
        // Get a set of reasonable initial values to start
        BNPerturbationAnalyzer helper = new BNPerturbationAnalyzer();
        Map<BooleanVariable, Number> varToInit1 = helper.createInitials(network, defaultValue);
        Map<BooleanVariable, Number> varToInit = varToInit1;
        values.clear();
        BNPerturbationInjector injectors = new BNPerturbationInjector();
        Map<BooleanVariable, Double> varToInhibition = new HashMap<>();
        Map<BooleanVariable, Double> varToActivation = new HashMap<>();
        injectors.inject(network,
                        proteinInhibition,
                        proteinActivation, 
                        varToInhibition, 
                        varToActivation);
        for (BooleanVariable var : variables) {
            String reactomeId = var.getProperty("reactomeId");
//            if (reactomeId == null || !displayedIds.contains(reactomeId))
//                continue;
            List<Object> rowValues = new ArrayList<>();
            values.add(rowValues);
            rowValues.add(var);
            rowValues.add(EntityType.Respondent);
            Number init = varToInit.get(var);
            if (init == null)
                rowValues.add(defaultValue);
            else
                rowValues.add(init);
            ModificationType mType = ModificationType.None;
            Double mValue = 0.0d; // Default
            if (varToInhibition.containsKey(var)) {
                mType = ModificationType.Inhibition;
                mValue = varToInhibition.get(var);
            }
            else if (varToActivation.containsKey(var)) { // If a var has both values, we will pick inhibition
                mType = ModificationType.Activation;
                mValue = varToActivation.get(var);
            }
            rowValues.add(mType);
            rowValues.add(mValue);
        }
        // For easy search
        varToValues.clear();
        values.forEach(row -> varToValues.put((BooleanVariable)row.get(0), row));
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
        switch (columnIndex) { 
            case 0 : return BooleanVariable.class;
            case 1 : return EntityType.class;
            case 2 : return Number.class;
            case 3 : return ModificationType.class;
            case 4 : return Double.class;
            default : return Number.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // If a simulation has been displayed, don't edit 
        if (tableHeaders.size() > 5)
            return false;
        // Only type and initial values can be edited
        if (columnIndex > 0 && columnIndex < 5)
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
    
    public Object getValueAt(BooleanVariable var, int columnIndex) {
        List<Object> rowValues = varToValues.get(var);
        if (rowValues == null)
            return null;
        if (columnIndex >= 0 && columnIndex < rowValues.size())
            return rowValues.get(columnIndex);
        return null;
    }
    
    enum EntityType {
        Stimulation,
        Respondent
    }
    
}