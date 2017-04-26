/*
 * Created on Apr 25, 2017
 *
 */
package org.reactome.cytoscape.bn;

import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.cytoscape.bn.BooleanNetworkSamplePane.EntityType;

/**
 * This customized JPanel is used to show comparison results between two simulation.
 * @author gwu
 *
 */
public class SimulationComparisonPane extends VariableCytoPaneComponent {
    
    public SimulationComparisonPane(String title) {
        super(title);
    }
    
    protected void modifyContentPane() {
        super.modifyContentPane();
        // Re-create control tool bars
        for (int i = 0; i < controlToolBar.getComponentCount(); i++) {
            controlToolBar.remove(i);
        }
        controlToolBar.add(closeGlue);
        createHighlightViewBtn();
        controlToolBar.add(hiliteDiagramBtn);
        controlToolBar.add(closeBtn);
    }

    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new ComparisonTableModel();
    }
    
    public void setSimulations(SimulationTableModel sim1,
                               SimulationTableModel sim2) {
        ComparisonTableModel model = (ComparisonTableModel) contentTable.getModel();
        model.setSimulations(sim1, sim2);
        // Highlight pathway diagrams based on ratio
        hilitePathway(model.getColumnCount() - 1);
    }
    
    @Override
    protected void reHilitePathway() {
        // Just highlite based on the last column
        hilitePathway(contentTable.getColumnCount() - 1);
    }
    
    private class ComparisonTableModel extends VariableTableModel implements VariableTableModelInterface {
        
        public ComparisonTableModel() {
        }
        
        public void setSimulations(SimulationTableModel sim1,
                                   SimulationTableModel sim2) {
            // Set up columns
            setUpColumnNames(sim1, sim2);
            // add values
            addValues(sim1, sim2);
            fireTableStructureChanged();
        }

        @Override
        protected boolean validateValue(Number number) {
            String text = number.toString();
            if (text.equals("NaN") || text.contains("Inf"))
                return false;
            return super.validateValue(number);
        }

        private void addValues(SimulationTableModel sim1,
                               SimulationTableModel sim2) {
            // Since both modules are sorted based on BooleanVariables, the following
            // should be safe
            tableData.clear();
            int startIndex = 5;
            for (int i = 0; i < sim1.getRowCount(); i++) {
                Object[] row = new Object[columnHeaders.length];
                tableData.add(row);
                // Entity
                row[0] = sim1.getValueAt(i, 0);
                // Type
                row[1] = sim1.getValueAt(i, 1);
                row[2] = sim2.getValueAt(i, 1);
                // Initial values
                row[3] = sim1.getValueAt(i, 2);
                row[4] = sim2.getValueAt(i, 2);
                startIndex = 5;
                for (int j = 3; j < sim1.getColumnCount(); j++) {
                    row[startIndex ++] = sim1.getValueAt(i, j);
                }
                for (int j = 3; j < sim2.getColumnCount(); j++) {
                    row[startIndex ++] = sim2.getValueAt(i, j);
                }
                Number value1 = (Number) sim1.getValueAt(i, sim1.getColumnCount() - 1);
                Number value2 = (Number) sim2.getValueAt(i, sim2.getColumnCount() - 1);
                double ratio = value1.doubleValue() / value2.doubleValue();
                row[startIndex] = ratio;
            }
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return BooleanVariable.class;
            else if (columnIndex < 3)
                return EntityType.class;
            else 
                return Number.class;
        }

        private void setUpColumnNames(SimulationTableModel sim1, 
                                      SimulationTableModel sim2) {
            int cols = sim1.getColumnCount() + sim2.getColumnCount(); // Shared entity column
            columnHeaders = new String[cols];
            int index = 0;
            columnHeaders[index ++] = "Entity";
            columnHeaders[index ++] = sim1.getSimulationName() + ":Type";
            columnHeaders[index ++] = sim2.getSimulationName() + ":Type";
            columnHeaders[index ++] = sim1.getSimulationName() + ":Initial";
            columnHeaders[index ++] = sim2.getSimulationName() + ":Initial";
            for (int i = 3; i < sim1.getColumnCount(); i++) {
                columnHeaders[index ++] = sim1.getSimulationName() + ":" + sim1.getColumnName(i);
            }
            for (int i = 3; i < sim2.getColumnCount(); i++) {
                columnHeaders[index ++] = sim2.getSimulationName() + ":" + sim2.getColumnName(i);
            }
            // Last column is on ratio
            columnHeaders[index] = "AttractorRatio";
        }
    }
    
}
