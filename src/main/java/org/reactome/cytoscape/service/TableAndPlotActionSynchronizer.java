/*
 * Created on Apr 1, 2014
 *
 */
package org.reactome.cytoscape.service;

import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableModel;

import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.plot.CategoryMarker;
import org.jfree.chart.plot.CategoryPlot;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * This utility class is used to do synchronization between a JTable and JFreePlot
 * for mouse and selection actions.
 * @author gwu
 *
 */
public class TableAndPlotActionSynchronizer {
    private JTable table;
    private ChartPanel chartPane;
    private boolean isFromPlot;
    
    /**
     * Default constructor.
     */
    public TableAndPlotActionSynchronizer(JTable table,
                                          ChartPanel chartPanel) {
        // Make sure the plot contained by chartPanel is a CategoryPlot
        if (!(chartPanel.getChart().getPlot() instanceof CategoryPlot))
            throw new IllegalArgumentException("The plot in the passed ChartPanel object should be a CategoryPlot!");
        this.table = table;
        this.chartPane = chartPanel;
        installListeners();
    }
    
    private void installListeners() {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting())
                    doTableSelection();
            }
        });
        
        // For mouse selection
        chartPane.addChartMouseListener(new ChartMouseListener() {
            
            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
            }
            
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                doChartMouseClicked(event);
            }
        });
    }
    
    private void doChartMouseClicked(ChartMouseEvent event) {
        ChartEntity entity = event.getEntity();
        if (entity == null || !(entity instanceof CategoryItemEntity))
            return;
        CategoryPlot plot = (CategoryPlot) chartPane.getChart().getPlot();
        CategoryItemEntity caEntity = (CategoryItemEntity) entity;
        plot.clearDomainMarkers();
        CategoryMarker marker = PlugInUtilities.createMarker(caEntity.getColumnKey());
        plot.addDomainMarker(marker);
        // Need to select the row in the table
        isFromPlot = true;
        selectRowInTableBasedOnFirstColumn((String)caEntity.getColumnKey());
        isFromPlot = false;
    }
    
    private void selectRowInTableBasedOnFirstColumn(String rowKey) {
        table.clearSelection();
        // Find the row index in the table model
        TableModel model = table.getModel();
        int selected = -1;
        for (int i = 0; i < model.getRowCount(); i++) {
            String tmp = (String) model.getValueAt(i, 0);
            if (tmp.equals(rowKey)) {
                selected = i;
                break;
            }
        }
        if (selected == -1)
            return;
        int index = table.convertRowIndexToView(selected);
        table.setRowSelectionInterval(index, index);
        Rectangle rect = table.getCellRect(index, 0, false);
        table.scrollRectToVisible(rect);
    }
    
    private void doTableSelection() {
        if (isFromPlot)
            return; // No need
        CategoryPlot plot = (CategoryPlot) chartPane.getChart().getPlot();
        // Need to clear all markers first
        plot.clearDomainMarkers();
        int[] rows = table.getSelectedRows();
        if (rows == null || rows.length == 0) {
            return;
        }
        TableModel model = table.getModel();
        for (int i = 0; i < rows.length; i++) {
            int index = table.convertRowIndexToModel(rows[i]);
            String sample = (String) model.getValueAt(index, 0);
            CategoryMarker marker = PlugInUtilities.createMarker(sample);
            plot.addDomainMarker(marker);
        }
    }
    
}
