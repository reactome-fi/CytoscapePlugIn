/*
 * Created on Feb 9, 2015
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.table.TableRowSorter;

import org.apache.commons.math.MathException;
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
import org.reactome.cytoscape.service.TTestTableModel;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This customized JPanel combines a table for t-test and box plots together into a single user interface component.
 * The wrapped type T is used for the key for values (e.g. it can be a String or a Variable).
 * @author gwu
 *
 */
public class TTestTablePlotPane<T> extends JPanel {
    
    private DefaultBoxAndWhiskerCategoryDataset dataset;
    private CategoryPlot plot;
    private ChartPanel chartPanel;
    private JTable tTestResultTable;
    // For combined p-value
    private JLabel combinedPValueLabel;
    private JLabel combinedTitleLabel;
    // Cache values for sorting purpose
    // We use simple String, instead of T, for easy access to table values.
    private Map<String, List<Double>> nameToValues1;
    private Map<String, List<Double>> nameToValues2;
    private String dataLabel1;
    private String dataLabel2;
    
    /**
     * Default constructor.
     */
    public TTestTablePlotPane() {
        init();
    }
    
    private void init() {
        setLayout(new BorderLayout());
        
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
        jsp.setDividerLocation(0.50d);
        jsp.setDividerLocation(150); // Give the plot 150 px initially
        

        add(jsp, BorderLayout.CENTER);
        
        nameToValues1 = new HashMap<String, List<Double>>();
        nameToValues2 = new HashMap<String, List<Double>>();
        
        installListeners();
    }
    
    protected void installListeners() {
        tTestResultTable.getRowSorter().addRowSorterListener(new RowSorterListener() {
            
            @Override
            public void sorterChanged(RowSorterEvent e) {
                rePlotData();
            }
        });
        // Use this simple method to make sure marker is synchronized between two views.
        TableAndPlotActionSynchronizer tpSyncHelper = new TableAndPlotActionSynchronizer(tTestResultTable, 
                                                                                         chartPanel);
    }
    
    public JTable getTable() {
        return this.tTestResultTable;
    }

    private void rePlotData() {
        dataset.clear();
        TTestTableModel tableModel = (TTestTableModel) tTestResultTable.getModel();
        // In two cases, these values will be set dynamically. So we have to
        // get them in the table.
        for (int i = 0; i < tTestResultTable.getRowCount(); i++) {
            int index = tTestResultTable.convertRowIndexToModel(i);
            String name = (String) tableModel.getValueAt(index, 0);
            List<Double> values1 = nameToValues1.get(name);
            dataset.add(values1, dataLabel1, name);
            List<Double> values2 = nameToValues2.get(name);
            dataset.add(values2, dataLabel2, name);
        }
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
    }
    
    private JPanel createTTestResultTable() {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEtchedBorder());
        
        JLabel label = new JLabel("Mean Value Comparison Results");
        Font font = label.getFont();
        label.setFont(font.deriveFont(Font.BOLD));
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
                if (column == 1) // Just use the String comparator for name
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
        
