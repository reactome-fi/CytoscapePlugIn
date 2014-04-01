/*
 * Created on Apr 1, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.inference.TestUtils;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.reactome.cytoscape.util.PlugInUtilities;
import org.reactome.pgm.PGMVariable;
import org.reactome.r3.util.MathUtilities;

/**
 * This customized JDialog is used to show details information based on t-test for
 * IPA values.
 * @author gwu
 *
 */
public class TTestDetailDialog extends JDialog {
    private DefaultBoxAndWhiskerCategoryDataset dataset;
    private CategoryPlot plot;
    private ChartPanel chartPanel;
    private JTable tTestResultTable;
    // For combined p-value
    private JLabel combinedPValueLabel;
    // Cache calculated IPA values for sorting purpose
    private Map<String, List<Double>> realSampleToIPAs;
    private Map<String, List<Double>> randomSampleToIPAs;
    
    public TTestDetailDialog(JFrame frame) {
        super(frame);
        init();
    }
    
    private void init() {
        JPanel boxPlotPane = createBoxPlotPane();
        JPanel ttestResultPane = createTTestResultTable();
        JPanel combinedPValuePane = createCombinedPValuePane();
        
        JPanel lowerPane = new JPanel();
        lowerPane.setLayout(new BorderLayout());
        lowerPane.add(ttestResultPane, BorderLayout.CENTER);
        lowerPane.add(combinedPValuePane, BorderLayout.SOUTH);
        
        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                        boxPlotPane,
                                        lowerPane);
        jsp.setDividerLocation(0.5d);
        jsp.setDividerLocation(300); // Need to set an integer. Otherwise, the plot is too narrow.
        boxPlotPane.setPreferredSize(new Dimension(800, 300));
        
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        JPanel closePane = new JPanel();
        closePane.add(closeBtn);
        
        getContentPane().add(jsp, BorderLayout.CENTER);
        getContentPane().add(closePane, BorderLayout.SOUTH);
        
        realSampleToIPAs = new HashMap<String, List<Double>>();
        randomSampleToIPAs = new HashMap<String, List<Double>>();
        
