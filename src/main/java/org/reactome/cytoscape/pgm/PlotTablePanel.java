/*
 * Created on Mar 26, 2014
 *
 */
package org.reactome.cytoscape.pgm;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.RowSorterEvent;
import javax.swing.event.RowSorterListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This customized JPanel is used to plot contents in a table using JFreeChart.
 * @author gwu
 *
 */
public class PlotTablePanel extends JPanel {
    private final String EMPTY_DATA_MESSAGE = "Select one or more variables having no \"INFINITY\" value to plot.";
    private final String TOO_MANY_LINES_MESSAGE = "Too many columns in the table to draw lines!";
    private final int MAXIMUM_COLUMNS_FOR_PLOT = 13;
    public static final String FDR_COL_NAME_AFFIX = "(FDR)";
    public static final String P_VALUE_COL_NAME_AFFIX = "(pValue)";
    // Used to draw
    private DefaultCategoryDataset dataset;
    // For p-values
    private DefaultCategoryDataset fdrDataset;
    private CategoryPlot plot;
    private ChartPanel chartPane;
    // Table content
    private JTable contentTable;
    private JScrollPane tableJsp;
    
    /**
     * Default constructor.
     */
    public PlotTablePanel(String axisName,
                          boolean needFDRAxis) {
        init(axisName, 
             needFDRAxis);
    }

    private void init(String axisName,
                      boolean needPValuePlot) {
        setLayout(new BorderLayout());
        
        JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        jsp.setDividerLocation(0.50d);
        jsp.setDividerLocation(150); // Give the plot 150 px initially
        add(jsp, BorderLayout.CENTER);
        
        JPanel graphPane = createGraphPane(axisName, needPValuePlot);
        jsp.setLeftComponent(graphPane);
        // Should be replaced by an actual table
        contentTable = new JTable();
        tableJsp = new JScrollPane();
        tableJsp.setViewportView(contentTable);
        jsp.setRightComponent(tableJsp);
    }
    
    public void setTable(JTable table) {
        this.contentTable = table;
        // Replace the original default table with this one.
        tableJsp.setViewportView(table);
        
        contentTable.getModel().addTableModelListener(new TableModelListener() {
            
            @Override
            public void tableChanged(TableModelEvent e) {
                // The following call has to wait until other Swing related thing is done.
                // Otherwise, it is possible to throw an index related exception
                SwingUtilities.invokeLater(new Runnable() {
                    
                    @Override
                    public void run() {
                        resetPlotDataset();
                    }
                });
//                resetPlotDataset();
            }
        });
        contentTable.getRowSorter().addRowSorterListener(new RowSorterListener() {
            
            @Override
            public void sorterChanged(RowSorterEvent e) {
                resetPlotDataset();
            }
        });
        
        // A simple object to link marking together for both table and plot.
        TableAndPlotActionSynchronizer tpSync = new TableAndPlotActionSynchronizer(contentTable,
                                                                                   chartPane);
    }
    
    public void setTableModel(TableModel model) {
        contentTable.setModel(model);
    }
    
    public TableModel getTableModel() {
        return contentTable.getModel();
    }
    
    /**
     * Set the FDR axis visible or invisible. This method
     * cannot be called even though a p-value axis is not initialized
     * using a false parameter in its object's constructor.
     * @param isVisible
     */
    public void setFDRAxisVisible(boolean isVisible) {
        ValueAxis fdrAxis = plot.getRangeAxis(1);
        if (fdrAxis != null)
            fdrAxis.setVisible(isVisible);
    }
    
    /**
     * Set the text lable for the Y-axis.
     * @param label
     */
    public void setYAxisLabel(String label) {
        ValueAxis valueAxis = plot.getRangeAxis(); // This should be y-axis
        valueAxis.setLabel(label);
    }
    
