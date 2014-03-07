/*
 * Created on Mar 5, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.reactome.pgm.PGMFactor;
import org.reactome.pgm.PGMVariable;

/**
 * This customized JDialg is used to show factor graph values (aka factor
 * graph functions).
 * @author gwu
 *
 */
public class FactorValuesDialog extends JDialog {
    private JTable table;
    private JTextArea textLabel;
    
    /**
     * @param owner
     */
    public FactorValuesDialog(Frame owner) {
        super(owner);
        init();
    }
    
    private void init() {
        setTitle("Factor Values");
        
        JPanel labelPane = new JPanel();
        labelPane.setLayout(new BorderLayout());
        labelPane.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        textLabel = new JTextArea();
        textLabel.setBackground(labelPane.getBackground());
        textLabel.setWrapStyleWord(true);
        textLabel.setLineWrap(true);
        textLabel.setEditable(false);
        Font font = textLabel.getFont();
        font = font.deriveFont(Font.BOLD);
        textLabel.setFont(font);
        labelPane.add(textLabel, BorderLayout.CENTER);
        getContentPane().add(labelPane, BorderLayout.NORTH);
        
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
        
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        
        JPanel controlPane = new JPanel();
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        controlPane.add(closeBtn);
        getContentPane().add(controlPane, BorderLayout.SOUTH);
    }
    
    /**
     * The client should call this method to set the values for display.
     * @param factor
     */
    public void setFactor(PGMFactor factor) {
        textLabel.setText("Values for Factor \"" + factor.getLabel() + "\"");
        FactorValueTableModel model = (FactorValueTableModel) table.getModel();
        model.setFactor(factor);
    }
    
    private class FactorValueTableModel extends AbstractTableModel {
        private PGMFactor factor;
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
            List<PGMVariable> variables = factor.getVariables();
            if (column > variables.size())
                return "Value";
            return variables.get(column - 1).getLabel();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        public void setFactor(PGMFactor factor) {
            this.factor = factor;
            int rowCount = getRowCount();
            int colCount = getColumnCount();
            values = new String[rowCount][colCount];
            
            int index = 0;
            List<PGMVariable> variables = factor.getVariables();
            List<Double> factorValues = factor.getValues();
            maxState = variables.get(0).getStates() - 1;
            
            List<Integer> states = new ArrayList<Integer>(variables.size());
            for (PGMVariable variable : variables) {
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
                                  List<Double> factorValues) {
            values[row] = new String[states.size() + 2];
            values[row][0] = row + "";
            int index = 1;
            for (Integer state : states) {
                values[row][index] = state + "";
                index ++;
            }
            values[row][states.size() + 1] = factorValues.get(row) + "";
        }

        @Override
        public int getRowCount() {
            if (factor == null)
                return 0;
            return factor.getValues().size(); 
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
