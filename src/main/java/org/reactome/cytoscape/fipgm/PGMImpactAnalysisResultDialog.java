/*
 * Created on Sep 17, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import org.apache.commons.math.MathException;
import org.gk.qualityCheck.ResultTableModel;
import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.reactome.cytoscape.pgm.TTestTablePlotPane;
import org.reactome.cytoscape.service.TTestTableModel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.factorgraph.Variable;

/**
 * A customized JDialog showing the analysis results from FI_PGM impact.
 * @author gwu
 *
 */
public class PGMImpactAnalysisResultDialog extends JDialog {
    private final int MAXIMUM_ROW_FOR_PLOT = 500;
    // A hard-coded list of genes
    private final int TOTAL_GENE = 1000;
    private JTable resultTable;
    private boolean isOkClicked;
    private JLabel noteLabel;
    // To show results by doing t-test
    private TTestTablePlotPane<Variable> tTestPlotPane;
    
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
        
        tTestPlotPane = createTablePlotPane();
        resultTable = tTestPlotPane.getTable();
        contentPane.add(tTestPlotPane, BorderLayout.CENTER);

        // Add filters based on Genes and/or Sum
        JPanel filterPane = createFilterPane();
        filterPane.setBorder(BorderFactory.createEtchedBorder());
        JPanel bottomPane = new JPanel();
        bottomPane.setBorder(BorderFactory.createEtchedBorder());
        bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.Y_AXIS));
        bottomPane.add(filterPane);
        noteLabel = new JLabel("Note: If more than " + MAXIMUM_ROW_FOR_PLOT + " rows are displayed in " +
                               "the table, only " + MAXIMUM_ROW_FOR_PLOT + " rows are plotted.");
        noteLabel.setFont(noteLabel.getFont().deriveFont(Font.ITALIC));
        JPanel notePane = new JPanel();
        notePane.setLayout(new FlowLayout(FlowLayout.LEFT));
        notePane.add(noteLabel);
        notePane.setBorder(BorderFactory.createEtchedBorder());
        bottomPane.add(notePane);
        contentPane.add(bottomPane, BorderLayout.SOUTH);
        
        getContentPane().add(contentPane, BorderLayout.CENTER);
        
        DialogControlPane controlPane = new DialogControlPane();
        getContentPane().add(controlPane, BorderLayout.SOUTH);
        setSize(1000, 650);
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
    
    private TTestTablePlotPane<Variable> createTablePlotPane() {
        TTestTablePlotPane<Variable> tablePlotPane = new TTestTablePlotPane<Variable>() {

            @Override
            protected String[] getAnnotations(Variable key) {
                return new String[]{key.getName()}; // Don't want to show two columns having the same content.
            }

            @Override
            protected String getKey(Variable key) {
                return key.getName();
            }

            @Override
            protected void sortValueKeys(List<Variable> list) {
                Collections.sort(list, new Comparator<Variable>() {
                    public int compare(Variable var1, Variable var2) {
                        String key1 = var1.getName();
                        String key2 = var2.getName();
                        return key1.compareTo(key2);
                    }
                });
            }
            
        };
        TTestTableModel model = (TTestTableModel) tablePlotPane.getTable().getModel();
        String[] headers = new String[]{
                "Name",
                "RealMean",
                "RandomMean",
                "MeanDiff",
                "p-value",
                "FDR"
        };
        model.setColHeaders(Arrays.asList(headers),
                            1);
        tablePlotPane.setChartTitle("Boxplot for Protein Impact Score");
        Font font = tablePlotPane.getBottomPValueLabel().getFont();
        font = font.deriveFont(Font.BOLD);
        tablePlotPane.getBottomPValueLabel().setFont(font);
        tablePlotPane.getBottomTitleLabel().setFont(font);
        tablePlotPane.getBottomTitleLabel().setText("Choose genes to construct a FI network: ");
        // Set labels
        tablePlotPane.getPlot().getRangeAxis().setAttributedLabel("Impact Score");
        tablePlotPane.getPlot().getDomainAxis().setAttributedLabel("Protein");
        tablePlotPane.setMaximumRowsForPlot(MAXIMUM_ROW_FOR_PLOT); // Just an arbitrary limit
        return tablePlotPane;
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
        final JComboBox<String> andOrBox1 = new JComboBox<String>(andOr);
        pane.add(andOrBox1);
        label = new JLabel("FDR less than: ");
        pane.add(label);
        final JTextField fdrTF = new JTextField();
        fdrTF.setColumns(4);
        pane.add(fdrTF);
        // For MeanDiff
        final JComboBox<String> andOrBox2 = new JComboBox<String>(andOr);
        pane.add(andOrBox2);
        label = new JLabel("MeanDiff greater than: ");
        pane.add(label);
        final JTextField meanDiffTF = new JTextField();
        meanDiffTF.setColumns(4);
        pane.add(meanDiffTF);
        // Enable filtering
        DocumentListener docListener = new DocumentListener() {
            
            @Override
            public void removeUpdate(DocumentEvent e) {
                filterRows(geneTF, 
                           andOrBox1, 
                           fdrTF,
                           andOrBox2,
                           meanDiffTF);
            }
            
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterRows(geneTF, 
                           andOrBox1, 
                           fdrTF,
                           andOrBox2,
                           meanDiffTF);
            }
            
            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };
        geneTF.getDocument().addDocumentListener(docListener);
        fdrTF.getDocument().addDocumentListener(docListener);
        meanDiffTF.getDocument().addDocumentListener(docListener);
        andOrBox1.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                filterRows(geneTF, 
                           andOrBox1, 
                           fdrTF,
                           andOrBox2,
                           meanDiffTF);
            }
        });
        andOrBox2.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                filterRows(geneTF, 
                           andOrBox1, 
                           fdrTF,
                           andOrBox2,
                           meanDiffTF);
            }
        });
        return pane;
    }
    
    private void filterRows(final JTextField geneTF,
                            final JComboBox<String> andOrBox1,
                            final JTextField fdrTF,
                            final JComboBox<String> andOrBox2,
                            final JTextField meanDiffTF) {
        // Check to make sure the number is a numer
        try {
            String text = fdrTF.getText().trim();
            if (text.length() > 0)
                new Double(text);
            text = meanDiffTF.getText().trim();
            if (text.length() > 0)
                new Double(text); // Just a test
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
                String fdr = fdrTF.getText().trim();
                String meanDiff = meanDiffTF.getText().trim();
                boolean rtn = true;
                if (gene.length() > 0) {
                    String geneEntry = (String) entry.getValue(0);
                    rtn &= geneEntry.contains(gene);
                }
                if (fdr.length() > 0) {
                    // String has been used for all column
                    String fdrEntry = (String) entry.getValue(5);
                    andOr = andOrBox1.getSelectedItem().toString();
                    if (andOr.equals("and"))
                        rtn &= (new Double(fdrEntry) < new Double(fdr));
                    else
                        rtn |= (new Double(fdrEntry) < new Double(fdr));
                }
                if (meanDiff.length() > 0) {
                 // String has been used for all column
                    String meanDiffEntry = (String) entry.getValue(3);
                    andOr = andOrBox2.getSelectedItem().toString();
                    if (andOr.equals("and"))
                        rtn &= (new Double(meanDiffEntry) > new Double(meanDiff));
                    else 
                        rtn |= (new Double(meanDiffEntry) > new Double(meanDiff));
                }
                return rtn;
            }
        };
        TableRowSorter<ResultTableModel> rowSorter = (TableRowSorter<ResultTableModel>) resultTable.getRowSorter();
        rowSorter.setRowFilter(rowFilter);
        tTestPlotPane.getBottomPValueLabel().setText(resultTable.getRowCount() + " displayed.");
    }
    
    public boolean isOkClicked() {
        return this.isOkClicked;
    }
    
    public Map<String, Double> getSelectedGeneToScore() {
        Map<String, Double> geneToScore = new HashMap<>();
        for (int i = 0; i < resultTable.getRowCount(); i++) {
            String gene = (String) resultTable.getValueAt(i, 0);
            // Use the average score
            String scoreText = (String) resultTable.getValueAt(i, 1);
            Double score = new Double(scoreText);
            geneToScore.put(gene, score);
        }
        return geneToScore;
    }
    
    public void setSampleResults(Map<String, Map<Variable, Double>> sampleToVarToScore,
                                 Map<String, Map<Variable, Double>> randomSampleToVarToScore) {
        try {
            Map<Variable, List<Double>> varToScores = getVariableToScores(sampleToVarToScore);
            Map<Variable, List<Double>> randomVarToScores = getVariableToScores(randomSampleToVarToScore);
            
            tTestPlotPane.setDisplayValues("Real Samples",
                                           varToScores, 
                                           "Random Samples", 
                                           randomVarToScores);
            tTestPlotPane.getBottomPValueLabel().setText(varToScores.size() + " displayed.");
        }
        catch(MathException e) {
            JOptionPane.showMessageDialog(this,
                                          "Error in displaying results: " + e,
                                          "Error in Result Display",
                                          JOptionPane.ERROR_MESSAGE);
            return;
        }
        //        // Need to get genes
        //        Set<String> genes = new HashSet<String>();
        //        // Get from the first result
        //        Map<Variable, Double> varToScore = sampleToVarToScore.values().iterator().next();
        //        for (Variable var : varToScore.keySet())
        //            genes.add(var.getName());
        //        ResultTableModel model = (ResultTableModel) resultTable.getModel();
        //        List<String> geneList = new ArrayList<>(genes);
        //        Collections.sort(geneList);
        //        model.setGenes(geneList);
        //        for (String sample : sampleToVarToScore.keySet())
        //            addSampleResult(sample, sampleToVarToScore.get(sample));
        //        model.calcualteMeans();
        //        displayNote(sampleToVarToScore);
        //        model.fireTableStructureChanged();
    }
    
    private Map<Variable, List<Double>> getVariableToScores(Map<String, Map<Variable, Double>> sampleToVarToScore) {
        Map<Variable, List<Double>> varToScores = new HashMap<>();
        for (String sample : sampleToVarToScore.keySet()) {
            Map<Variable, Double> varToScore = sampleToVarToScore.get(sample);
            for (Variable var : varToScore.keySet()) {
                Double score = varToScore.get(var);
                List<Double> scores = varToScores.get(var);
                if (scores == null) {
                    scores = new ArrayList<>();
                    varToScores.put(var, scores);
                }
                scores.add(score);
            }
        }
        return varToScores;
    }
}