    private JPanel createGraphPane(String axisName,
                                   boolean needPValuePlot) {
        dataset = new DefaultCategoryDataset();
        // Want to control data update by this object self to avoid
        // conflict exception.
        dataset.setNotify(false);
        CategoryAxis axisX = new CategoryAxis("Sample");
        // Draw lines but not shapes. However, this is user configurable
        // in the fly. So the choice is not so critical.
        LineAndShapeRenderer renderer = new LineAndShapeRenderer(true, 
                                                                 false);
        renderer.setBaseStroke(new BasicStroke(2.0f)); // Give it a little more stroke
        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        plot = new CategoryPlot(dataset,
                                axisX,
                                new NumberAxis(axisName),
                                renderer);
        
        if (needPValuePlot) {
            fdrDataset = new DefaultCategoryDataset();
            plot.setDataset(1, fdrDataset);
            NumberAxis pValueAxis = new NumberAxis("p-Value/FDR");
            plot.setRangeAxis(1, pValueAxis);
            LineAndShapeRenderer renderer1 = new LineAndShapeRenderer(true, 
                                                                      false);
            renderer1.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
            plot.setRenderer(1, renderer1);
            plot.mapDatasetToRangeAxis(1, 1);
        }
        
        plot.setNoDataMessage(EMPTY_DATA_MESSAGE);
        
        JFreeChart chart = new JFreeChart(plot);
        chartPane = new ChartPanel(chart);
        chartPane.setBorder(BorderFactory.createEtchedBorder());

        return chartPane;
    }
    
    private void resetPlotDataset() {
        dataset.clear();
        if (fdrDataset != null)
            fdrDataset.clear();
        TableModel model = contentTable.getModel();
        if (model.getColumnCount() > MAXIMUM_COLUMNS_FOR_PLOT) {
            plot.setNoDataMessage(TOO_MANY_LINES_MESSAGE);
        }
        else {
            plot.setNoDataMessage(EMPTY_DATA_MESSAGE);
            // In the following, use the model, instead of the table,
            // to get values. For some reason, the table's data is not correct!
            // Most likely, this is because the use of a RowSorter.
            for (int col = 1; col < model.getColumnCount(); col++) {
                List<Double> values = readValues(model, col);
                if (values == null)
                    continue;
                String colName = model.getColumnName(col);
                for (int row = 0; row < model.getRowCount(); row++) {
                    int index = contentTable.convertRowIndexToModel(row);
                    String sample = (String) model.getValueAt(index, 0);
                    if (colName.endsWith(P_VALUE_COL_NAME_AFFIX) ||
                        colName.endsWith(FDR_COL_NAME_AFFIX))
                        fdrDataset.addValue(values.get(index),
                                            colName,
                                            sample);
                    else
                        dataset.addValue(values.get(index),
                                         colName,
                                         sample);
                }
            }
            // The following code is used to control performance:
            // 16 is arbitrary
            CategoryAxis axis = plot.getDomainAxis();
            if (model.getRowCount() > PlugInUtilities.PLOT_CATEGORY_AXIX_LABEL_CUT_OFF) {
                axis.setTickLabelsVisible(false);
                axis.setTickMarksVisible(false);
            }
            else {
                axis.setTickLabelsVisible(true);
                axis.setTickMarksVisible(true);
            }
        }
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
        if (fdrDataset != null) {
            DatasetChangeEvent pValueEvent = new DatasetChangeEvent(this, fdrDataset);
            plot.datasetChanged(pValueEvent);
        }
    }
    
    /**
     * Use this helper method to read in a list of double value displayed in a 
     * table. If there is any INFINITY in the column, a null is returned.
     * @param col
     * @return
     */
    private List<Double> readValues(TableModel model,
                                    int col) {
        List<Double> rtn = new ArrayList<Double>();
        try {
            for (int row = 0; row < model.getRowCount(); row ++) {
                String value = (String) model.getValueAt(row, col);
                rtn.add(new Double(value));
            }
        }
        catch(NumberFormatException e) {
            return null;
        }
        return rtn;
    }
    
}