        combinedTitleLabel = new JLabel("Combined p-value using an extension of Fisher's method (click to see the reference): ");
        combinedTitleLabel.setToolTipText("Click to view the reference");
        combinedTitleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        combinedPValueLabel = new JLabel("1.0");
        combinedTitleLabel.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                String url = "http://www.sciencedirect.com/science/article/pii/S0167715202003103";
                PlugInUtilities.openURL(url);
            }
            
        });
        panel.add(combinedTitleLabel);
        panel.add(combinedPValueLabel);
        
        return panel;
    }
    
    private JPanel createBoxPlotPane() {
        dataset = new DefaultBoxAndWhiskerCategoryDataset();
        // Want to control data update by this object self to avoid
        // conflict exception.
        dataset.setNotify(false);
        CategoryAxis xAxis = new CategoryAxis("Variable");
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
        Font font = getFont();
        font = font.deriveFont(Font.BOLD);
        JFreeChart chart = new JFreeChart("Boxplot for Integrated Pathway Activities (IPAs) of Outputs", 
                                          font,
                                          plot, 
                                          true);
        chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEtchedBorder());
        return chartPanel;
    }
    
    /**
     * Set the label as the title for combined p-value. Usually there is no need to call this
     * method.
     * @param title
     */
    public void setCombinedPValueTitle(String title) {
        combinedTitleLabel.setText(title);
    }
    
    /**
     * Set the data to be displayed in this component. 
     * @param dataLabel1
     * @param nameToValues1
     * @param dataLabel2
     * @param nameToValues2
     * @throws MathException
     */
    public void setDisplayValues(String dataLabel1,
                                 Map<T, List<Double>> nameToValues1,
                                 String dataLabel2,
                                 Map<T, List<Double>> nameToValues2) throws MathException {
        this.dataLabel1 = dataLabel1;
        this.dataLabel2 = dataLabel2;
        // Perform a simple sort
        List<T> nameList = new ArrayList<T>(nameToValues1.keySet());
        sortValueKeys(nameList);
        // Make sure names in the two lists are the same
        for (T name : nameList) {
            if (!nameToValues2.keySet().contains(name)) {
                throw new IllegalArgumentException("Sample " + name + " is not in " + dataLabel2);
            }
        }
        dataset.clear();
        this.nameToValues1.clear();
        this.nameToValues2.clear();
        TTestTableModel tableModel = (TTestTableModel) tTestResultTable.getModel();
        tableModel.reset(); // Reset the original data if any.
        List<Double> pvalues = new ArrayList<Double>();
        // For combined p-values
        List<List<Double>> values = new ArrayList<List<Double>>();
        for (T name : nameList) {
            String key = getKey(name);
            List<Double> values1 = nameToValues1.get(name);
            dataset.add(values1, 
                        dataLabel1, 
                        key);
            this.nameToValues1.put(key, values1);
            List<Double> values2 = nameToValues2.get(name);
            dataset.add(values2,
                        dataLabel2, 
                        key);
            this.nameToValues2.put(key, values2);
            double pvalue = tableModel.addRow(values1,
                                              values2,
                                              getAnnotations(name));
            pvalues.add(pvalue);
            values.add(values1);
        }
        // The following code is used to control performance:
        // 16 is arbitrary
        CategoryAxis axis = plot.getDomainAxis();
        if (nameList.size() > PlugInUtilities.PLOT_CATEGORY_AXIX_LABEL_CUT_OFF) {
            axis.setTickLabelsVisible(false);
            axis.setTickMarksVisible(false);
        }
        else {
            axis.setTickLabelsVisible(true);
            axis.setTickMarksVisible(true);
        }
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
        // Make a copy to avoid modifying by the called method
        tableModel.calculateFDRs(new ArrayList<Double>(pvalues));
        tableModel.fireTableStructureChanged();
        calculateCombinedPValue(pvalues, values);
    }
    
    protected String[] getAnnotations(T key) {
        return new String[]{key.toString()};
    }
    
    protected String getKey(T key) {
        return key.toString();
    }
    
    protected void sortValueKeys(List<T> list) {
    }
    
    /**
     * Calculate combined p-value from a list of p-values using Fisher's method and display
     * it in a label.
     * @param pvalues
     * @throws MathException
     */
    private void calculateCombinedPValue(List<Double> pvalues,
                                         List<List<Double>> values) throws MathException {
//        double combinedPValue = MathUtilities.combinePValuesWithFisherMethod(pvalues);
        // Since it is pretty sure, variables are dependent in pathway, use another method
        // to combine p-values
        PValueCombiner combiner = new PValueCombiner();
        double combinedPValue = combiner.combinePValue(values,
                                                       pvalues);
        combinedPValueLabel.setText(PlugInUtilities.formatProbability(combinedPValue));
    }
    
}