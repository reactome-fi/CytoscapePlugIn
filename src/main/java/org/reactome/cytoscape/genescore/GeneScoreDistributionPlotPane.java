package org.reactome.cytoscape.genescore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.gk.util.GKApplicationUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.LayeredBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DatasetChangeEvent;
import org.reactome.r3.util.MathUtilities;

/**
 * This class is used to show the ranks of a selected pathway in the whole distribution
 * of gene scores using jFreeChart's bar plot.
 * The code related to jFreeChart is modified from this web page:
 * https://github.com/ngadde/playground/blob/master/com.iis.sample1/src/main/java/demo/LayeredBarChartDemo2.java
 * @TODO: If the performance is an issue, we may need to create a bona fide plot panel.
 * @author wug
 *
 */
public class GeneScoreDistributionPlotPane extends JPanel {
    private DefaultCategoryDataset dataset;
    private CategoryPlot plot;

    public GeneScoreDistributionPlotPane() {
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        
        JPanel chartPane = createGraphPane();
        add(chartPane, BorderLayout.CENTER);
    }
    
    /**
     * Set the data for plot.
     * @param geneToScore
     * @param pathwayGenes
     */
    public void setGeneToScore(Map<String, Double> geneToScore,
                               Set<String> pathwayGenes) {
        dataset.clear();
        // Sort based on scores
        geneToScore.keySet()
                   .stream()
                   .sorted((g1, g2) -> geneToScore.get(g2).compareTo(geneToScore.get(g1)))
                   .forEach(gene -> dataset.addValue(geneToScore.get(gene), "All Genes", gene));
        pathwayGenes.stream()
                    .filter(gene -> geneToScore.get(gene) != null)
                    .sorted((g1, g2) -> geneToScore.get(g2).compareTo(geneToScore.get(g1)))
                    .forEach(gene -> dataset.addValue(geneToScore.get(gene), "Pathway", gene));
        
        // Fire the data change event
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
    }

    private JPanel createGraphPane() {
        dataset = new HashedCategoryDataSet();
        dataset.setNotify(false);
        
        JFreeChart chart = ChartFactory.createBarChart("Gene Score Distribution",
                "Gene",
                "Score",
                dataset,
                PlotOrientation.HORIZONTAL, 
                true, 
                false, 
                false);
        
        plot = (CategoryPlot) chart.getPlot();
        plot.setRangePannable(true);
        plot.setRangeGridlinesVisible(false);
        plot.setBackgroundPaint(Color.WHITE);
        // Want to use a smaller font
        Font titleFont = chart.getTitle().getFont();
        chart.getTitle().setFont(titleFont.deriveFont(titleFont.getSize() - 2.0f));
        
        CategoryAxis geneAxis = plot.getDomainAxis();
        geneAxis.setTickLabelsVisible(false); // Turn off genes display
        geneAxis.setTickMarksVisible(false);
        
        LayeredBarRenderer renderer = new LayeredBarRenderer() {

            @Override
            protected void calculateBarWidth(CategoryPlot plot, 
                    Rectangle2D dataArea, 
                    int rendererIndex,
                    CategoryItemRendererState state) {
                if (rendererIndex == 1)
                    state.setBarWidth(2.0d);
                else 
                    state.setBarWidth(1.0d);
            }
            
        };
        renderer.setDrawBarOutline(false);
        renderer.setBaseItemLabelsVisible(false);
        // Use a transparent light grey
        renderer.setSeriesPaint(0, new Color(211, 211, 211));
        renderer.setSeriesPaint(1, Color.RED);
        plot.setRenderer(renderer);
        plot.setDatasetRenderingOrder(DatasetRenderingOrder.REVERSE);
        
        ChartPanel chartPane = new ChartPanel(chart);
        chartPane.setDoubleBuffered(true);
        chartPane.setBorder(BorderFactory.createEtchedBorder());

        return chartPane;
    }
    
    /**
     * Customized DefaultCategoryDataset class to quick performance by using Map, instead of List,
     * for columns, which are genes. 
     * @author wug
     *
     */
    private class HashedCategoryDataSet extends DefaultCategoryDataset {
        private Map<Comparable, Integer> columnKeyToIndex;
        private List<Comparable> columnKeys;
        private List<Comparable> rowKeys;
        private Map<Comparable, Map<Comparable, Number>> rowToColToValue;
        
