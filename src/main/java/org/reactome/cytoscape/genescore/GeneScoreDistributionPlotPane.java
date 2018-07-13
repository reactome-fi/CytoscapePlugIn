package org.reactome.cytoscape.genescore;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

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
                   .forEach(gene -> {
                       dataset.addValue(geneToScore.get(gene), "All Genes", gene);
                   });
        pathwayGenes.stream()
                    .filter(gene -> geneToScore.get(gene) != null)
                    .sorted((g1, g2) -> geneToScore.get(g2).compareTo(geneToScore.get(g1)))
                    .forEach(gene -> dataset.addValue(geneToScore.get(gene), "Pathway", gene));
        
        // Fire the data change event
        DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
        plot.datasetChanged(event);
    }

    private JPanel createGraphPane() {
        dataset = new DefaultCategoryDataset();
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
        chartPane.setBorder(BorderFactory.createEtchedBorder());

        return chartPane;
    }

}
