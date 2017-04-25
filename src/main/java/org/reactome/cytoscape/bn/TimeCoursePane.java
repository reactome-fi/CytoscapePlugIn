/*
 * Created on Apr 24, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import org.reactome.cytoscape.service.AnimationPlayer;
import org.reactome.cytoscape.service.AnimationPlayerControl;
import org.reactome.cytoscape.service.NetworkModulePanel;
import org.reactome.cytoscape.service.PathwayHighlightControlPanel;
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
    private PathwayHighlightControlPanel hiliteControlPane;
    // For animation
    private JComboBox<String> timeBox;
    private boolean duringTimeBoxUpdate;
    
    /**
     * Default constructor.
     */
    public TimeCoursePane(String title) {
        super(title);
        hideOtherNodesBox.setVisible(false);
        modifyContentPane();
        enableSelectionSync();
    }
    
    public PathwayHighlightControlPanel getHiliteControlPane() {
        return hiliteControlPane;
    }

    public void setHiliteControlPane(PathwayHighlightControlPanel hiliteControlPane) {
        this.hiliteControlPane = hiliteControlPane;
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
        addControls();
        controlToolBar.add(closeGlue);
        controlToolBar.add(closeBtn);
        addTablePlotPane();
    }
    
    private void addControls() {
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));
        // Show a list of samples
        JLabel timeLabel = new JLabel("Choose time to highlight pathway:");
        timeBox = new JComboBox<>();
        timeBox.setEditable(false);
        DefaultComboBoxModel<String> sampleModel = new DefaultComboBoxModel<>();
        timeBox.setModel(sampleModel);
        panel.add(timeLabel);
        panel.add(timeBox);
        // For performing animation
        AnimationPlayerControl animiationControl = new AnimationPlayerControl();
        TimeListAnimationPlayer player = new TimeListAnimationPlayer();
        animiationControl.setPlayer(player);
        animiationControl.setInterval(500); // 0.5 second per sample
        panel.add(animiationControl);

        // Link these two boxes together
        timeBox.addItemListener(new ItemListener() {
            
            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED)
                    handleTimeBoxSelection();
            }
        });
        controlToolBar.add(panel);
    }
    
    private void handleTimeBoxSelection() {
        if (duringTimeBoxUpdate)
            return;
        TimeCourseTableModel model = (TimeCourseTableModel) contentPane.getTable().getModel();
        String time = (String) timeBox.getSelectedItem();
        int col = model.getColumn(time);
        hilitePathway(col);
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
        
        duringTimeBoxUpdate = true;
        DefaultComboBoxModel<String> timeModel = (DefaultComboBoxModel<String>) timeBox.getModel();
        timeModel.removeAllElements();
        for (int i = 1; i < model.getColumnCount(); i++)
            timeModel.addElement(model.getColumnName(i));
        duringTimeBoxUpdate = false;
        timeBox.setSelectedIndex(timeModel.getSize() - 1); // Select the last time point
    }
    
    private void hilitePathway(int column) {
        if (hiliteControlPane == null || column < 1) // The first column is entity
            return;
        hiliteControlPane.setVisible(true);
        TimeCourseTableModel model = (TimeCourseTableModel) contentPane.getTable().getModel();
        Map<String, Double> idToValue = model.getIdToValue(column);
        hiliteControlPane.setIdToValue(idToValue);
        // Use 0 and 1 as default
        double[] minMaxValues = hiliteControlPane.calculateMinMaxValues(0.0d, 1.0d);
        hiliteControlPane.resetMinMaxValues(minMaxValues);
    }
    
    private class TimeListAnimationPlayer implements AnimationPlayer {
        
        public TimeListAnimationPlayer() {
        }

        @Override
        public void forward() {
            int selectedIndex = timeBox.getSelectedIndex();
            if (selectedIndex == timeBox.getItemCount() - 1)
                selectedIndex = -1;
            timeBox.setSelectedIndex(selectedIndex + 1);
        }

        @Override
        public void backward() {
            int selectedIndex = timeBox.getSelectedIndex();
            if (selectedIndex == 0)
                selectedIndex = timeBox.getItemCount();
            timeBox.setSelectedIndex(selectedIndex - 1);
        }
        
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
        
        public int getColumn(String colName) {
            for (int i = 0; i < columnHeaders.length; i++) {
                if (columnHeaders[i].equals(colName))
                    return i;
            }
            return -1;
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
        
        public Map<String, Double> getIdToValue(int column) {
            Map<String, Double> idToValue = new HashMap<>();
            // Use the last column
            for (int i = 0; i < tableData.size(); i++) {
                Object[] row = tableData.get(i);
                BooleanVariable var = (BooleanVariable) row[0];
                String id = var.getProperty("reactomeId");
                if (id == null)
                    continue;
                Number value = (Number) row[column];
                idToValue.put(id, value.doubleValue());
            }
            return idToValue;
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
