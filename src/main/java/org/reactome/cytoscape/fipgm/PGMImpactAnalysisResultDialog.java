/*
 * Created on Sep 17, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.Variable;

/**
 * A customized JDialog showing the analysis results from FI_PGM impact.
 * @author gwu
 *
 */
public class PGMImpactAnalysisResultDialog extends JDialog {
    // A hard-coded list of genes
    private final int TOTAL_GENE = 1000;
    private JTable resultTable;
    private boolean isOkClicked;
    private JLabel tableLabel;
    private JLabel noteLabel;
    
    public PGMImpactAnalysisResultDialog() {
        super(PlugInObjectManager.getManager().getCytoscapeDesktop());
        init();
    }
    
    private void init() {
        setTitle("FI PGM Impact Analysis Results");
        JPanel contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        Border out = BorderFactory.createEtchedBorder();
        Border in = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        contentPane.setBorder(BorderFactory.createCompoundBorder(out, in));
        tableLabel = new JLabel("Choose genes to display");
        tableLabel.setFont(tableLabel.getFont().deriveFont(Font.BOLD));
        contentPane.add(tableLabel, BorderLayout.NORTH);
        initResultTable();
        contentPane.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        // Add filters based on Genes and/or Sum
        JPanel filterPane = createFilterPane();
        filterPane.setBorder(BorderFactory.createEtchedBorder());
        JPanel bottomPane = new JPanel();
        bottomPane.setBorder(BorderFactory.createEtchedBorder());
        bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.Y_AXIS));
        bottomPane.add(filterPane);
        noteLabel = new JLabel("Note:");
        noteLabel.setFont(noteLabel.getFont().deriveFont(Font.ITALIC));
        bottomPane.add(noteLabel);
        contentPane.add(bottomPane, BorderLayout.SOUTH);
        
        getContentPane().add(contentPane, BorderLayout.CENTER);
        
        DialogControlPane controlPane = new DialogControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        setSize(750, 650);
        GKApplicationUtilities.center(this);
        
        // Add some controls
        controlPane.getOKBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!validateSelectedGenes())
                    return;
                dispose();
                isOkClicked = true;
            }
        });
        controlPane.getCancelBtn().addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
                isOkClicked = false;
            }
        });
    }
    
    private boolean validateSelectedGenes() {
        // Check the number of displayed genes
        int totalGenes = resultTable.getRowCount();
        if (totalGenes > TOTAL_GENE) {
            JOptionPane.showMessageDialog(this,
                                          "Please filter genes to less than " + TOTAL_GENE + ".", 
                                          "Too Many Genes",
                                          JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    private JPanel createFilterPane() {
        JPanel pane = new JPanel();
//        pane.setBorder(BorderFactory.createEtchedBorder());
        pane.setLayout(new FlowLayout(FlowLayout.LEFT));
        JLabel label = new JLabel("Show rows for genes containing: ");
        pane.add(label);
        final JTextField geneTF = new JTextField();
        geneTF.setColumns(6);
        pane.add(geneTF);
        String[] andOr = new String[]{"and", "or"};
        final JComboBox<String> andOrBox = new JComboBox<String>(andOr);
        pane.add(andOrBox);
        label = new JLabel("Sum of scores greater than: ");
        pane.add(label);
        final JTextField sumTF = new JTextField();
        sumTF.setColumns(4);
        pane.add(sumTF);
        // Enable filtering
        DocumentListener docListener = new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterRows(geneTF, andOrBox, sumTF);
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterRows(geneTF, andOrBox, sumTF);
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };
        geneTF.getDocument().addDocumentListener(docListener);
        sumTF.getDocument().addDocumentListener(docListener);
        andOrBox.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                filterRows(geneTF, andOrBox, sumTF);
            }
        });
        return pane;
    }
    
    private void filterRows(final JTextField geneTF,
                            final JComboBox<String> andOrBox,
                            final JTextField sumTF) {
        // Check to make sure the number is a numer
        try {
            String text = sumTF.getText().trim();
            if (text.length() > 0)
                new Double(text);
        }
        catch(NumberFormatException e) {
            return; // Just do nothing
        }
        RowFilter<ResultTableModel, Object> rowFilter = new RowFilter<ResultTableModel, Object>() {
            @Override
            public boolean include(Entry<? extends ResultTableModel, ? extends Object> entry) {
                // The first GUI should be text for gene
                String gene = geneTF.getText().trim();
                // The second is AndOr box
                String andOr = null;
                // The third GUI should be text for sum
                String sum = sumTF.getText().trim();
                boolean rtn = true;
                if (gene.length() > 0) {
                    String geneEntry = (String) entry.getValue(0);
                    rtn &= geneEntry.contains(gene);
                    andOr = andOrBox.getSelectedItem().toString();
                }
                if (sum.length() > 0) {
                    Double sumEntry = (Double) entry.getValue(1);
                    if (andOr == null || andOr.equals("and"))
                        rtn &= (sumEntry > new Double(sum));
                    else
                        rtn |= (sumEntry > new Double(sum));
                }
                return rtn;
            }
        };
        TableRowSorter<ResultTableModel> rowSorter = (TableRowSorter<ResultTableModel>) resultTable.getRowSorter();
        rowSorter.setRowFilter(rowFilter);
        tableLabel.setText("Choose genes to construct a FI network (" + resultTable.getRowCount() + " displayed)");
    }
    
    private void initResultTable() {
        resultTable = new JTable();
        ResultTableModel model = new ResultTableModel();
        resultTable.setModel(model);
        // Need to add table sort
        TableRowSorter<ResultTableModel> sorter = new TableRowSorter<ResultTableModel>(model);
        resultTable.setRowSorter(sorter);
        // Renderer for double to control the digits
        TableCellRenderer renderer = new DefaultTableCellRenderer() {
            
            @Override
            public Component getTableCellRendererComponent(JTable table, 
                                                           Object value,
                                                           boolean isSelected,
                                                           boolean hasFocus,
                                                           int row, 
                                                           int column) {
                Component comp = super.getTableCellRendererComponent(table, 
                                                                     value,
                                                                     isSelected, 
                                                                     hasFocus, 
                                                                     row, 
                                                                     column);
                if (value instanceof Double) {
                    setText(String.format("%.5f", value));
                }
                return comp;
            }
        };
        resultTable.setDefaultRenderer(Double.class, renderer);
        model.addTableModelListener(new TableModelListener() {
            
            @Override
            public void tableChanged(TableModelEvent e) {
                tableLabel.setText("Choose genes to construct a FI network (" + resultTable.getRowCount() + " displayed)");
            }
        });
    }
    
    public boolean isOkClicked() {
        return this.isOkClicked;
    }
    
    public Map<String, Double> getSelectedGeneToScore() {
        Map<String, Double> geneToScore = new HashMap<>();
        for (int i = 0; i < resultTable.getRowCount(); i++) {
            String gene = (String) resultTable.getValueAt(i, 0);
            Double score = (Double) resultTable.getValueAt(i, 1);
            geneToScore.put(gene, score);
        }
        return geneToScore;
    }
    
    public void setSampleResults(Map<String, Map<Variable, Double>> sampleToVarToScore) {
        // Need to get genes
        Set<String> genes = new HashSet<String>();
        // Get from the first result
        Map<Variable, Double> varToScore = sampleToVarToScore.values().iterator().next();
        for (Variable var : varToScore.keySet())
            genes.add(var.getName());
        ResultTableModel model = (ResultTableModel) resultTable.getModel();
        List<String> geneList = new ArrayList<>(genes);
        Collections.sort(geneList);
        model.setGenes(geneList);
        for (String sample : sampleToVarToScore.keySet())
            addSampleResult(sample, sampleToVarToScore.get(sample));
        model.calcualteMeans();
        displayNote(sampleToVarToScore);
        model.fireTableStructureChanged();
    }

    private void displayNote(Map<String, Map<Variable, Double>> sampleToVarToScore) {
        StringBuilder message = new StringBuilder();
        message.append("Note: ");
        message.append(sampleToVarToScore.size()).append(" samples in total. ");
        if (sampleToVarToScore.size() > 8)
            message.append("Only 8 samples are listed.");
        noteLabel.setText(message.toString());
    }
    
    private void addSampleResult(String sample,
                                 Map<Variable, Double> varToScore) {
        Map<String, Double> geneToScore = new HashMap<String, Double>();
        for (Variable var : varToScore.keySet()) {
            Double score = varToScore.get(var);
            geneToScore.put(var.getName(), score);
        }
        SampleResult sampleResult = new SampleResult();
        sampleResult.sampleName = sample;
        sampleResult.geneToScore = geneToScore;
        ResultTableModel model = (ResultTableModel) resultTable.getModel();
        model.addSampleResult(sampleResult);
    }
    
    /**
     * A simple class to hold sample and inference result information for the table model.
     * @author gwu
     *
     */
    private class SampleResult {
        
        String sampleName;
        Map<String, Double> geneToScore;
        
    }
    
    /**
     * A customized TableModel showing the inference results.
     * @author gwu
     *
     */
    private class ResultTableModel extends AbstractTableModel {
        // The results
        private List<SampleResult> sampleResults;
        private List<String> genes;
        private List<Double> means; 
        
        public ResultTableModel() {
            // Initialize with empty values to avoid null exception
            sampleResults = new ArrayList<SampleResult>();
            genes = new ArrayList<String>();
            means = new ArrayList<Double>();
        }
        
        public void setGenes(List<String> genes) {
            this.genes.clear();
            this.genes.addAll(genes);
            // Empty sums
            means.clear();
            for (int i = 0; i < genes.size(); i++)
                means.add(0.0d); // Initialize it with 0.
        }
        
        public void calcualteMeans() {
            int sampleSize = sampleResults.size();
            for (int i = 0; i < means.size(); i++) {
                Double mean = means.get(i);
                means.set(i, mean / sampleSize);
            }
        }
        
        public void addSampleResult(SampleResult sampleResult) {
            sampleResults.add(sampleResult);
            updateSums(sampleResult);
        }
        
        private void updateSums(SampleResult sampleResult) {
            for (int i = 0; i < genes.size(); i++) {
                String gene = genes.get(i);
                Double mean = means.get(i);
                Double score = sampleResult.geneToScore.get(gene);
                mean += score;
                means.set(i, mean);
            }
        }

        @Override
        public int getRowCount() {
            return genes.size();
        }

        @Override
        public int getColumnCount() {
            // The maximum samples number should be
            // 10, which is arbitray.
            int cols = 2 + sampleResults.size(); // At least 10 columns
            if (cols > 10)
                cols = 10;
            return cols;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return String.class;
            return Double.class;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0)
                return genes.get(rowIndex);
            if (columnIndex == 1)
                return means.get(rowIndex);
            SampleResult result = sampleResults.get(columnIndex - 2);
            return result.geneToScore.get(genes.get(rowIndex));
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0)
                return "Gene";
            if (column == 1)
                return "Mean";
            SampleResult result = sampleResults.get(column - 2);
            return result.sampleName;
        }
    }

}
