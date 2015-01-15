/*
 * Created on Mar 5, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.reactome.factorgraph.Factor;
import org.reactome.factorgraph.Variable;

/**
 * This customized JDialg is used to show factor graph values (aka factor
 * graph functions).
 * @author gwu
 *
 */
public class FactorValuesDialog extends PGMNodeValuesDialog {
    private JTable table;
    private JLabel textLabel;
    
    /**
     * @param owner
     */
    public FactorValuesDialog(Frame owner) {
        super(owner);
        setTitle("Factor Values");
    }
    
    @Override
    protected JComponent createContentPane() {
        textLabel = new JLabel("");
        textLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        getContentPane().add(textLabel, BorderLayout.NORTH);
        
        table = new JTable();
        FactorValueTableModel model = new FactorValueTableModel();
        table.setModel(model);
        // Want to sort based on values
        TableRowSorter<FactorValueTableModel> sorter = new TableRowSorter<FactorValueTableModel>() {

            @Override
            public Comparator<?> getComparator(int column) {
                if (column == 0) {
                    Comparator<String> comparator = new Comparator<String>() {
                        public int compare(String value1, String value2) {
                            return new Integer(value1).compareTo(new Integer(value2));
                        }
                    };
                    return comparator;
                }
                if (column == table.getColumnCount() - 1) {
                    Comparator<String> comparator = new Comparator<String>() {
                        public int compare(String value1, String value2) {
                            return new Double(value1).compareTo(new Double(value2));
                        }
                    };
                    return comparator;
                }
                return super.getComparator(column);
            }
            
        };
        sorter.setModel(model);
        table.setRowSorter(sorter);
        return new JScrollPane(table);
    }
    
    /**
     * The client should call this method to set the values for display.
     * @param factor
     */
    public void setFactor(Factor factor) {
        textLabel.setText("<html><u><b>Values for Factor \"" + factor.getName() + "\"</b></u></html>\"");
        FactorValueTableModel model = (FactorValueTableModel) table.getModel();
        model.setFactor((Factor)factor);
    }
    
    private class FactorValueTableModel extends AbstractTableModel {
        private Factor factor;
        // For quick display
        private String[][] values;
        // The maximum state of a variable: this should be the same
        // for all variable
        private int maxState;
        
        public FactorValueTableModel() {
        }
        
        @Override
        public String getColumnName(int column) {
            if (factor == null)
                return super.getColumnName(column);
            if (column == 0)
                return "Index";
            List<Variable> variables = factor.getVariables();
            if (column > variables.size())
                return "Value";
            return variables.get(column - 1).getName();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public void setFactor(Factor factor) {
            this.factor = factor;
            int rowCount = getRowCount();
            int colCount = getColumnCount();
            values = new String[rowCount][colCount];
            
            int index = 0;
            List<Variable> variables = factor.getVariables();
            double[] factorValues = factor.getValues();
            maxState = variables.get(0).getStates() - 1;
            
            List<Integer> states = new ArrayList<Integer>(variables.size());
            for (Variable variable : variables) {
                states.add(0);
            }
            
            index = 0;
            setRowValues(states, index, factorValues);
            while (!isDone(states)) {
                for (int i = 0; i < states.size(); i++) {
                    Integer state = states.get(i);
                    if (state < maxState) {
                        states.set(i, ++state);
                        for (int j = i - 1; j >= 0; j--) {
                            states.set(j, 0); // Just reset
                        }
                        break;
                    }
                }
                index ++;
                setRowValues(states, index, factorValues);
            }
            
            fireTableStructureChanged();
        }
        
        private void setRowValues(List<Integer> states,
                                  int row,
                                  double[] factorValues) {
            values[row] = new String[states.size() + 2];
            values[row][0] = row + "";
            int index = 1;
            for (Integer state : states) {
                values[row][index] = state + "";
                index ++;
            }
            values[row][states.size() + 1] = factorValues[row] + "";
        }

        @Override
        public int getRowCount() {
            if (factor == null)
                return 0;
            return factor.getValues().length; 
        }

        @Override
        public int getColumnCount() {
            if (factor == null)
                return 0;
            // The first colum is index and the last column is value
            return factor.getVariables().size() + 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return values[rowIndex][columnIndex];
        }
        
        private boolean isDone(List<Integer> states) {
            for (Integer state : states) {
                if (state < maxState)
                    return false;
            }
            return true;
        }
        
    }
    
}
