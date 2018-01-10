/*
 * Created on Apr 25, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;

import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.booleannetwork.SimulationComparator;
import org.reactome.cytoscape.bn.SimulationTableModel.EntityType;
import org.reactome.cytoscape.bn.SimulationTableModel.ModificationType;

/**
 * This customized JPanel is used to show comparison results between two simulation.
 * @author gwu
 *
 */
public class SimulationComparisonPane extends VariableCytoPaneComponent {
    // Provide a summary
    private JLabel summaryLabel;
    
    public SimulationComparisonPane(String title) {
        super(title);
    }
    
    protected void modifyContentPane() {
        super.modifyContentPane();
        // Re-create control tool bars
        for (int i = 0; i < controlToolBar.getComponentCount(); i++) {
            controlToolBar.remove(i);
        }
        summaryLabel = new JLabel();
        controlToolBar.add(summaryLabel);
        controlToolBar.add(closeGlue);
        createHighlightViewBtn();
        controlToolBar.add(hiliteDiagramBtn);
        controlToolBar.add(closeBtn);
    }

    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new ComparisonTableModel();
    }
    
    public void setSimulations(SimulationTableModel perturbed,
                               SimulationTableModel reference) {
        ComparisonTableModel model = (ComparisonTableModel) contentTable.getModel();
        model.setSimulations(perturbed, reference);
        // Highlight pathway diagrams based on ratio
        hilitePathway(model.getColumnCount() - 1);
        showSummary(model);
    }
    
    private void showSummary(ComparisonTableModel model) {
        // Get the total output variables
        int counter = 0;
        double sum = 0.0d;
        for (int i = 0; i < model.getRowCount(); i++) {
            BooleanVariable var = (BooleanVariable) model.getValueAt(i, 0);
            String isOutput = var.getProperty("output");
            if (isOutput != null && isOutput.equals(Boolean.TRUE + "")) {
                counter ++;
                Number value = (Number) model.getValueAt(i, model.getColumnCount() - 1);
                sum += Math.abs(value.doubleValue());
            }
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Summary: ");
        builder.append(counter).append(" outputs; ");
        builder.append("Sum of output absolute difference: ");
        builder.append(String.format("%.2e", sum)).append("; ");
        double mean = sum / counter;
        builder.append("Average: ").append(String.format("%.4e", mean)).append(".");
        summaryLabel.setText(builder.toString());
    }
    
    @Override
    protected void reHilitePathway() {
        // Just highlight based on the last column
        hilitePathway(contentTable.getColumnCount() - 1);
    }
    
    private class ComparisonTableModel extends VariableTableModel {
        
        public ComparisonTableModel() {
        }
        
        public void setSimulations(SimulationTableModel perturbed,
                                   SimulationTableModel reference) {
            // Set up columns
            setUpColumnNames(perturbed, reference);
            // add values
            addValues(perturbed, reference);
            fireTableStructureChanged();
        }

        @Override
        protected boolean validateValue(Number number) {
            String text = number.toString();
            if (text.equals("NaN") || text.contains("Inf"))
                return false;
            return super.validateValue(number);
        }
        
        private void addValues(SimulationTableModel perturbed,
                               SimulationTableModel reference) {
            // Since both modules are sorted based on BooleanVariables, the following
            // should be safe
            tableData.clear();
            SimulationComparator comparator = new SimulationComparator();
            String propKey = "reactomeId";
            comparator.setVarPropertyKey(propKey);
            // varToDiff is keyed based on reference variables
            Map<BooleanVariable, Double> varToDiff = comparator.calculateDiff(perturbed.getSimulationResults(), 
                                                                              reference.getSimulationResults(),
                                                                              20, // These three parameters are arbitrary
                                                                              0.01d,
                                                                              1000);
            // Have to base var in the map in case two different networks are used (e.g. network
            // has been modified because of set members being chosen)
            // We want to show variables in the original table models only. However varToDiff may have
            // too many variables.
            List<BooleanVariable> pertVars = perturbed.getVariables();
            Map<String, BooleanVariable> pertPropToVar = new HashMap<>();
            pertVars.forEach(var -> pertPropToVar.put(var.getProperty(propKey), var));
            
            List<BooleanVariable> refVars = reference.getVariables();
            refVars.forEach(refVar -> {
                Double diff = varToDiff.get(refVar);
                if (diff == null)
                    return;
                String prop = refVar.getProperty(propKey);
                BooleanVariable pertVar = pertPropToVar.get(prop);
                if (pertVar == null)
                    return;

                Object[] row = new Object[columnHeaders.length];
                tableData.add(row);
                // Entity
                row[0] = refVar;
                // Type
                row[1] = perturbed.getValueAt(pertVar, 1);
                row[2] = reference.getValueAt(refVar, 1);
                // Initial values
                row[3] = perturbed.getValueAt(pertVar, 2);
                row[4] = reference.getValueAt(refVar, 2);
                
                // Type
                row[5] = perturbed.getValueAt(pertVar, 3);
                row[6] = reference.getValueAt(refVar, 3);
                // Initial values
                row[7] = perturbed.getValueAt(pertVar, 4);
                row[8] = reference.getValueAt(refVar, 4);
                
                row[9] = diff;
            });
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return BooleanVariable.class;
            else if (columnIndex < 3)
                return EntityType.class;
            else if (columnIndex < 5)
                return ModificationType.class;
            else 
                return Number.class;
        }

        private void setUpColumnNames(SimulationTableModel sim1, 
                                      SimulationTableModel sim2) {
            // Don't show two modification columns for the time being
            int cols = 10; 
            columnHeaders = new String[cols];
            int index = 0;
            columnHeaders[index ++] = "Entity";
            columnHeaders[index ++] = sim1.getSimulationName() + ":Type";
            columnHeaders[index ++] = sim2.getSimulationName() + ":Type";
            columnHeaders[index ++] = sim1.getSimulationName() + ":Initial";
            columnHeaders[index ++] = sim2.getSimulationName() + ":Initial";
            columnHeaders[index ++] = sim1.getSimulationName() + ":Modification";
            columnHeaders[index ++] = sim2.getSimulationName() + ":Modification";
            columnHeaders[index ++] = sim1.getSimulationName() + ":Strength";
            columnHeaders[index ++] = sim2.getSimulationName() + ":Strength";
            // Last column is about relative difference
            columnHeaders[index] = "RelativeDifference";
        }
    }
    
}
