/*
 * Created on Apr 24, 2017
 *
 */
package org.reactome.cytoscape.bn;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import org.gk.util.DialogControlPane;
import org.gk.util.GKApplicationUtilities;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.data.general.DatasetChangeEvent;
import org.reactome.booleannetwork.BooleanVariable;
import org.reactome.cytoscape.service.AnimationPlayer;
import org.reactome.cytoscape.service.AnimationPlayerControl;
import org.reactome.cytoscape.service.PlotTablePanel;
import org.reactome.cytoscape.util.PlugInUtilities;

/**
 * @author gwu
 *
 */
public class TimeCoursePane extends VariableCytoPaneComponent {
    // 20 is arbitrary. However, the user can change this always.
    private final int INITIAL_MAX_COLUMNS = 20;
    private TimeCoursePlotPane contentPane;
    // For animation
    private JComboBox<String> timeBox;
    private boolean duringTimeBoxUpdate;
    // Cache variables' tracks to break synchronization between table and plot
    // so that the user can configure how many columns needed to be shown in 
    // case there are too many time steps in simulations.
    private Map<BooleanVariable, List<Number>> varToTrack;
    
    /**
     * Default constructor.
     */
    public TimeCoursePane(String title) {
        super(title);
        hideOtherNodesBox.setVisible(false);
        modifyContentPane();
    }
    
    @Override
    protected void modifyContentPane() {
        super.modifyContentPane();
        // Re-create control tool bars
        for (int i = 0; i < controlToolBar.getComponentCount(); i++) {
            controlToolBar.remove(i);
        }
        addControls();
        controlToolBar.add(closeGlue);
        createHighlightViewBtn();
        controlToolBar.add(hiliteDiagramBtn);
        controlToolBar.add(closeBtn);
        addTablePlotPane();
        
        // Add a note
        String note = new String("Note: The plot shows all results. But the table may show partial results. "
                               + "Use \"Configure Columns\" popup menu to set columns for display.");
        JLabel noteLabel = new JLabel(note);
        noteLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        Font font = noteLabel.getFont();
        font = font.deriveFont(Font.ITALIC, font.getSize2D() - 1.0f);
        noteLabel.setFont(font);
        add(noteLabel, BorderLayout.SOUTH);
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
        reHilitePathway();
    }
    
