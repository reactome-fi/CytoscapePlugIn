/*
 * Created on Mar 10, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.reactome.cytoscape.service.PlotTablePanel;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * A customized JDialog for showing values (aka marginal properties) of a variable.
 * @author gwu
 *
 */
public class VariableValuesDialog extends PGMNodeValuesDialog {
    private JLabel textLabel;
    private JLabel state0Value;
    private JLabel state1Value;
    private JLabel state2Value;
    // To display posterior probabilities
    private JPanel posteriorPane;
    private PlotTablePanel posteriorValuePane;
    
    /**
     * @param owner
     */
    public VariableValuesDialog(Frame owner) {
        super(owner);
        setTitle("Variable Marginals");
    }
    
    @Override
    protected JComponent createContentPane() {
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        contentPane.setBorder(BorderFactory.createEtchedBorder());
        
        textLabel = new JLabel("Marginal Probabilities for Variable");
        textLabel.setHorizontalAlignment(JLabel.CENTER);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.weightx = 1.0d;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        contentPane.add(textLabel, constraints);
        
        constraints.gridwidth = 1;
        constraints.insets = new Insets(2, 2, 2, 2);
        state0Value = addStateLabels(contentPane, constraints, 0);
        state1Value = addStateLabels(contentPane, constraints, 1);
        state2Value = addStateLabels(contentPane, constraints, 2);
        
        createPosteriorPane();
        
        constraints.gridwidth = 2;
        constraints.weighty = 1.0d;
        constraints.gridy ++;
        constraints.gridx = 0;
        constraints.insets = new Insets(8, 4, 4, 4);
        constraints.fill = GridBagConstraints.BOTH;
        contentPane.add(posteriorPane, constraints);
        
        return contentPane;
    }  
    
    private void createPosteriorPane() {
        posteriorPane = new JPanel();
        posteriorPane.setLayout(new BorderLayout());
        
        JLabel label = new JLabel("Marginal probabilities in samples:");
        posteriorPane.add(label, BorderLayout.NORTH);
        
        posteriorValuePane = new PlotTablePanel("Probability", false);
        posteriorPane.add(posteriorValuePane, BorderLayout.CENTER);
        
        JTable posteriorTable = new JTable();
        PosteriorTableModel model = new PosteriorTableModel();
        posteriorTable.setModel(model);
        TableRowSorter<PosteriorTableModel> sorter = new TableRowSorter<PosteriorTableModel>() {

            @Override
            public Comparator<?> getComparator(int column) {
                if (column == 0)
                    return super.getComparator(0);
                // Compare based on double values
                Comparator<String> comparator = new Comparator<String>() {
                    public int compare(String value1, String value2) {
                        return new Double(value1).compareTo(new Double(value2));
                    }
                };
                return comparator;
            }
            
        };
        sorter.setModel(model);
        posteriorTable.setRowSorter(sorter);
        posteriorValuePane.setTable(posteriorTable);
    }
    
    public void setVariableValues(VariableInferenceResults varValues) {
        List<Double> values = varValues.getPriorValues();
        if (values == null || values.size() == 0) {
            textLabel.setText("<html><center><b><u>Unknown marginal probabilities for variable \"" + varValues.getVariable().getName() + "\".</u></b></center></html>");
            return;
        }
        textLabel.setText("<html><center><b><u>Marginal Probabilities for Variable \"" + varValues.getVariable().getName() + "\"</u></b></center></html>");
        List<JLabel> labels = new ArrayList<JLabel>();
        labels.add(state0Value);
        labels.add(state1Value);
        labels.add(state2Value);
        for (int i = 0; i < values.size(); i++) {
            Double value = values.get(i);
            JLabel label = labels.get(i);
            label.setText(PlugInUtilities.formatProbability(value));
        }
        // Display posterior probabilities if existing
        if (varValues.getPosteriorValues() == null || varValues.getPosteriorValues().size() == 0) {
            posteriorPane.setVisible(false);
        }
        else {
            posteriorPane.setVisible(true);
            PosteriorTableModel model = (PosteriorTableModel) posteriorValuePane.getTableModel();
            model.setData(varValues.getPosteriorValues());
        }
    }
    
    private JLabel addStateLabels(JPanel contentPane,
                                  GridBagConstraints constraints,
                                  int state) {
        JLabel label = new JLabel("State " + state + ": ");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        constraints.gridy ++;
        constraints.gridx = 0;
        contentPane.add(label, constraints);
        JLabel valueLabel = new JLabel("");
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        constraints.gridx = 1;
        contentPane.add(valueLabel, constraints);
        return valueLabel;
    }
    
    /**
     * This is used to show values from observed samples.
     * @author gwu
     *
     */
    private class PosteriorTableModel extends AbstractTableModel {
        
        private String[] colNames = new String[]{"Sample",
                "State 0", 
                "State 1", 
                "State 2"};
        
        private List<String> sampleList;
        private List<List<String>> valuesList;
        
        public PosteriorTableModel() {
        }

        @Override
        public int getRowCount() {
            if (sampleList == null)
                return 0;
            return sampleList.size();
        }
        
        @Override
        public int getColumnCount() {
            return colNames.length;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public String getColumnName(int column) {
            return colNames[column];
        }
        
        public void setData(Map<String, List<Double>> sampleToValues) {
            if (sampleToValues == null)
                return;
            sampleList = new ArrayList<String>(sampleToValues.keySet());
            Collections.sort(sampleList);
            valuesList = new ArrayList<List<String>>();
            for (String sample : sampleList) {
                List<Double> values = sampleToValues.get(sample);
                List<String> copy = new ArrayList<String>(values.size());
                valuesList.add(copy);
                for (Double value : values)
                    copy.add(PlugInUtilities.formatProbability(value));
            }
            fireTableStructureChanged();
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0)
                return sampleList.get(rowIndex);
            else
                return valuesList.get(rowIndex).get(columnIndex - 1);
        }
    }
    
}
