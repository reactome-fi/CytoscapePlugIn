/*
 * Created on Apr 24, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

import org.gk.graphEditor.SelectionMediator;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.data.general.DatasetChangeEvent;
import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.cytoscape.service.NetworkModulePanel;
import org.reactome.cytoscape.service.PlotTablePanel;
import org.reactome.cytoscape.util.PlugInObjectManager;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * @author gwu
 *
 */
public class TimeCoursePane extends NetworkModulePanel {
    private PlotTablePanel contentPane;
    private VariableSelectionHandler selectionHandler;
    
    /**
     * Default constructor.
     */
    public TimeCoursePane(String title) {
        super(title);
        hideOtherNodesBox.setVisible(false);
        modifyContentPane();
        enableSelectionSync();
    }
    
    private void enableSelectionSync() {
        selectionHandler = new VariableSelectionHandler();
        selectionHandler.setVariableTable(contentPane.getTable());
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.addSelectable(selectionHandler);
        
        contentPane.getTable().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                handleTableSelection();
            }
        });
    }
    
    private void handleTableSelection() {
        SelectionMediator mediator = PlugInObjectManager.getManager().getDBIdSelectionMediator();
        mediator.fireSelectionEvent(selectionHandler);
    }
    
    protected void modifyContentPane() {
        // Re-create control tool bars
        for (int i = 0; i < controlToolBar.getComponentCount(); i++) {
            controlToolBar.remove(i);
        }
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
        addTablePlotPane();
    }

    protected void addTablePlotPane() {
        // Add a JSplitPane for the table and a new graph pane to display graphs
        for (int i = 0; i < getComponentCount(); i++) {
            Component comp = getComponent(i);
            if (comp instanceof JScrollPane) {
                remove(comp);
                break;
            }
        }
        contentPane = new TimeCoursePlotPane();
        contentPane.setTable(contentTable);
        // Don't allow for easy handling
        contentTable.getTableHeader().setReorderingAllowed(false);
        add(contentPane, BorderLayout.CENTER);
    }

    @Override
    protected NetworkModuleTableModel createTableModel() {
        return new TimeCourseTableModel();
    }
    
    /**
     * Set the actual data to be displayed.
     * @param variables
     */
    public void setSimulationResults(List<BooleanVariable> variables) {
        TimeCourseTableModel model = (TimeCourseTableModel) contentTable.getModel();
        model.setTimeCourse(variables);
    }
    
    private class TimeCourseTableModel extends NetworkModuleTableModel implements VariableTableModelInterface {
        
        public TimeCourseTableModel() {
            columnHeaders = new String[1];
            columnHeaders[0] = "Entity";
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 0)
                return BooleanVariable.class;
            else
                return Number.class;
        }

        public void setTimeCourse(List<BooleanVariable> variables) {
            // Find how many time steps
            int steps = variables.get(0).getTrack().length;
            columnHeaders = new String[steps + 1];
            columnHeaders[0] = "Entity";
            for (int i = 1; i <= steps; i++)
                columnHeaders[i] = "Step " + i;
            tableData.clear();
            for (BooleanVariable var : variables) {
                Object[] row = new Object[var.getTrack().length + 1];
                row[0] = var;
                Number[] track = var.getTrack();
                for (int i = 0; i < track.length; i++)
                    row[i + 1] = track[i];
                tableData.add(row);
            }
            fireTableStructureChanged();
        }

        @Override
        public List<Integer> getRowsForSelectedIds(List<Long> ids) {
            List<Integer> rtn = new ArrayList<>();
            for (int i = 0; i < tableData.size(); i++) {
                Object[] row = tableData.get(i);
                BooleanVariable var = (BooleanVariable) row[0];
                String reactomeId = var.getProperty("reactomeId");
                if (reactomeId == null)
                    continue;
                if (ids.contains(new Long(reactomeId)))
                    rtn.add(i);
            }
            return rtn;
        }
        
    }
    
    private class TimeCoursePlotPane extends PlotTablePanel {
        
        public TimeCoursePlotPane() {
            super("Time Step", "Boolean Fuzzy Value");
        }
        
        @Override
        public void setTable(JTable table) {
            this.contentTable = table;
            // For showing BooleanVariable
            table.setDefaultRenderer(BooleanVariable.class, new DefaultTableCellRenderer() {

                @Override
                protected void setValue(Object value) {
                    BooleanVariable var = (BooleanVariable) value;
                    setText(var == null ? "" : var.getName());
                }
                
            });
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
                }
            });
            
            contentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    // The following call has to wait until other Swing related thing is done.
                    // Otherwise, it is possible to throw an index related exception
                    SwingUtilities.invokeLater(new Runnable() {
                        
                        @Override
                        public void run() {
                            resetPlotDataset();
                        }
                    });
                }
            });
        }
        
        @Override
        protected void resetPlotDataset() {
            dataset.clear();
            int selectedRows = contentTable.getSelectedRowCount();
            if (selectedRows > MAXIMUM_COLUMNS_FOR_PLOT) {
                plot.setNoDataMessage(TOO_MANY_LINES_MESSAGE);
            }
            else if (selectedRows == 0) {
                plot.setNoDataMessage(EMPTY_DATA_MESSAGE);
            }
            else {
                TableModel model = contentTable.getModel();
                for (int row : contentTable.getSelectedRows()) {
                    int modelRow = contentTable.convertRowIndexToModel(row);
                    BooleanVariable var = (BooleanVariable) model.getValueAt(modelRow, 0);
                    for (int i = 1; i < model.getColumnCount(); i++) {
                        Number value = (Number) model.getValueAt(modelRow, i);
                        dataset.addValue(value, var.getName(), model.getColumnName(i));
                    }
                    // The following code is used to control performance:
                    // 16 is arbitrary
                    CategoryAxis axis = plot.getDomainAxis();
                    if (model.getColumnCount() > PlugInUtilities.PLOT_CATEGORY_AXIX_LABEL_CUT_OFF) {
                        axis.setTickLabelsVisible(false);
                        axis.setTickMarksVisible(false);
                    }
                    else {
                        axis.setTickLabelsVisible(true);
                        axis.setTickMarksVisible(true);
                    }
                }
            }
            DatasetChangeEvent event = new DatasetChangeEvent(this, dataset);
            plot.datasetChanged(event);
        }
    }
}