        public HashedCategoryDataSet() {
            columnKeyToIndex = new HashMap<>();
            rowKeys = new ArrayList<>();
            columnKeys = new ArrayList<>();
            rowToColToValue = new HashMap<>();
        }
        
        @Override
        public int getRowCount() {
            return rowKeys.size();
        }

        @Override
        public int getColumnCount() {
            return columnKeys.size();
        }

        @Override
        public Number getValue(int row, int column) {
            Comparable rowKey = getRowKey(row);
            Comparable colKey = getColumnKey(column);
            return getValue(rowKey, colKey);
        }

        @Override
        public Comparable getRowKey(int row) {
            return rowKeys.get(row);
        }

        @Override
        public int getRowIndex(Comparable key) {
            return rowKeys.indexOf(key); // We expect to see two rows. Therefore, this should be very quick.
        }

        @Override
        public List getRowKeys() {
            return rowKeys;
        }

        @Override
        public Comparable getColumnKey(int column) {
            return columnKeys.get(column);
        }

        @Override
        public List getColumnKeys() {
            return columnKeys;
        }

        @Override
        public Number getValue(Comparable rowKey, Comparable columnKey) {
            Map<Comparable, Number> colToValue = rowToColToValue.get(rowKey);
            if (colToValue == null)
                return null;
            return colToValue.get(columnKey);
        }

        @Override
        public void addValue(Number value, Comparable rowKey, Comparable columnKey) {
            rowToColToValue.compute(rowKey, (key, map) -> {
                if (map == null)
                    map = new HashMap<>();
                map.put(columnKey, value.doubleValue());
                return map;
            });
            if (!rowKeys.contains(rowKey))
                rowKeys.add(rowKey);
            if (columnKeyToIndex.containsKey(columnKey))
                return;
            columnKeyToIndex.put(columnKey, columnKeyToIndex.size() - 1);
            columnKeys.add(columnKey);
            fireDatasetChanged();
        }

        @Override
        public void removeValue(Comparable rowKey, Comparable columnKey) {
            Map<Comparable, Number> colToValue = rowToColToValue.get(rowKey);
            if (colToValue != null)
                colToValue.remove(columnKey);
            fireDatasetChanged();
        }

        @Override
        public void removeRow(int rowIndex) {
            Comparable rowKey = getRowKey(rowIndex);
            removeRow(rowKey);
        }

        @Override
        public void removeRow(Comparable rowKey) {
            rowToColToValue.remove(rowKey);
            fireDatasetChanged();
        }

        @Override
        public void removeColumn(int columnIndex) {
            removeColumn(getColumnKey(columnIndex));
        }

        @Override
        public void removeColumn(Comparable columnKey) {
            rowToColToValue.forEach((row, map) -> map.remove(columnKey));
            fireDatasetChanged();
        }

        @Override
        public void clear() {
            columnKeys.clear();
            rowKeys.clear();
            columnKeyToIndex.clear();
            rowToColToValue.clear();
        }

        @Override
        public int hashCode() {
            return rowToColToValue.hashCode();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            throw new CloneNotSupportedException("Don't clone this customized DefaultCategoryDataSet!");
        }

        @Override
        public int getColumnIndex(Comparable key) {
            Integer index = columnKeyToIndex.get(key);
            if (index == null)
                return -1;
            return index;
        }
        
    }
    
    /**
     * This is a test method.
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        String fileName = "/Users/wug/Documents/eclipse_workspace/ohsu/results/beataml/Trametinib_Corr_Gene_Auc_101918.txt";
        Map<String, Double> geneToScore = new HashMap<>();
        Files.lines(Paths.get(fileName))
             .skip(1)
             .forEach(line -> {
                 String[] tokens = line.split("\t");
                 geneToScore.put(tokens[0], new Double(tokens[1]));
             });
        
        geneToScore.keySet()
        .stream()
        .sorted((g1, g2) -> geneToScore.get(g2).compareTo(geneToScore.get(g1)))
        .forEach(gene -> geneToScore.get(gene));
        
        GeneScoreDistributionPlotPane plotPane = new GeneScoreDistributionPlotPane();
        Set<String> genes = MathUtilities.randomSampling(geneToScore.keySet(), 250);
        plotPane.setGeneToScore(geneToScore, genes);
        JFrame frame = new JFrame("Gene Score Distribution");
        frame.getContentPane().add(plotPane, BorderLayout.CENTER);
        frame.setSize(400, 800);
        GKApplicationUtilities.center(frame);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

}
