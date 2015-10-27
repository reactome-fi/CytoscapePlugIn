/*
 * Created on Oct 12, 2015
 *
 */
package org.reactome.cytoscape.fipgm;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import org.apache.commons.math.MathException;
import org.gk.qualityCheck.ResultTableModel;
import org.reactome.cytoscape.pgm.TTestTablePlotPane;
import org.reactome.cytoscape.service.TTestTableModel;
import org.reactome.factorgraph.Variable;

/**
 * A customized JPanel containing a TTestTablePlot and a pane for filtering.
 * @author gwu
 *
 */
public class FilterableTTestTablePlotPane extends JPanel {
    private int maximumRowForPlot = 500; // default 50;
    private TTestTablePlotPane<Variable> tTestPlotPane;
    private JTable resultTable;
    private JLabel noteLabel;
    // Track this so that we can turn it off
    private JPanel filterPane;
    
    /**
     * Default constructor.
     */
    public FilterableTTestTablePlotPane() {
        init();
    }
    
    public TTestTablePlotPane<Variable> gettTestPlotPane() {
        return tTestPlotPane;
    }
    
    public JLabel getNoteLabel() {
        return this.noteLabel;
    }

    public JTable getResultTable() {
        return resultTable;
    }

    public int getMaximumRowForPlot() {
        return maximumRowForPlot;
    }

    public void setMaximumRowForPlot(int maximumRowForPlot) {
        this.maximumRowForPlot = maximumRowForPlot;
    }
    
    public void hideFilterPane() {
        filterPane.setVisible(false);
    }
    
    public void setSampleResults(Map<String, Map<Variable, Double>> sampleToVarToScore,
                                 Map<String, Map<Variable, Double>> randomSampleToVarToScore) throws MathException {
        Map<Variable, List<Double>> varToScores = getVariableToScores(sampleToVarToScore);
        Map<Variable, List<Double>> randomVarToScores = getVariableToScores(randomSampleToVarToScore);
        
        tTestPlotPane.setDisplayValues("Real Samples",
                                       varToScores, 
                                       "Random Samples", 
                                       randomVarToScores);
        tTestPlotPane.getBottomPValueLabel().setText(varToScores.size() + " displayed.");
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

    private void init() {
        setLayout(new BorderLayout());
        Border out = BorderFactory.createEtchedBorder();
        Border in = BorderFactory.createEmptyBorder(2, 2, 2, 2);
        setBorder(BorderFactory.createCompoundBorder(out, in));
        
        tTestPlotPane = createTablePlotPane();
        resultTable = tTestPlotPane.getTable();
        add(tTestPlotPane, BorderLayout.CENTER);

        // Add filters based on Genes and/or Sum
        filterPane = createFilterPane();
        filterPane.setBorder(BorderFactory.createEtchedBorder());
        JPanel bottomPane = new JPanel();
        bottomPane.setBorder(BorderFactory.createEtchedBorder());
        bottomPane.setLayout(new BoxLayout(bottomPane, BoxLayout.Y_AXIS));
        bottomPane.add(filterPane);
        noteLabel = new JLabel("Note: If more than " + maximumRowForPlot + " rows are displayed in " +
                               "the table, only " + maximumRowForPlot + " rows are plotted.");
        noteLabel.setFont(noteLabel.getFont().deriveFont(Font.ITALIC));
        JPanel notePane = new JPanel();
        notePane.setLayout(new FlowLayout(FlowLayout.LEFT));
        notePane.add(noteLabel);
        notePane.setBorder(BorderFactory.createEtchedBorder());
        bottomPane.add(notePane);
        add(bottomPane, BorderLayout.SOUTH);
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
        tablePlotPane.setMaximumRowsForPlot(maximumRowForPlot); // Just an arbitrary limit
        return tablePlotPane;
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
        @SuppressWarnings("unchecked")
        TableRowSorter<ResultTableModel> rowSorter = (TableRowSorter<ResultTableModel>) resultTable.getRowSorter();
        rowSorter.setRowFilter(rowFilter);
        tTestPlotPane.getBottomPValueLabel().setText(resultTable.getRowCount() + " displayed.");
    }
    
}