        installListeners();
    }
    
    private void installListeners() {
        tTestResultTable.getRowSorter().addRowSorterListener(new RowSorterListener() {
            
            @Override
            public void sorterChanged(RowSorterEvent e) {
                rePlotData();
            }
        });
        // Use this simple method to make sure marker is syncrhonized between two views.
        TableAndPlotActionSynchronizer tpSyncHelper = new TableAndPlotActionSynchronizer(tTestResultTable,
                                                                                         chartPanel);
    }
    
    private void rePlotData() {
        dataset.clear();
        TTestTableModel tableModel = (TTestTableModel) tTestResultTable.getModel();
        for (int i = 0; i < tTestResultTable.getRowCount(); i++) {
            int index = tTestResultTable.convertRowIndexToModel(i);
            String varLabel = (String) tableModel.getValueAt(index, 0);
            List<Double> realIPAs = realSampleToIPAs.get(varLabel);
            dataset.add(realIPAs, "Real Samples", varLabel);
            List<Double> randomIPAs = randomSampleToIPAs.get(varLabel);
            dataset.add(randomIPAs, "Random Samples", varLabel);
        }
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
    }
    
    private JPanel createTTestResultTable() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        JLabel label = new JLabel("T-Test Results");
        Font font = label.getFont();
        label.setFont(font.deriveFont(Font.BOLD, font.getSize() + 2.0f));
        // Want to make sure label is in the middle, so
        // add a panel
        JPanel labelPane = new JPanel();
        labelPane.add(label);
        panel.add(labelPane, BorderLayout.NORTH);
        
        TTestTableModel model = new TTestTableModel();
        tTestResultTable = new JTable(model);
        // Need to add a row sorter
        TableRowSorter<TTestTableModel> sorter = new TableRowSorter<TTestTableModel>(model) {

            @Override
            public Comparator<?> getComparator(int column) {
                if (column == 0) // Just use the String comparator.
                    return super.getComparator(0);
                Comparator<String> rtn = new Comparator<String>() {
                    public int compare(String var1, String var2) {
                        Double value1 = new Double(var1);
                        Double value2 = new Double(var2);
                        return value1.compareTo(value2);
                    }
                };
                return rtn;
            }
            
        };
        tTestResultTable.setRowSorter(sorter);
        
        panel.add(new JScrollPane(tTestResultTable), BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createCombinedPValuePane() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createEtchedBorder());
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        JLabel titleLabel = new JLabel("Combined p-value using Fisher's method: ");
        combinedPValueLabel = new JLabel("1.0");
        
        panel.add(titleLabel);
        panel.add(combinedPValueLabel);
        
        return panel;
    }
    
    private JPanel createBoxPlotPane() {
        dataset = new DefaultBoxAndWhiskerCategoryDataset();
        // Want to control data update by this object self to avoid
        // conflict exception.
        dataset.setNotify(false);
        CategoryAxis xAxis = new CategoryAxis("Output Variable");
        NumberAxis yAxis = new NumberAxis("IPA");
        BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
        // We want to show the variable label
        BoxAndWhiskerToolTipGenerator tooltipGenerator = new BoxAndWhiskerToolTipGenerator() {

            @Override
            public String generateToolTip(CategoryDataset dataset,
                                          int row,
                                          int column) {
                Object variable = dataset.getColumnKey(column);
                String rtn = super.generateToolTip(dataset, row, column);
                return "Variable: " + variable + " " + rtn;
            }
        };
        renderer.setBaseToolTipGenerator(tooltipGenerator);
        plot = new CategoryPlot(dataset,
                                xAxis, 
                                yAxis,
                                renderer);
        JFreeChart chart = new JFreeChart("Boxplot for Output IPAs", 
                                          plot);
        chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEtchedBorder());
        return chartPanel;
    }
    
    public void setVariables(List<PGMVariable> variables) throws MathException {
        // Do a sort
        List<PGMVariable> sortedVars = new ArrayList<PGMVariable>(variables);
        Collections.sort(sortedVars, new Comparator<PGMVariable>() {
            public int compare(PGMVariable var1, PGMVariable var2) {
                return var1.getLabel().compareTo(var2.getLabel());
            }
        });
        dataset.clear();
        realSampleToIPAs.clear();
        randomSampleToIPAs.clear();
        TTestTableModel tableModel = (TTestTableModel) tTestResultTable.getModel();
        List<Double> pvalues = new ArrayList<Double>();
        for (PGMVariable var : sortedVars) {
            Map<String, List<Double>> sampleToProbs = var.getPosteriorValues();
            List<Double> realIPAs = addValueToDataset(sampleToProbs, 
                                                      "Real Samples",
                                                      var);
            List<Double> randomIPAs = addValueToDataset(var.getRandomPosteriorValues(),
                                                        "Random Samples",
                                                        var);
            double pvalue = tableModel.addRow(realIPAs, 
                                              randomIPAs, 
                                              var.getLabel());
            pvalues.add(pvalue);
            realSampleToIPAs.put(var.getLabel(), realIPAs);
            randomSampleToIPAs.put(var.getLabel(), randomIPAs);
        }
        // The following code is used to control performance:
        // 16 is arbitrary
        CategoryAxis axis = plot.getDomainAxis();
        if (variables.size() > PlugInUtilities.PLOT_CATEGORY_AXIX_LABEL_CUT_OFF) {
            axis.setTickLabelsVisible(false);
            axis.setTickMarksVisible(false);
        }
        else {
            axis.setTickLabelsVisible(true);
            axis.setTickMarksVisible(true);
        }
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
        tableModel.fireTableStructureChanged();
        setCombinedPValue(pvalues);
    }
    
    /**
     * Calculate combined p-value from a list of p-values using Fisher's method and display
     * it in a label.
     * @param pvalues
     * @throws MathException
     */
    private void setCombinedPValue(List<Double> pvalues) throws MathException {
        double combinedPValue = MathUtilities.combinePValuesWithFisherMethod(pvalues);
        combinedPValueLabel.setText(PlugInUtilities.formatProbability(combinedPValue));
    }
    
    private List<Double> addValueToDataset(Map<String, List<Double>> sampleToProbs,
                                           String label,
                                           PGMVariable var) {
        List<Double> ipas = new ArrayList<Double>();
        for (List<Double> probs : sampleToProbs.values()) {
            double ipa = PlugInUtilities.calculateIPA(var.getValues(), probs);
            ipas.add(ipa);
        }
        dataset.add(ipas, label, var.getLabel());
        return ipas;
    }
    
    class TTestTableModel extends AbstractTableModel {
        private List<String> colHeaders;
        private List<String[]> data;
        
        public TTestTableModel() {
            String[] headers = new String[]{
                    "Variable",
                    "RealMean",
                    "RandomMean",
                    "MeanDiff",
                    "t-statistic",
                    "p-value"
            };
            colHeaders = Arrays.asList(headers);
            data = new ArrayList<String[]>();
        }
        
        /**
         * Add a new column to the table model. P-value will be returned from this method.
         * @param realIPAs
         * @param randomIPAs
         * @param varLabel
         * @return
         * @throws MathException
         */
        public double addRow(List<Double> realIPAs,
                             List<Double> randomIPAs,
                             String varLabel) throws MathException {
            double realMean = MathUtilities.calculateMean(realIPAs);
            double randomMean = MathUtilities.calculateMean(randomIPAs);
            double diff = realMean - randomMean;
            // Need a double array
            double[] realArray = new double[realIPAs.size()];
            for (int i = 0; i < realIPAs.size(); i++)
                realArray[i] = realIPAs.get(i);
            double[] randomArray = new double[randomIPAs.size()];
            for (int i = 0; i < randomIPAs.size(); i++)
                randomArray[i] = randomIPAs.get(i);
            double t = TestUtils.t(realArray,
                                   randomArray);
            double pvalue = TestUtils.tTest(realArray,
                                            randomArray);
            
            String[] row = new String[colHeaders.size()];
            row[0] = varLabel;
            row[1] = PlugInUtilities.formatProbability(realMean);
            row[2] = PlugInUtilities.formatProbability(randomMean);
            row[3] = PlugInUtilities.formatProbability(diff);
            row[4] = PlugInUtilities.formatProbability(t);
            row[5] = PlugInUtilities.formatProbability(pvalue);
            
            data.add(row);
            
            return pvalue;
        }

        @Override
        public int getRowCount() {
            return data.size();
        }

        @Override
        public int getColumnCount() {
            return colHeaders.size(); 
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            String[] row = data.get(rowIndex);
            return row[columnIndex];
        }

        @Override
        public String getColumnName(int column) {
            return colHeaders.get(column);
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return String.class;
        }
    }
}