    @Override
    protected void reHilitePathway() {
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

    @Override
    protected void doContentTablePopup(MouseEvent e) {
        JPopupMenu popup = createExportAnnotationPopup();
        
        popup.addSeparator();
        JMenuItem configueColumnsItem = new JMenuItem("Configure Columns");
        configueColumnsItem.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                configureColumns();
            }
        });
        popup.add(configueColumnsItem);
        
        popup.show(contentTable, 
                   e.getX(), 
                   e.getY());
    }
    
    private void configureColumns() {
        TimeCourseTableModel model = (TimeCourseTableModel) contentTable.getModel();
        Window window = (Window) SwingUtilities.getAncestorOfClass(Window.class, this);
        ColumnConfigurationDialog dialog = new ColumnConfigurationDialog(window);
        dialog.setTimeSteps(model.getStartTime(),
                            model.getEndTime());
        dialog.setModal(true);
        dialog.setLocationRelativeTo(window);
        dialog.setVisible(true);
        if (!dialog.isOkClicked)
            return;
        int startTime = dialog.getStartStep();
        int endTime = dialog.getEndTF();
        
        // Used to reset selection
        Set<BooleanVariable> selectedVars = getSelectedVars();
        model.setTimeSteps(startTime, endTime);
        if (selectedVars != null)
            selectVars(selectedVars);
    }

    /**
     * Set the actual data to be displayed.
     * @param variables
     */
    public void setSimulationResults(List<BooleanVariable> variables) {
        recordTracks(variables);
        TimeCourseTableModel model = (TimeCourseTableModel) contentTable.getModel();
        model.initTimeSteps();
    }

    private void setUpTimeBox(TimeCourseTableModel model) {
        duringTimeBoxUpdate = true;
        DefaultComboBoxModel<String> timeModel = (DefaultComboBoxModel<String>) timeBox.getModel();
        timeModel.removeAllElements();
        for (int i = 1; i < model.getColumnCount(); i++)
            timeModel.addElement(model.getColumnName(i));
        duringTimeBoxUpdate = false;
        timeBox.setSelectedIndex(timeModel.getSize() - 1); // Select the last time point
    }
    
    private int getMaxTimeStep() {
        return varToTrack.values().stream().findAny().get().size();
    }
    
    private Set<BooleanVariable> getSelectedVars() {
        Set<BooleanVariable> selectedVars = new HashSet<>();
        TableModel model = contentTable.getModel();
        for (int row : contentTable.getSelectedRows()) {
            int modelRow = contentTable.convertRowIndexToModel(row);
            BooleanVariable var = (BooleanVariable) model.getValueAt(modelRow, 0);
            selectedVars.add(var);
        }
        return selectedVars;
    }
    
    private void selectVars(Set<BooleanVariable> vars) {
        ListSelectionModel selectionModel = contentTable.getSelectionModel();
        selectionModel.setValueIsAdjusting(true);
        for (int i = 0; i < contentTable.getRowCount(); i++) {
            BooleanVariable var = (BooleanVariable) contentTable.getValueAt(i, 0);
            if (vars.contains(var))
                selectionModel.addSelectionInterval(i, i);
        }
        selectionModel.setValueIsAdjusting(false);
    }
    
    private void recordTracks(List<BooleanVariable> variables) {
        if (varToTrack == null)
            varToTrack = new HashMap<>();
        else
            varToTrack.clear();
        variables.forEach(var -> {
            Number[] track = var.getTrack();
            List<Number> copy = Arrays.asList(track);
            varToTrack.put(var,  copy);
        });
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
    
    private class TimeCourseTableModel extends VariableTableModel {
        private int startTime;
        private int endTime;
        
        public TimeCourseTableModel() {
        }
        
        public void setTimeSteps(int start, int end) {
            // Need to do a little bit check
            if (start < 0)
                start = 0;
            int max = varToTrack.values().stream().findAny().get().size();
            if (end > max - 1)
                end = max - 1;
            this.startTime = start;
            this.endTime = end;
            resetData();
            setUpTimeBox(this);
        }
        
        public int getStartTime() {
            return this.startTime;
        }
        
        public int getEndTime() {
            return this.endTime;
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

        public void initTimeSteps() {
            // Find how many time steps
            int steps = getMaxTimeStep();
            int end = steps - 1; // Since it is used inclusively
            int start = steps - INITIAL_MAX_COLUMNS;
            setTimeSteps(start, end);
        }

        private void resetData() {
            columnHeaders = new String[(endTime - startTime) + 2];
            columnHeaders[0] = "Entity";
            int index = 0;
            for (int i = startTime; i <= endTime; i++)
                columnHeaders[++ index] = "Step " + i;
            tableData.clear();
            List<BooleanVariable> variables = new ArrayList<>(varToTrack.keySet());
            variables.sort((var1, var2) -> var1.getName().compareTo(var2.getName()));
            for (BooleanVariable var : variables) {
                Object[] row = new Object[columnHeaders.length];
                row[0] = var;
                List<Number> track = varToTrack.get(var);
                index = 0;
                for (int i = startTime; i <= endTime; i++)
                    row[++index] = track.get(i);
                tableData.add(row);
            }
            fireTableStructureChanged();
        }
    }
    
    private class TimeCoursePlotPane extends PlotTablePanel {
        
        public TimeCoursePlotPane() {
            super("Time Step", "Logic Fuzzy Value");
        }
        
        @Override
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
                }
            });
            
            contentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting())
                        return; 
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
        
        /**
         * TODO: The following method is called twice during a selection and should be fixed in the future.
         */
        @Override
        protected synchronized void resetPlotDataset() {
            dataset.clear();
            int selectedRows = contentTable.getSelectedRowCount();
            if (selectedRows > MAXIMUM_COLUMNS_FOR_PLOT) {
                plot.setNoDataMessage(TOO_MANY_LINES_MESSAGE);
            }
            else if (selectedRows == 0) {
                plot.setNoDataMessage(EMPTY_DATA_MESSAGE);
            }
            else {
                Set<BooleanVariable> selectedVars = getSelectedVars();
                selectedVars.forEach(var -> {
                    List<Number> track = varToTrack.get(var);
                    for (int i = 0; i < track.size(); i++) {
                        Number value = track.get(i);
                        dataset.addValue(value, var.getName(), "Step " + i);
                    }
                });
                // The following code is used to control performance:
                // 16 is arbitrary
                int maxTimePoint = getMaxTimeStep();
                CategoryAxis axis = plot.getDomainAxis();
                if (maxTimePoint > PlugInUtilities.PLOT_CATEGORY_AXIX_LABEL_CUT_OFF) {
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
        }
    }
    
    private class ColumnConfigurationDialog extends JDialog {
        private boolean isOkClicked;
        private JTextField startTF;
        private JTextField endTF;
        
        public ColumnConfigurationDialog(Window owner) {
            super(owner);
            init();
        }
        
        public void setTimeSteps(int start, int end) {
            startTF.setText(start + "");
            endTF.setText(end + "");
        }
        
        public int getStartStep() {
            return getInteger(startTF);
        }
        
        private int getInteger(JTextField tf) {
            String text = tf.getText().trim();
            if (text.length() == 0)
                return 0;
            return Integer.parseInt(text);
        }
        
        public int getEndTF() {
            return getInteger(endTF);
        }
        
        private boolean validateValues() {
            int start = getStartStep();
            int end = getEndTF();
            if (start >= end) {
                JOptionPane.showMessageDialog(this,
                                              "Start value should be smaller than end value!",
                                              "Error in Values", 
                                              JOptionPane.ERROR_MESSAGE);
                return false;
            }
            return true;
        }
        
        private void init() {
            setTitle("Configure Columns");
            JPanel contentPane = new JPanel();
            contentPane.setBorder(BorderFactory.createEtchedBorder());
            contentPane.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(4, 4, 4, 4);
            constraints.anchor = GridBagConstraints.WEST;
            JLabel label = GKApplicationUtilities.createTitleLabel("Enter time steps to display in table:");
            constraints.gridwidth = 2;
            contentPane.add(label, constraints);
            JLabel startLabel = new JLabel("Start (inclusive):");
            constraints.gridy = 1;
            constraints.gridwidth = 1;
            contentPane.add(startLabel, constraints);
            startTF = new JTextField();
            startTF.setColumns(4);
            constraints.gridx = 1;
            contentPane.add(startTF, constraints);
            JLabel endLabel = new JLabel("End (inclusive):");
            constraints.gridx = 0;
            constraints.gridy = 2;
            contentPane.add(endLabel, constraints);
            endTF = new JTextField();
            endTF.setColumns(4);
            constraints.gridx = 1;
            contentPane.add(endTF, constraints);
            String note = "Note: Enter numbers only.";
            JLabel noteLabel = new JLabel(note);
            Font font = noteLabel.getFont();
            font = font.deriveFont(Font.ITALIC);
            noteLabel.setFont(font);
            constraints.gridx = 0;
            constraints.gridy ++;
            constraints.gridwidth = 2;
            contentPane.add(noteLabel, constraints);
            getContentPane().add(contentPane, BorderLayout.CENTER);
            
            DialogControlPane controlPane = new DialogControlPane();
            getContentPane().add(controlPane,  BorderLayout.SOUTH);
            controlPane.getCancelBtn().addActionListener(e -> dispose());
            controlPane.getOKBtn().addActionListener(e -> {
                if (!validateValues())
                    return; // Don't turn off the dialog
                isOkClicked = true;
                dispose();
            });
            
            setSize(400, 250);
            
            getRootPane().setDefaultButton(controlPane.getOKBtn());
        }
        
    }
}
